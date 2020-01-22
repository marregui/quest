package io.crate.cli.backend;

import io.crate.cli.common.EventListener;
import io.crate.cli.common.EventSpeaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


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
    public static final int MAX_BATCH_SIZE = 20000;
    private static final String[] STATUS_COL_NAME = {"Status"};
    private static final int[] STATUS_COL_TYPE = { Types.VARCHAR };
    private static final Object[] STATUS_OK_VALUE = {"OK"};


    private final ConcurrentMap<String, Future<?>> runningQueries;
    private final ConcurrentMap<String, String> cancelRequests;
    private volatile ExecutorService cachedES;


    public SQLExecutor() {
        runningQueries = new ConcurrentHashMap<>();
        cancelRequests = new ConcurrentHashMap<>();
    }

    public void start() {
        if (null != cachedES) {
            throw new IllegalStateException("already started");
        }
        runningQueries.clear();
        AtomicInteger threadId = new AtomicInteger(0);
        cachedES = Executors.newCachedThreadPool(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName(String.format(
                    Locale.ENGLISH,
                    "%s-%d",
                    SQLExecutor.class.getSimpleName(),
                    threadId.incrementAndGet()));
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

    public void submit(SQLExecutionRequest request, EventListener<SQLExecutor, SQLExecutionResponse> eventListener) {
        if (null == cachedES) {
            throw new IllegalStateException("not started");
        }
        if (null == eventListener) {
            throw new IllegalStateException("eventListener cannot be null");
        }
        String sourceId = request.getSourceId();
        cancelSubmittedRequest(request);
        runningQueries.put(sourceId, cachedES.submit(() -> executeQuery(request, eventListener)));
        LOGGER.info("Query [{}] from [{}] submitted", request.getKey(), sourceId);
    }

    private final void executeQuery(SQLExecutionRequest request, EventListener<SQLExecutor, SQLExecutionResponse> eventListener) {
        long checkpointTs = System.nanoTime();
        long startTS = checkpointTs;
        String sourceId = request.getSourceId();
        String key = request.getKey();
        String query = request.getCommand();
        SQLConnection conn = request.getSQLConnection();
        LOGGER.info("Executing query [{}]: {}", key, query);
        if (false == conn.checkConnectivity()) {
            checkpointTs = System.nanoTime();
            LOGGER.error("While about to run [{}] from [{}], lost connectivity with {}", key, sourceId, conn);
            eventListener.onSourceEvent(
                    SQLExecutor.this,
                    EventType.QUERY_FAILURE,
                    new SQLExecutionResponse(
                            request,
                            toMillis(checkpointTs - startTS),
                            new RuntimeException(String.format(Locale.ENGLISH,"Connection [%s] is down", conn))));
            return;
        }
        LOGGER.info("Connectivity looks ok [{}]: {}", key, query);
        long queryExecutedTs;
        long queryExecutedMs;
        int rowId = 0;
        int batchId = 0;
        int batchSize = START_BATCH_SIZE;
        SQLTable resultsTable = SQLTable.emptyTable(request.getKey());
        checkpointTs = System.nanoTime();
        eventListener.onSourceEvent(
                SQLExecutor.this,
                EventType.QUERY_STARTED,
                new SQLExecutionResponse(
                        sourceId,
                        batchId++,
                        conn,
                        query,
                        toMillis(checkpointTs - startTS),
                        0L,
                        0L,
                        resultsTable));
        try (Statement stmt = conn.getConnection().createStatement()) {
            boolean checkResults = stmt.execute(query);
            queryExecutedTs = System.nanoTime();
            queryExecutedMs = toMillis(queryExecutedTs - startTS);
            if (checkResults) {
                eventListener.onSourceEvent(
                        SQLExecutor.this,
                        EventType.QUERY_FETCHING,
                        new SQLExecutionResponse(
                                sourceId,
                                batchId++,
                                conn,
                                query,
                                toMillis(checkpointTs - startTS),
                                queryExecutedMs,
                                toMillis(checkpointTs - queryExecutedTs),
                                resultsTable));
                ResultSet rs = stmt.getResultSet();
                boolean hasColumnMetadata = false;
                while (rs.next()) {
                    String cancelSourceId = cancelRequests.get(sourceId);
                    if (null != cancelSourceId && cancelSourceId.equals(sourceId)) {
                        break;
                    }
                    checkpointTs = System.nanoTime();
                    if (false == hasColumnMetadata) {
                        resultsTable.extractColumnMetadata(rs);
                        hasColumnMetadata = true;
                    }
                    resultsTable.addRow(String.valueOf(rowId++), rs);
                    if (0 == rowId % batchSize) {
                        eventListener.onSourceEvent(
                                SQLExecutor.this,
                                EventType.RESULTS_AVAILABLE,
                                new SQLExecutionResponse(
                                        sourceId,
                                        batchId++,
                                        conn,
                                        query,
                                        toMillis(checkpointTs - startTS),
                                        queryExecutedMs,
                                        toMillis(checkpointTs - queryExecutedTs),
                                        resultsTable));
                        if (batchSize <= MAX_BATCH_SIZE) {
                            batchSize *= 2;
                            resultsTable = new SQLTable(key);
                            hasColumnMetadata = false;
                        }
                    }
                }
            } else {
                LOGGER.info("Query [{}] from [{}]: OK", key, sourceId);
                resultsTable.setSingleRow(String.valueOf(rowId++),
                        STATUS_COL_NAME, STATUS_COL_TYPE, STATUS_OK_VALUE);
            }
        } catch (SQLException e) {
            LOGGER.error("Error query [{}] from [{}]: {}", key, sourceId, e.getMessage());
            checkpointTs = System.nanoTime();
            eventListener.onSourceEvent(
                    SQLExecutor.this,
                    EventType.QUERY_FAILURE,
                    new SQLExecutionResponse(request, toMillis(checkpointTs - startTS), e));
            return;
        }
        checkpointTs = System.nanoTime();
        String cancelSourceId = cancelRequests.remove(sourceId);
        boolean wasCancelled = null != cancelSourceId && cancelSourceId.equals(key);
        if (wasCancelled) {
            resultsTable.clear();
            eventListener.onSourceEvent(
                    SQLExecutor.this,
                    EventType.QUERY_CANCELLED,
                    new SQLExecutionResponse(
                            sourceId,
                            batchId++,
                            conn,
                            query,
                            toMillis(checkpointTs - startTS),
                            queryExecutedMs,
                            toMillis(checkpointTs - queryExecutedTs),
                            resultsTable));
        } else {
            eventListener.onSourceEvent(
                    SQLExecutor.this,
                    EventType.QUERY_COMPLETED,
                    new SQLExecutionResponse(
                            sourceId,
                            batchId++,
                            conn,
                            query,
                            toMillis(checkpointTs - startTS),
                            queryExecutedMs,
                            toMillis(checkpointTs - queryExecutedTs),
                            resultsTable));
            runningQueries.remove(key);
            LOGGER.info("Query [{}] from [{}] {} results, elapsed ms:{} (query:{}, fetch:{})",
                    key,
                    sourceId,
                    rowId,
                    toMillis(checkpointTs - startTS),
                    queryExecutedMs,
                    toMillis(checkpointTs - queryExecutedTs));
        }
    }

    public void cancelSubmittedRequest(SQLExecutionRequest request) {
        if (null == cachedES) {
            throw new IllegalStateException("not started");
        }
        String sourceId = request.getSourceId();
        Future<?> runningQuery = runningQueries.remove(sourceId);
        if (null != runningQuery && false == runningQuery.isDone() && false == runningQuery.isCancelled()) {
            LOGGER.info("Cancelling pre-existing query [{}] from [{}]", request.getKey(), sourceId);
            runningQuery.cancel(true);
            cancelRequests.put(sourceId, sourceId);
        }
    }

    private static final long toMillis(long nanos) {
        return TimeUnit.MILLISECONDS.convert(nanos, TimeUnit.NANOSECONDS);
    }
}
