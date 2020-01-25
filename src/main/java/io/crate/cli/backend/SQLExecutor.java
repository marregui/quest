/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */
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

    public static final int QUERY_EXECUTION_TIMEOUT_SECS = 60;
    public static final int SO_RCVBUF = 1 * 1024 * 1024; // 1 MB
    public static final int MAX_BATCH_SIZE = 20000;
    private static final int START_BATCH_SIZE = 100;


    public enum EventType {
        QUERY_STARTED,
        QUERY_FETCHING,
        QUERY_COMPLETED,
        QUERY_CANCELLED,
        QUERY_FAILURE,
        RESULTS_AVAILABLE
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SQLExecutor.class);
    private static final String[] STATUS_COL_NAME = {"Status"};
    private static final int[] STATUS_COL_TYPE = {Types.VARCHAR};
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
        cancelRequests.clear();
        AtomicInteger threadId = new AtomicInteger(0);
        cachedES = Executors.newCachedThreadPool(r -> {
            Thread executorThread = new Thread(r);
            executorThread.setDaemon(true);
            executorThread.setName(String.format(
                    Locale.ENGLISH,
                    "%s-%d",
                    SQLExecutor.class.getSimpleName(),
                    threadId.incrementAndGet()));
            return executorThread;
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
            cancelRequests.clear();
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

    public void cancelSubmittedRequest(SQLExecutionRequest request) {
        if (null == cachedES) {
            throw new IllegalStateException("not started");
        }
        String sourceId = request.getSourceId();
        Future<?> runningQuery = runningQueries.remove(sourceId);
        if (null != runningQuery && false == runningQuery.isDone() && false == runningQuery.isCancelled()) {
            cancelRequests.put(sourceId, sourceId);
            LOGGER.info("Cancelling pre-existing query [{}] from [{}]", request.getKey(), sourceId);
            runningQuery.cancel(true);
        }
    }

    private final void executeQuery(SQLExecutionRequest request, EventListener<SQLExecutor, SQLExecutionResponse> eventListener) {
        long startTS = System.nanoTime();
        String sourceId = request.getSourceId();
        SQLConnection conn = request.getSQLConnection();
        String query = request.getCommand();
        LOGGER.info("Executing query [{}] from [{}] on connection {}: {}",
                request.getKey(), sourceId, conn, query);
        if (false == conn.checkConnectivity()) {
            LOGGER.error("While about to run [{}] from [{}], lost connectivity with: {}",
                    request.getKey(), sourceId, conn);
            eventListener.onSourceEvent(
                    SQLExecutor.this,
                    EventType.QUERY_FAILURE,
                    new SQLExecutionResponse(
                            request,
                            0L,
                            new RuntimeException(String.format(
                                    Locale.ENGLISH,
                                    "Connection [%s] is down",
                                    conn))));
            cancelRequests.remove(sourceId); // should there be any pending
            return;
        }
        LOGGER.info("Connectivity looks ok [{}] from [{}]: {}", request.getKey(), sourceId, conn);

        long queryExecutedTs = System.nanoTime();
        long queryExecutedMs;
        SQLTable resultsTable = SQLTable.emptyTable(request.getKey());
        int batchSize = START_BATCH_SIZE;
        int batchId = 0;
        int rowId = 0;
        eventListener.onSourceEvent(
                SQLExecutor.this,
                EventType.QUERY_STARTED,
                new SQLExecutionResponse(
                        sourceId,
                        batchId++,
                        conn,
                        query,
                        toMillis(queryExecutedTs - startTS),
                        0L,
                        0L,
                        resultsTable));
        try (Statement stmt = conn.getConnection().createStatement()) {

            // execute query, this can take seconds
            stmt.setQueryTimeout(QUERY_EXECUTION_TIMEOUT_SECS);
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
                                queryExecutedMs,
                                queryExecutedMs,
                                0L,
                                resultsTable));
                ResultSet rs = stmt.getResultSet();
                boolean hasColumnMetadata = false;
                while (rs.next()) {
                    if (null != cancelRequests.remove(sourceId)) {
                        break;
                    }
                    long checkpointTs = System.nanoTime();
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
                            resultsTable = new SQLTable(request.getKey());
                            hasColumnMetadata = false;
                        }
                    }
                }
            } else {
                LOGGER.info("Query [{}] from [{}]: OK", request.getKey(), sourceId);
                resultsTable.setSingleRow(String.valueOf(rowId++),
                        STATUS_COL_NAME, STATUS_COL_TYPE, STATUS_OK_VALUE);
            }
        } catch (SQLException e) {
            runningQueries.remove(sourceId);
            cancelRequests.remove(sourceId);
            LOGGER.error("Error query [{}] from [{}]: {}",
                    request.getKey(), sourceId, e.getMessage());
            eventListener.onSourceEvent(
                    SQLExecutor.this,
                    EventType.QUERY_FAILURE,
                    new SQLExecutionResponse(
                            request,
                            toMillis(System.nanoTime() - startTS),
                            e));
            return;
        }

        long checkpointTs = System.nanoTime();
        runningQueries.remove(sourceId);
        String cancelSourceId = cancelRequests.remove(sourceId);
        boolean wasCancelled = null != cancelSourceId;
        EventType eventType = wasCancelled ? EventType.QUERY_CANCELLED : EventType.QUERY_COMPLETED;
        if (wasCancelled) {
            resultsTable.clear();
        }
        eventListener.onSourceEvent(
                SQLExecutor.this,
                eventType,
                new SQLExecutionResponse(
                        sourceId,
                        batchId++,
                        conn,
                        query,
                        toMillis(checkpointTs - startTS),
                        queryExecutedMs,
                        toMillis(checkpointTs - queryExecutedTs),
                        resultsTable));
        LOGGER.info("{} [{}] from [{}] {} results, elapsed milliseconds:{} (query:{}, fetch:{})",
                eventType,
                request.getKey(),
                sourceId,
                rowId,
                toMillis(checkpointTs - startTS),
                queryExecutedMs,
                toMillis(checkpointTs - queryExecutedTs));
    }

    private static final long toMillis(long nanos) {
        return TimeUnit.MILLISECONDS.convert(nanos, TimeUnit.NANOSECONDS);
    }
}
