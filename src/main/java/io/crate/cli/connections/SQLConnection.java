package io.crate.cli.connections;

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
}
