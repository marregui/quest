package io.crate.cli.connections;

import io.crate.cli.gui.common.DefaultRowType;
import io.crate.shade.org.postgresql.jdbc.PgResultSetMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


public class SQLConnection extends ConnectionDescriptor implements Closeable {

    private final Logger logger;
    private final AtomicBoolean isConnected;
    private java.sql.Connection sqlConnection;


    public SQLConnection(String name, SQLConnection other) {
        this(name);
        if (null != other) {
            setHost(other.getHost());
            setPort(other.getPort());
            setUsername(other.getUsername());
            setPassword(other.getPassword());
        }
    }

    public SQLConnection(String name) {
        super(name);
        logger = LoggerFactory.getLogger(String.format(
                Locale.ENGLISH,
                "%s [%s]",
                SQLConnection.class.getSimpleName(),
                getKey()));
        isConnected = new AtomicBoolean();
    }

    public boolean isConnected() {
        return isConnected.get();
    }

    public boolean checkConnectivity() {
        try {
            this.isConnected.set(null != sqlConnection
                    && sqlConnection.isValid(10));
        } catch (SQLException e) {
            isConnected.set(false);
        }
        return isConnected.get();
    }

    public synchronized java.sql.Connection open() throws SQLException {
        if (isConnected.get()) {
            return sqlConnection;
        }
        logger.info("Connecting");
        sqlConnection = DriverManager.getConnection(getUrl(), loginProperties());
        isConnected.set(true);
        logger.info("Connected");
        return sqlConnection;
    }

    @Override
    public synchronized void close() {
        try {
            if (null != sqlConnection && false == sqlConnection.isClosed()) {
                logger.info("Closing");
                sqlConnection.close();
                logger.info("Closed");
            }
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        } finally {
            sqlConnection = null;
            isConnected.set(false);
        }
    }

    public void testConnection() throws SQLException {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(getUrl(), loginProperties());
            if (false == connection.isValid(10)) {
                throw new SQLException(String.format(
                        Locale.ENGLISH,
                        "connection with %s is not valid (tried for %d secs)",
                        this,
                        10));
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
