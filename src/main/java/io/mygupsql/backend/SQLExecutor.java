/* **
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) 2019 - 2022, Miguel Arregui a.k.a. marregui
 */

package io.mygupsql.backend;

import java.io.Closeable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import io.mygupsql.EventConsumer;
import io.mygupsql.EventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Single threaded SQL statements executor. The daemon thread serialises
 * execution of SQL statements, identifying each by source id. For a given
 * source, only one SQL execution request is allowed to run at the time.
 * Submitting a new request will result in terminating an already running
 * request, or if the request has not been executed yet it will be preempted
 * from running and the new request will take its place.
 */
public class SQLExecutor implements EventProducer<SQLExecutor.EventType>, Closeable {

    /**
     * Query execution has the following state machine:
     */
    public enum EventType {
        /**
         * The connection is valid, execution has started.
         */
        STARTED,
        /**
         * Query execution is going well so far, partial results have been collected.
         */
        RESULTS_AVAILABLE,
        /**
         * Query execution went well, all results have been collected.
         */
        COMPLETED,
        /**
         * Query execution was cancelled.
         */
        CANCELLED,
        /**
         * Query execution failed.
         */
        FAILURE
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SQLExecutor.class);
    static final int QUERY_EXECUTION_TIMEOUT_SECS = 60;
    static final int MAX_BATCH_SIZE = 20_000;
    private static final int START_BATCH_SIZE = 100;

    private final ConcurrentMap<String, Future<?>> runningQueries;
    private final ConcurrentMap<String, String> cancelRequests;
    private ExecutorService executor;

    public SQLExecutor() {
        runningQueries = new ConcurrentHashMap<>();
        cancelRequests = new ConcurrentHashMap<>();
    }

    /**
     * Starts the single daemon thread pool executor.
     */
    public synchronized void start() {
        if (executor != null) {
            throw new IllegalStateException("already started");
        }
        runningQueries.clear();
        cancelRequests.clear();
        ThreadFactory threads = Executors.defaultThreadFactory();
        String name = getClass().getSimpleName();
        executor = Executors.newFixedThreadPool(1, runnable -> {
            Thread thread = threads.newThread(runnable);
            thread.setDaemon(true);
            thread.setName(name);
            return thread;
        });
        LOGGER.info("{} is running", name);
    }

