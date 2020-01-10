package io.crate.cli.connections;

import io.crate.cli.gui.common.EventListener;
import io.crate.cli.gui.common.EventSpeaker;
import io.crate.shade.org.postgresql.jdbc.PgResultSetMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;


public class SQLExecutor implements EventSpeaker<SQLExecutor.EventType>, Closeable {

    public enum EventType {
        RESULTS_AVAILABLE,
        RESULTS_COMPLETED,
        QUERY_FAILURE
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SQLExecutor.class);
    private static final long BATCH_SIZE = 100;


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
        LOGGER.info(String.format(
                Locale.ENGLISH,
                "%s is running",
                SQLExecutor.class.getSimpleName()));
    }

    public void submit(SQLExecutionRequest request) {
        if (null == cachedES) {
            throw new IllegalStateException("not started");
        }
        String key = request.getKey();
        Future<?> result = runningQueries.get(key);
        if (null != result && false == result.isDone() && false == result.isCancelled()) {
            LOGGER.info("cancelling pre-existing query for key: {}", key);
            result.cancel(true);
        }
        runningQueries.put(key, cachedES.submit(() -> {
            SQLConnection conn = request.getSQLConnection();
            if (false == conn.checkConnectivity()) {
                return;
            }
            String query = request.getCommand();
            LOGGER.info("Executing query: {}", query);
            long rowId = 0;
            long batchId = 0;
            List<SQLRowType> rows = new ArrayList<>();
            try (Statement stmt = conn.getConnection().createStatement()) {
                boolean checkResults = stmt.execute(query);
                if (checkResults) {
                    ResultSet rs = stmt.getResultSet();
                    while (rs.next()) {
                        rows.add(new SQLRowType(String.valueOf(rowId++), extractColumns(rs)));
                        if (0 == rowId % BATCH_SIZE) {
                            eventListener.onSourceEvent(
                                    SQLExecutor.this,
                                    EventType.RESULTS_AVAILABLE,
                                    new SQLExecutionResponse(key, batchId++, conn, query, rows));
                            rows.clear();
                        }
                    }
                } else {
                    rows.add(new SQLRowType(String.valueOf(rowId++),
                            Collections.singletonMap("Status", "OK")));
                }
            } catch (SQLException e) {
                LOGGER.error("Error query '{}': {}", query, e.getMessage());
                eventListener.onSourceEvent(
                        SQLExecutor.this,
                        EventType.QUERY_FAILURE,
                        new SQLExecutionResponse(key, batchId++, conn, query, e));
                return;
            }
            LOGGER.info("{} results", rowId);
            eventListener.onSourceEvent(
                    SQLExecutor.this,
                    EventType.RESULTS_COMPLETED,
                    new SQLExecutionResponse(key, batchId++, conn, query, rows));
        }));
    }

    private static Map<String, Object> extractColumns(ResultSet rs) throws SQLException {
        PgResultSetMetaData metaData = (PgResultSetMetaData) rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        Map<String, Object> attributes = new LinkedHashMap<>(columnCount);
        for (int i = 1; i <= columnCount; i++) {
            attributes.put(metaData.getColumnName(i), rs.getObject(i));
        }
        return attributes;
    }

    @Override
    public void close() {
        if (null == cachedES) {
            throw new IllegalStateException("already closed");
        }
        cachedES.shutdownNow();
        try {
            cachedES.awaitTermination(200L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            cachedES = null;
            LOGGER.info(String.format(
                    Locale.ENGLISH,
                    "%s has finished",
                    SQLExecutor.class.getSimpleName()));
        }
    }
}
