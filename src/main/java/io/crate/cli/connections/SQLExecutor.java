package io.crate.cli.connections;

import io.crate.cli.gui.common.EventListener;
import io.crate.cli.gui.common.EventSpeaker;
import io.crate.shade.org.postgresql.jdbc.PgResultSetMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;
import java.util.concurrent.*;


public class SQLExecutor implements EventSpeaker<SQLExecutor.EventType>, Closeable {

    public enum EventType {
        RESULTS_AVAILABLE
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
        Future<?> result = runningQueries.get(request.getKey());
        if (null != result) {
            result.cancel(true);
        }
        runningQueries.put(request.getKey(), cachedES.submit(() -> {
            String key = request.getKey();
            SQLConnection conn = request.getSQLConnection();
            String query = request.getCommand();
            if (false == conn.checkConnectivity()) {
                return;
            }
            LOGGER.info(String.format(Locale.ENGLISH, "Executing query: %s", query));
            List<SQLRowType> rows = new ArrayList<>();
            try (PreparedStatement stmt = conn.open().prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                long rowId = 0;
                while (rs.next()) {
                    PgResultSetMetaData metaData = (PgResultSetMetaData) rs.getMetaData();
                    int resultSetSize = metaData.getColumnCount();
                    Map<String, Object> attributes = new LinkedHashMap<>(resultSetSize);
                    for (int i = 1; i <= resultSetSize; i++) {
                        attributes.put(metaData.getColumnName(i), rs.getObject(i));
                    }
                    rows.add(new SQLRowType(String.valueOf(rowId++), attributes));
                    if (0 == rowId % BATCH_SIZE) {
                        eventListener.onSourceEvent(
                                SQLExecutor.this,
                                EventType.RESULTS_AVAILABLE,
                                new SQLExecutionResponse(key, conn, query, rows));
                        rows.clear();
                    }
                }
            } catch(Throwable throwable) {
                throw new RuntimeException(throwable);
            }
            if (rows.size() > 0) {
                eventListener.onSourceEvent(
                        SQLExecutor.this,
                        EventType.RESULTS_AVAILABLE,
                        new SQLExecutionResponse(key, conn, query, rows));
            }
        }));
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
