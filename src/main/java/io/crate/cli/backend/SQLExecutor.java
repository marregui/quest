package io.crate.cli.backend;

import io.crate.cli.common.EventListener;
import io.crate.cli.common.EventSpeaker;
import io.crate.shade.org.postgresql.jdbc.PgResultSetMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class SQLExecutor implements EventSpeaker<SQLExecutor.EventType>, Closeable {

    public enum EventType {
        QUERY_STARTED,
        QUERY_FETCHING,
        QUERY_COMPLETED,
        QUERY_CANCELLED,
        QUERY_FAILURE,
        RESULTS_AVAILABLE
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SQLExecutor.class);
    private static final int START_BATCH_SIZE = 100;
    private static final int MAX_BATCH_SIZE = 20000;
    private static final String[] STATUS_COL_NAME_ONLY = {"Status"};
    private static final int[] STATUS_COL_TYPE_ONLY = { Types.VARCHAR };
    private static final Object[] STATUS_OK_VALUE_ONLY = {"OK"};


    private final EventListener<SQLExecutor, SQLExecutionResponse> eventListener;
    private final Map<String, Future<?>> runningQueries;
    private volatile ExecutorService cachedES;


    public SQLExecutor(EventListener<SQLExecutor, SQLExecutionResponse> eventListener) {
        this.eventListener = eventListener;
        runningQueries = new HashMap<>();
    }

    public void start() {
        if (null != cachedES) {
            throw new IllegalStateException("already started");
        }
        runningQueries.clear();
        cachedES = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
        LOGGER.info("{} is running", SQLExecutor.class.getSimpleName());
    }

    @Override
    public void close() {
        if (null == cachedES) {
            throw new IllegalStateException("already closed");
        }
        for (Future<?> query : runningQueries.values()) {
            if (false == query.isDone() && false == query.isCancelled()) {
                query.cancel(true);
            }
        }
        cachedES.shutdownNow();
        try {
            cachedES.awaitTermination(200L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            cachedES = null;
            runningQueries.clear();
            LOGGER.info("{} has finished", SQLExecutor.class.getSimpleName());
        }
    }

    public void cancelSubmittedRequest(SQLExecutionRequest request) {
        if (null == cachedES) {
            throw new IllegalStateException("not started");
        }
        String key = request.getKey();
        Future<?> result = runningQueries.remove(key);
        if (null != result && false == result.isDone() && false == result.isCancelled()) {
            LOGGER.info("Cancelling pre-existing query for key: [{}]", key);
            result.cancel(true);
            request.setWasCancelled(true);
            eventListener.onSourceEvent(
                    SQLExecutor.this,
                    EventType.QUERY_CANCELLED,
                    new SQLExecutionResponse(request, 0L, 0L, 0L));
        }
    }

    public void submit(SQLExecutionRequest request) {
        if (null == cachedES) {
            throw new IllegalStateException("not started");
        }
        String key = request.getKey();
        cancelSubmittedRequest(request);
        runningQueries.put(key, cachedES.submit(() -> executeQuery(request)));
        LOGGER.info("Query [{}] submitted", key);
    }

    private final void executeQuery(SQLExecutionRequest request) {
        eventListener.onSourceEvent(
                SQLExecutor.this,
                EventType.QUERY_STARTED,
                new SQLExecutionResponse(request, 0L, 0L,0L));
        long checkpointTs = System.nanoTime();
        long startTS = checkpointTs;
        String key = request.getKey();
        String query = request.getCommand();
        LOGGER.info("Executing query [{}]: {}", key, query);
        SQLConnection conn = request.getSQLConnection();
        if (false == conn.checkConnectivity()) {
            LOGGER.error("While about to run [{}], lost connectivity with {}", key, conn);
            eventListener.onSourceEvent(
                    SQLExecutor.this,
                    EventType.QUERY_FAILURE,
                    new SQLExecutionResponse(
                            key,
                            conn,
                            query,
                            -1L, -1L, -1L,
                            new RuntimeException(String.format(
                                    Locale.ENGLISH,
                                    "Connection [%s] is down",
                                    conn))));
            return;
        }
        LOGGER.info("Connectivity looks ok [{}]: {}", key, query);
        long queryExecutedTs;
        long queryExecutedMs;
        int rowId = 0;
        int batchId = 0;
        int batchSize = START_BATCH_SIZE;
        List<SQLRowType> rows = new ArrayList<>(batchSize);
        eventListener.onSourceEvent(
                SQLExecutor.this,
                EventType.QUERY_STARTED,
                new SQLExecutionResponse(request,
                        toMillis(System.nanoTime() - startTS),
                        0L,
                        0L));
        try (Statement stmt = conn.getConnection().createStatement()) {
            boolean checkResults = stmt.execute(query);
            queryExecutedTs = System.nanoTime();
            queryExecutedMs = toMillis(queryExecutedTs - startTS);
            if (checkResults) {
                eventListener.onSourceEvent(
                        SQLExecutor.this,
                        EventType.QUERY_FETCHING,
                        new SQLExecutionResponse(
                                request,
                                queryExecutedMs,
                                queryExecutedMs,
                                0L));
                ResultSet rs = stmt.getResultSet();
                String[] columnNames = null;
                int[] columnTypes = null;
                while (rs.next()) {
                    checkpointTs = System.nanoTime();
                    if (null == columnNames) {
                        columnNames = extractColumnNames(rs);
                    }
                    if (null == columnTypes) {
                        columnTypes = extractColumnTypes(rs);
                    }
                    rows.add(new SQLRowType(
                            String.valueOf(rowId++),
                            columnNames,
                            columnTypes,
                            extractColumnValues(columnNames, rs)));
                    if (0 == rowId % batchSize) {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug(
                                    "Query [{}] rowId:{}, batchId:{}, batchSize:{}, batchSleep:{}, rows:{}",
                                    key, rowId, batchId, batchSize, batchId * 10L, rows.size());
                        }
                        eventListener.onSourceEvent(
                                SQLExecutor.this,
                                EventType.RESULTS_AVAILABLE,
                                new SQLExecutionResponse(
                                        key,
                                        batchId++,
                                        conn,
                                        query,
                                        toMillis(checkpointTs - startTS),
                                        queryExecutedMs,
                                        toMillis(checkpointTs - queryExecutedTs),
                                        rows));
                        if (batchSize <= MAX_BATCH_SIZE) {
                            batchSize *= 2;
                        }
                        rows = new ArrayList<>(batchSize);
                    }
                }
            } else {
                LOGGER.info("Query [{}]: OK", key);
                rows.add(new SQLRowType(
                        String.valueOf(rowId++),
                        STATUS_COL_NAME_ONLY,
                        STATUS_COL_TYPE_ONLY,
                        STATUS_OK_VALUE_ONLY));
            }
        } catch (SQLException e) {
            LOGGER.error("Error query [{}]: {}", key, e.getMessage());
            checkpointTs = System.nanoTime();
            eventListener.onSourceEvent(
                    SQLExecutor.this,
                    EventType.QUERY_FAILURE,
                    new SQLExecutionResponse(
                            key,
                            conn,
                            query,
                            toMillis(checkpointTs - startTS),
                            -1L,
                            -1L,
                            e));
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "Query [{}] Final rowId:{}, Final batchId:{}, batchSize:{}, Total rows:{}",
                    key, rowId, batchId, batchSize, rowId + 1);
        }
        checkpointTs = System.nanoTime();
        eventListener.onSourceEvent(
                SQLExecutor.this,
                EventType.QUERY_COMPLETED,
                new SQLExecutionResponse(
                        key,
                        batchId++,
                        conn,
                        query,
                        toMillis(checkpointTs - startTS),
                        queryExecutedMs,
                        toMillis(checkpointTs - queryExecutedTs),
                        rows));
        LOGGER.info("Query [{}] {} results, elapsed ms:{} (query:{}, fetch:{})",
                key,
                rowId,
                toMillis(checkpointTs - startTS),
                queryExecutedMs,
                toMillis(checkpointTs - queryExecutedTs));
    }

    private static final long toMillis(long nanos) {
        return TimeUnit.MILLISECONDS.convert(nanos, TimeUnit.NANOSECONDS);
    }

    private static String[] extractColumnNames(ResultSet rs) throws SQLException {
        PgResultSetMetaData metaData = (PgResultSetMetaData) rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        String[] columnNames = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnNames[i] = metaData.getColumnName(i + 1);
            metaData.getColumnType(i + 1);
        }
        return columnNames;
    }

    private static int[] extractColumnTypes(ResultSet rs) throws SQLException {
        PgResultSetMetaData metaData = (PgResultSetMetaData) rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        int[] columnTypes = new int[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnTypes[i] = metaData.getColumnType(i + 1);
        }
        return columnTypes;
    }

    private static Object[] extractColumnValues(String[] columnNames, ResultSet rs) throws SQLException {
        Object[] columns = new Object[columnNames.length];
        for (int i = 0; i < columnNames.length; i++) {
            columns[i] = rs.getObject(i + 1);
        }
        return columns;
    }
}