    /**
     * Terminates the single daemon thread pool executor, cancelling all submitted
     * queries.
     */
    @Override
    public synchronized void close() {
        if (executor == null) {
            throw new IllegalStateException("already closed");
        }
        // cancel running queries
        for (Future<?> query : runningQueries.values()) {
            if (!query.isDone() && !query.isCancelled()) {
                query.cancel(true);
            }
        }
        executor.shutdownNow();
        try {
            executor.awaitTermination(200L, TimeUnit.MILLISECONDS);
        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        finally {
            executor = null;
            runningQueries.clear();
            cancelRequests.clear();
            LOGGER.info("has finished");
        }
    }

    /**
     * Submits a SQL execution request. Executions are identified by the request's
     * source id. If a request by the same source has already been submitted, it is
     * preempted from running, or cancelled if running.
     * 
     * @param req           contains the SQL to be executed
     * @param eventConsumer receiver of responses to the request
     */
    public synchronized void submit(SQLRequest req, EventConsumer<SQLExecutor, SQLResponse> eventConsumer) {
        if (executor == null) {
            throw new IllegalStateException("not started");
        }
        if (eventConsumer == null) {
            throw new IllegalStateException("eventListener cannot be null");
        }
        cancelSubmittedRequest(req);
        String sourceId = req.getSourceId();
        runningQueries.put(sourceId, executor.submit(() -> executeRequest(req, eventConsumer)));
        LOGGER.info("Execution submitted [{}] from [{}]", req.getKey(), sourceId);
    }

    /**
     * @param req contains the source id and request key
     */
    public synchronized void cancelSubmittedRequest(SQLRequest req) {
        if (executor == null) {
            throw new IllegalStateException("not started");
        }
        String sourceId = req.getSourceId();
        Future<?> exec = runningQueries.remove(sourceId);
        if (exec != null && !exec.isDone() && !exec.isCancelled()) {
            cancelRequests.put(sourceId, req.getKey());
            LOGGER.info("Cancelling [{}] from [{}]", req.getKey(), sourceId);
            exec.cancel(true);
        }
    }

    private void executeRequest(SQLRequest req, EventConsumer<SQLExecutor, SQLResponse> eventListener) {
        final long start = System.nanoTime();
        String sourceId = req.getSourceId();
        Conn conn = req.getConnection();
        String query = req.getSQL();
        SQLTable table = new SQLTable(req.getKey());
        if (!conn.isValid()) {
            runningQueries.remove(sourceId);
            cancelRequests.remove(sourceId);
            LOGGER.error("Failed [{}] from [{}], lost connection: {}", req.getKey(), sourceId, conn);
            RuntimeException fail = new RuntimeException(String.format("Connection [%s] is not valid", conn));
            eventListener.onSourceEvent(SQLExecutor.this, EventType.FAILURE,
                new SQLResponse(req, ms(System.nanoTime() - start), fail, table));
            return;
        }
        String reqKey = cancelRequests.remove(sourceId);
        if (reqKey != null && reqKey.equals(req.getKey())) {
            runningQueries.remove(sourceId);
            long totalMs = ms(System.nanoTime() - start);
            LOGGER.info("Cancelled [{}] from [{}], {} ms", reqKey, sourceId, totalMs);
            eventListener.onSourceEvent(SQLExecutor.this, EventType.CANCELLED,
                new SQLResponse(req, totalMs, 0L, 0L, table));
        }
        else {
            LOGGER.info("Executing [{}] from [{}] over [{}]: {}", req.getKey(), sourceId, conn.getKey(), query);
            eventListener.onSourceEvent(SQLExecutor.this, EventType.STARTED,
                new SQLResponse(req, ms(System.nanoTime() - start), 0L, 0L, table));
            final long fetchStart;
            final long execMs;
            long rowId = 0;
            int batchSize = START_BATCH_SIZE;
            try (Statement stmt = conn.getConnection().createStatement()) {
                stmt.setQueryTimeout(QUERY_EXECUTION_TIMEOUT_SECS); // limit query execution time
                boolean returnsResults = stmt.execute(query);
                fetchStart = System.nanoTime();
                execMs = ms(fetchStart - start);
                if (returnsResults) {
                    for (ResultSet rs = stmt.getResultSet(); rs.next();) {
                        reqKey = cancelRequests.get(sourceId);
                        if (reqKey != null && reqKey.equals(req.getKey())) {
                            break;
                        }
                        long fetchChk = System.nanoTime();
                        if (!table.hasColMetadata()) {
                            table.setColMetadata(rs);
                        }
                        table.addRow(String.valueOf(rowId++), rs);
                        if (0 == rowId % batchSize) {
                            batchSize = Math.min(batchSize * 2, MAX_BATCH_SIZE);
                            long totalMs = ms(fetchChk - start);
                            long fetchMs = ms(fetchChk - fetchStart);
                            eventListener.onSourceEvent(SQLExecutor.this, EventType.RESULTS_AVAILABLE,
                                new SQLResponse(req, totalMs, execMs, fetchMs, table));
                        }
                    }
                }
            }
            catch (SQLException fail) {
                runningQueries.remove(sourceId);
                cancelRequests.remove(sourceId);
                LOGGER.error("Failed [{}] from [{}]: {}", req.getKey(), sourceId, fail.getMessage());
                eventListener.onSourceEvent(SQLExecutor.this, EventType.FAILURE,
                    new SQLResponse(req, ms(System.nanoTime() - start), fail, table));
                return;
            }
            runningQueries.remove(sourceId);
            EventType eventType = EventType.COMPLETED;
            long end = System.nanoTime();
            long totalMs = ms(end - start);
            long fetchMs = ms(end - fetchStart);
            reqKey = cancelRequests.remove(sourceId);
            if (reqKey != null && reqKey.equals(req.getKey())) {
                eventType = EventType.CANCELLED;
            }
            LOGGER.info("{} [{}] {} rows, {} ms (exec:{}, fetch:{})", eventType.name(), req.getKey(), table.size(), totalMs,
                execMs, fetchMs);
            eventListener.onSourceEvent(SQLExecutor.this, eventType,
                new SQLResponse(req, totalMs, execMs, fetchMs, table));
        }
    }

    private static long ms(long nanos) {
        return TimeUnit.MILLISECONDS.convert(nanos, TimeUnit.NANOSECONDS);
    }
}
