package io.crate.cli.connections;

import io.crate.cli.gui.common.DefaultRowType;
import io.crate.cli.gui.common.EventListener;
import io.crate.shade.org.postgresql.jdbc.PgResultSetMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class SQLExecutor implements Closeable {

    public enum EventType {
        RESULTS_AVAILABLE
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(SQLExecutor.class);


    public static class SQLExecution {

        private final String key;
        private final String query;
        private final SQLConnection conn;
        private final List<DefaultRowType> results;

        private SQLExecution(String key, String query, SQLConnection conn, List<DefaultRowType> results) {
            this.key = key;
            this.query = query;
            this.conn = conn;
            this.results = results;
        }

        public String getKey() {
            return key;
        }

        public String getQuery() {
            return query;
        }

        public SQLConnection getConn() {
            return conn;
        }

        public List<DefaultRowType> getResults() {
            return results;
        }
    }


    private final EventListener<SQLExecutor, SQLExecution> eventListener;
    private final Map<String, Future<?>> runningQueries;
    private volatile ExecutorService cachedES;


    public SQLExecutor(EventListener<SQLExecutor, SQLExecution> eventListener) {
        this.eventListener = eventListener;
        runningQueries = new HashMap<>();
    }

    public boolean isRunning() {
        return null != cachedES && cachedES.isTerminated();
    }

    public void start() {
        if (null != cachedES) {
            return;
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

    public void submit(String key, SQLConnection conn, String query) {
        if (null == cachedES) {
            return;
        }
        Future<?> result = runningQueries.get(key);
        if (null != result) {
            result.cancel(true);
        }
        runningQueries.put(key, cachedES.submit(() -> {
            if (false == conn.checkConnectivity()) {
                return;
            }
            LOGGER.info(String.format(Locale.ENGLISH, "Executing query: %s", query));
            List<DefaultRowType> rows = new ArrayList<>();
            try (PreparedStatement stmt = conn.open().prepareStatement(query);
                 ResultSet rs = stmt.executeQuery()) {
                int rowId = 0;
                while (rs.next()) {
                    PgResultSetMetaData metaData = (PgResultSetMetaData) rs.getMetaData();
                    int resultSetSize = metaData.getColumnCount();
                    Map<String, Object> attributes = new LinkedHashMap<>(resultSetSize);
                    for (int i = 1; i <= resultSetSize; i++) {
                        attributes.put(metaData.getColumnName(i), rs.getObject(i));
                    }
                    rows.add(new DefaultRowType(String.valueOf(rowId++), attributes));
                }
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
            eventListener.onSourceEvent(
                    SQLExecutor.this,
                    EventType.RESULTS_AVAILABLE,
                    new SQLExecution(key, query, conn, rows));
        }));
    }

    @Override
    public void close() {
        if (null == cachedES) {
            return;
        }
        cachedES.shutdownNow();
        try {
            cachedES.awaitTermination(200L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        LOGGER.info(String.format(
                Locale.ENGLISH,
                "%s has finished",
                SQLExecutor.class.getSimpleName()));
    }
}
