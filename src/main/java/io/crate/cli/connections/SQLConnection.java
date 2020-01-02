package io.crate.cli.connections;

import io.crate.cli.gui.common.EventListener;
import io.crate.cli.gui.common.DefaultRowType;
import io.crate.shade.org.postgresql.jdbc.PgResultSetMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.sql.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;


public class SQLConnection extends ConnectionDescriptor implements Closeable {

    public enum EvenType {
        CONNECTION_ESTABLISHED,
        CONNECTION_LOST,
        CONNECTION_CLOSED
    }

    private static final int IS_CONNECTED_CHECK_TIMEOUT_SECS = 10;


    private final Logger logger;
    private final AtomicBoolean isConnectivityCheckOngoing;
    private final AtomicBoolean isConnected;
    private java.sql.Connection sqlConnection;
    private ScheduledExecutorService sqlConnectionStatusChecker;
    private ScheduledFuture<?> sqlConnectionStatusChecks;
    private EventListener<SQLConnection, SQLConnection> eventListener;


    public SQLConnection(String name) {
        super(name);
        logger = LoggerFactory.getLogger(String.format(
                Locale.ENGLISH,
                "%s [%s]",
                SQLConnection.class.getSimpleName(),
                getKey()));
        isConnectivityCheckOngoing = new AtomicBoolean();
        isConnected = new AtomicBoolean();
    }

    public boolean isConnected() {
        return isConnected.get();
    }

    private boolean checkConnectivity() {
        isConnectivityCheckOngoing.set(true);
        try {
            this.isConnected.set(null != sqlConnection
                    && sqlConnection.isValid(IS_CONNECTED_CHECK_TIMEOUT_SECS));
        } catch (SQLException e) {
            isConnected.set(false);
        } finally {
            isConnectivityCheckOngoing.set(false);
        }
        return isConnected.get();
    }

    public synchronized java.sql.Connection open(EventListener<SQLConnection, SQLConnection>  eventListener) throws SQLException {
        if (isConnected.get()) {
            return sqlConnection;
        }
        this.eventListener = eventListener;
        logger.info("Connecting");
        sqlConnection = DriverManager.getConnection(getUrl(), loginProperties());
        isConnected.set(true);
        logger.info("Connected");
        sqlConnectionStatusChecker = Executors.newScheduledThreadPool(1);
        sqlConnectionStatusChecks = sqlConnectionStatusChecker.scheduleAtFixedRate(
                this::sqlConnectionStatusChecks,
                IS_CONNECTED_CHECK_TIMEOUT_SECS * 2,
                IS_CONNECTED_CHECK_TIMEOUT_SECS / 2,
                TimeUnit.SECONDS);
        logger.info(String.format(
                Locale.ENGLISH,
                "Connectivity check every %d secs",
                IS_CONNECTED_CHECK_TIMEOUT_SECS / 2));
        if (null != eventListener) {
            eventListener.onSourceEvent(this, EvenType.CONNECTION_ESTABLISHED, this);
        }
        return sqlConnection;
    }

    private void sqlConnectionStatusChecks() {
        if (isConnectivityCheckOngoing.get()) {
            return;
        }
        if (false == checkConnectivity()) {
            logger.info("Connection is no longer valid");
            close();
            if (null != eventListener) {
                eventListener.onSourceEvent(this, EvenType.CONNECTION_LOST, this);
            }
        }
    }

    @Override
    public synchronized void close() {
        isConnectivityCheckOngoing.set(true);
        try {
            if (null != sqlConnectionStatusChecker) {
                sqlConnectionStatusChecks.cancel(true);
                sqlConnectionStatusChecker.shutdownNow();
            }
            if (null != sqlConnection && false == sqlConnection.isClosed()) {
                logger.info("Closing");
                sqlConnection.close();
                logger.info("Closed");
                if (null != eventListener) {
                    eventListener.onSourceEvent(this, EvenType.CONNECTION_CLOSED, this);
                }
            }
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        } finally {
            sqlConnection = null;
            sqlConnectionStatusChecker = null;
            sqlConnectionStatusChecks = null;
            isConnectivityCheckOngoing.set(false);
            isConnected.set(false);
        }
    }

    public void testConnection() throws SQLException {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(getUrl(), loginProperties());
            if (false == connection.isValid(IS_CONNECTED_CHECK_TIMEOUT_SECS)) {
                throw new SQLException(String.format(
                        Locale.ENGLISH,
                        "connection with %s is not valid (tried for %d secs)",
                        this,
                        IS_CONNECTED_CHECK_TIMEOUT_SECS));
            }
        } finally {
            if (null != connection) {
                try {
                    if (false == connection.isClosed()) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public List<DefaultRowType> executeQuery(String query) throws SQLException {
        if (false == checkConnectivity()) {
            throw new SQLException("Not connected");
        }
        logger.info(String.format(Locale.ENGLISH, "Executing query: %s", query));
        List<DefaultRowType> rows = new ArrayList<>();
        try (PreparedStatement stmt = sqlConnection.prepareStatement(query);
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
        }
        return rows;
    }
}
