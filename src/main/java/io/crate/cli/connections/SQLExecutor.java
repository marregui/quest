package io.crate.cli.connections;

import io.crate.cli.gui.common.EventListener;
import io.crate.cli.gui.common.EventSpeaker;
import io.crate.shade.org.postgresql.jdbc.PgResultSetMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;


public class SQLExecutor implements EventSpeaker<SQLExecutor.EventType>, Closeable {

    public enum EventType {
        RESULTS_AVAILABLE,
        RESULTS_COMPLETED,
        QUERY_FAILURE
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SQLExecutor.class);
    private static final int START_BATCH_SIZE = 100;
    private static final int MAX_BATCH_SIZE = 10000;
    private static final String [] STATUS_COL_NAME_ONLY = { "Status" };
    private static final Object [] STATUS_OK_VALUE_ONLY = { "OK" };


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

    public void submit(SQLExecutionRequest request) {
        if (null == cachedES) {
            throw new IllegalStateException("not started");
        }
        String key = request.getKey();
        Future<?> result = runningQueries.get(key);
        if (null != result && false == result.isDone() && false == result.isCancelled()) {
            LOGGER.info("Cancelling pre-existing query for key: [{}]", key);
            result.cancel(true);
        }
        runningQueries.put(key, cachedES.submit(() -> {
            long startTS = System.nanoTime();
            SQLConnection conn = request.getSQLConnection();
            String query = request.getCommand();
            if (false == conn.checkConnectivity()) {
                LOGGER.error("While about to run [{}], lost connectivity with {}", key, conn);
                eventListener.onSourceEvent(
                        SQLExecutor.this,
                        EventType.QUERY_FAILURE,
                        new SQLExecutionResponse(
                                key,
                                conn,
                                query,
                                new RuntimeException(String.format(
                                        Locale.ENGLISH,
                                        "Connection [%s] is down",
                                        conn))));
                return;
            }
            long checkedConnectivityTS = System.nanoTime();
            long queryExecutedTS = checkedConnectivityTS;
            long resultSetConsumedTS = checkedConnectivityTS;
            LOGGER.info("Executing query [{}]: {}", key, query);
            int rowId = 0;
            int batchId = 0;
            int batchSize = START_BATCH_SIZE;
            List<SQLRowType> rows = new ArrayList<>(batchSize);
            try (Statement stmt = conn.getConnection().createStatement()) {
                boolean checkResults = stmt.execute(query);
                queryExecutedTS = System.nanoTime();
                if (checkResults) {
                    ResultSet rs = stmt.getResultSet();
                    String [] columnNames = null;
                    while (rs.next()) {
                        if (null == columnNames) {
                            columnNames = extractColumnNames(rs);
                        }
                        rows.add(new SQLRowType(
                                String.valueOf(rowId++),
                                columnNames,
                                extractColumns(columnNames, rs)));
                        if (0 == rowId % batchSize) {
                            LOGGER.debug(
                                    "Query [{}] rowId:{}, batchId:{}, batchSize:{}, rows:{}",
                                    key, rowId, batchId, batchSize, rows.size());
                            eventListener.onSourceEvent(
                                    SQLExecutor.this,
                                    EventType.RESULTS_AVAILABLE,
                                    new SQLExecutionResponse(key, batchId++, conn, query, rows));
                            if (batchSize <= MAX_BATCH_SIZE) {
                                batchSize *= 2;
                            }
                            rows = new ArrayList<>(batchSize);
                            try {
                                TimeUnit.MILLISECONDS.sleep(batchId * 10L);
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    }
                } else {
                    LOGGER.info("Query [{}]: OK", key);
                    rows.add(new SQLRowType(
                            String.valueOf(rowId++),
                            STATUS_COL_NAME_ONLY,
                            STATUS_OK_VALUE_ONLY));
                }
                resultSetConsumedTS = System.nanoTime();
            } catch (SQLException e) {
                LOGGER.error("Error query [{}]: {}", key, e.getMessage());
                eventListener.onSourceEvent(
                        SQLExecutor.this,
                        EventType.QUERY_FAILURE,
                        new SQLExecutionResponse(key, conn, query, e));
                return;
            }
            LOGGER.debug(
                    "Query [{}] Final rowId:{}, Final batchId:{}, batchSize:{}, Total rows:{}",
                    key, rowId, batchId, batchSize, rowId + 1);
            eventListener.onSourceEvent(
                    SQLExecutor.this,
                    EventType.RESULTS_COMPLETED,
                    new SQLExecutionResponse(key, batchId++, conn, query, rows));
            long totalElapsedMs = toMillis(resultSetConsumedTS - startTS);
            long queryExecutionElapsedMs = toMillis(queryExecutedTS - checkedConnectivityTS);
            long fetchResultsElapsedMs = toMillis(resultSetConsumedTS - queryExecutedTS);
            LOGGER.info("Query [{}] {} results, elapsed ms:{} (query:{}, fetch:{})",
                    key, rowId, totalElapsedMs, queryExecutionElapsedMs, fetchResultsElapsedMs);
        }));
        LOGGER.info("Query [{}] submitted", key);
    }

    private static final long toMillis(long nanos) {
        return TimeUnit.MILLISECONDS.convert(nanos, TimeUnit.NANOSECONDS);
    }

    private static String [] extractColumnNames(ResultSet rs) throws SQLException {
        PgResultSetMetaData metaData = (PgResultSetMetaData) rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        String [] columnNames = new String[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnNames[i] = metaData.getColumnName(i+1);
        }
        return columnNames;
    }

    private static Object [] extractColumns(String [] columnNames, ResultSet rs) throws SQLException {
        Object [] columns = new Object[columnNames.length];
        for (int i = 0; i < columnNames.length; i++) {
            columns[i] = rs.getObject(i + 1);
        }
        return columns;
    }
}
