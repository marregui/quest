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

package io.quest.backend;

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

import io.quest.model.EventConsumer;
import io.quest.model.EventProducer;
import io.quest.model.Conn;
import io.quest.model.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQLExecutor implements EventProducer<SQLExecutor.EventType>, Closeable {

    public enum EventType {
        STARTED,            // connection is valid, execution has started
        RESULTS_AVAILABLE,  // execution is going well so far, partial results have been collected
        COMPLETED,          // execution went well, all results have been collected
        CANCELLED,          // execution was cancelled
        FAILURE             // execution failed
    }

    public static final int MAX_BATCH_SIZE = 20_000;
    private static final int START_BATCH_SIZE = 100;
    public static final int QUERY_EXECUTION_TIMEOUT_SECS = 30;
    private static final Logger LOGGER = LoggerFactory.getLogger(SQLExecutor.class);

    private final ConcurrentMap<String, Future<?>> runningQueries;
    private ExecutorService executor;

    public SQLExecutor() {
        runningQueries = new ConcurrentHashMap<>();
    }

    public synchronized void start() {
        if (executor == null) {
            runningQueries.clear();
            final ThreadFactory threads = Executors.defaultThreadFactory();
            final String name = getClass().getSimpleName();
            executor = Executors.newFixedThreadPool(1, runnable -> {
                Thread thread = threads.newThread(runnable);
                thread.setDaemon(true);
                thread.setName(name);
                return thread;
            });
            LOGGER.info("{} is running", name);
        }
    }

    @Override
    public synchronized void close() {
        if (executor != null) {
            for (Future<?> query : runningQueries.values()) {
                if (!query.isDone() && !query.isCancelled()) {
                    query.cancel(true);
                }
            }
            executor.shutdownNow();
            try {
                executor.awaitTermination(400L, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                executor = null;
                runningQueries.clear();
                LOGGER.info("has finished");
            }
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
    public synchronized void submit(SQLExecutionRequest req, EventConsumer<SQLExecutor, SQLExecutionResponse> eventConsumer) {
        if (executor == null) {
            throw new IllegalStateException("not started");
        }
        if (eventConsumer == null) {
            throw new IllegalStateException("eventConsumer cannot be null");
        }
        cancelExistingRequest(req);
        String sourceId = req.getSourceId();
        runningQueries.put(sourceId, executor.submit(() -> executeRequest(req, eventConsumer)));
        LOGGER.info("Execution submitted [{}] from [{}]", req.getUniqueId(), sourceId);
    }

    public synchronized void cancelExistingRequest(SQLExecutionRequest req) {
        if (executor == null) {
            throw new IllegalStateException("not started");
        }
        final String sourceId = req.getSourceId();
        final Future<?> exec = runningQueries.remove(sourceId);
        if (exec != null && !exec.isDone() && !exec.isCancelled()) {
            exec.cancel(true);
            LOGGER.info("Cancelling [{}] from [{}]", req.getUniqueId(), sourceId);
        }
    }

    private void executeRequest(SQLExecutionRequest req, EventConsumer<SQLExecutor, SQLExecutionResponse> eventListener) {
        final long startNanos = System.nanoTime();
        final String sourceId = req.getSourceId();
        final Conn conn = req.getConnection();
        final String query = req.getSqlCommand();
        final Table table = new Table(req.getUniqueId());

        if (!conn.isValid()) {
            runningQueries.remove(sourceId);
            LOGGER.error("Failed [{}] from [{}], lost connection: {}", req.getUniqueId(), sourceId, conn);
            eventListener.onSourceEvent(
                    SQLExecutor.this,
                    EventType.FAILURE,
                    new SQLExecutionResponse(
                            req,
                            elapsedMs(startNanos),
                            new RuntimeException(String.format("Connection [%s] is not valid", conn)),
                            table
                    ));
            return;
        }
        LOGGER.info("Executing [{}] from [{}] over [{}]: {}", req.getUniqueId(), sourceId, conn.getUniqueId(), query);
        eventListener.onSourceEvent(
                SQLExecutor.this,
                EventType.STARTED,
                new SQLExecutionResponse(req, elapsedMs(startNanos), 0L, 0L, table));
        final long fetchStartNanos;
        final long execMs;
        int rowIdx = 0;
        int batchSize = START_BATCH_SIZE;
        try (Statement stmt = conn.getConnection().createStatement()) {
            stmt.setQueryTimeout(QUERY_EXECUTION_TIMEOUT_SECS);
            final boolean returnsResults = stmt.execute(query);
            fetchStartNanos = System.nanoTime();
            execMs = ms(fetchStartNanos - startNanos);
            if (returnsResults) {
                for (ResultSet rs = stmt.getResultSet(); rs.next(); ) {
                    final long fetchChkNanos = System.nanoTime();
                    if (!table.hasColMetadata()) {
                        table.setColMetadata(rs);
                    }
                    table.addRow(rowIdx++, rs);
                    if (0 == rowIdx % batchSize) {
                        batchSize = Math.min(batchSize * 2, MAX_BATCH_SIZE);
                        final long totalMs = ms(fetchChkNanos - startNanos);
                        final long fetchMs = ms(fetchChkNanos - fetchStartNanos);
                        eventListener.onSourceEvent(
                                SQLExecutor.this,
                                EventType.RESULTS_AVAILABLE,
                                new SQLExecutionResponse(req, totalMs, execMs, fetchMs, table));
                    }
                }
            }
        } catch (SQLException fail) {
            runningQueries.remove(sourceId);
            LOGGER.error("Failed [{}] from [{}]: {}", req.getUniqueId(), sourceId, fail.getMessage());
            eventListener.onSourceEvent(
                    SQLExecutor.this,
                    EventType.FAILURE,
                    new SQLExecutionResponse(req, elapsedMs(startNanos), fail, table));
            return;
        }
        runningQueries.remove(sourceId);
        EventType eventType = EventType.COMPLETED;
        final long endNanos = System.nanoTime();
        final long totalMs = ms(endNanos - startNanos);
        final long fetchMs = ms(endNanos - fetchStartNanos);
        LOGGER.info("{} [{}] {} rows, {} ms (exec:{}, fetch:{})",
                eventType.name(), req.getUniqueId(), table.size(), totalMs, execMs, fetchMs);
        eventListener.onSourceEvent(
                SQLExecutor.this,
                eventType,
                new SQLExecutionResponse(req, totalMs, execMs, fetchMs, table));
    }

    private static long elapsedMs(long start) {
        return ms(System.nanoTime() - start);
    }

    private static long ms(long nanos) {
        return TimeUnit.MILLISECONDS.convert(nanos, TimeUnit.NANOSECONDS);
    }
}
