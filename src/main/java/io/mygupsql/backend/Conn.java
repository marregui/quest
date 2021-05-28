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
 * Copyright 2020, Miguel Arregui a.k.a. marregui
 */

package io.mygupsql.backend;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Database connection, it extends base class {@link ConnAttrs} which
 * provides persistence for the attributes. It adds logging and connectivity
 * methods to open/close the connection with the database and check its
 * validity.
 */
public class Conn extends ConnAttrs implements Closeable {

    /**
     * Time to wait for the database operation used to validate the connection to
     * complete, 10 seconds.
     */
    private static final int ISVALID_TIMEOUT_SECS = 10;

    // non persistent attributes, transient:
    private final transient Logger logger;
    private final transient AtomicBoolean isOpen;
    private transient Connection conn;

    /**
     * Constructor.
     * 
     * @param name name of the connection
     */
    public Conn(String name) {
        super(name);
        isOpen = new AtomicBoolean();
        logger = LoggerFactory.getLogger(String.format("%s [%s]", getClass().getSimpleName(), getKey()));
    }

    /**
     * Deep copy constructor to create a new copy of the connection, with a
     * different name.
     * 
     * @param name  name of the connection
     * @param other source connection
     */
    public Conn(String name, Conn other) {
        this(name);
        if (other != null) {
            setHost(other.getHost());
            setPort(other.getPort());
            setUsername(other.getUsername());
            setPassword(other.getPassword());
            setDefault(other.isDefault());
        }
    }

    /**
     * Shallow copy constructor, used by the store, attributes are a reference to
     * the attributes of 'other'.
     * 
     * @param other original store item
     */
    public Conn(StoreEntry other) {
        super(other);
        isOpen = new AtomicBoolean();
        logger = LoggerFactory.getLogger(String.format("%s [%s]", getClass().getSimpleName(), getKey()));
    }

    /**
     * @return true if open() was called and thus the connection is open. No checks
     *         on validity.
     */
    public boolean isOpen() {
        return isOpen.get();
    }

    /**
     * Connection getter. No checks as to whether it is set, open and/or valid are
     * applied.
     * 
     * @return the connection
     */
    public Connection getConnection() {
        return conn;
    }

    /**
     * Returns true if the connection has not been closed and is still valid. The
     * driver shall submit a query on the connection or use some other mechanism
     * that positively verifies the connection is still valid when this method is
     * called.
     * <p>
     * The query submitted by the driver to validate the connection shall be
     * executed in the context of the current transaction. Waits up to 10 seconds
     * for the database operation used to validate the connection to complete. If
     * the timeout period expires before the operation completes, this method
     * returns false.
     *
     * @return true if the connection is valid, false otherwise
     */
    public boolean isValid() {
        try {
            isOpen.set(conn != null && conn.isValid(ISVALID_TIMEOUT_SECS));
        }
        catch (SQLException e) {
            isOpen.set(false);
            conn = null;
        }
        return isOpen.get();
    }

    /**
     * Opens the connection, sets it to auto commit.
     * 
     * @return the connection
     * @throws SQLException when the connection cannot be established
     */
    public synchronized Connection open() throws SQLException {
        if (isOpen.get()) {
            return conn;
        }
        logger.info("Connecting");
        conn = DriverManager.getConnection(getUri(), loginProperties());
        conn.setAutoCommit(true);
        isOpen.set(true);
        logger.info("Connected");
        return conn;
    }

    /**
     * Closes the connection.
     */
    @Override
    public synchronized void close() {
        try {
            if (conn != null && !conn.isClosed()) {
                logger.info("Closing");
                conn.close();
                logger.info("Closed");
            }
        }
        catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
        finally {
            conn = null;
            isOpen.set(false);
        }
    }

    /**
     * Opens the connection, checks its validity with a max time wait of 10 seconds
     * and then closes it. If any of this fails, a SQLException is thrown to mean
     * that there would not be connectivity should we try to open the connection.
     * 
     * @throws SQLException if the connection cannot be established
     */
    public void testConnectivity() throws SQLException {
        Connection testConn = null;
        try {
            testConn = DriverManager.getConnection(getUri(), loginProperties());
            if (!testConn.isValid(ISVALID_TIMEOUT_SECS)) {
                throw new SQLException(
                    String.format("connection with %s is not valid (tried for %d secs)", this, ISVALID_TIMEOUT_SECS));
            }
        }
        finally {
            if (testConn != null) {
                try {
                    if (!testConn.isClosed()) {
                        testConn.close();
                    }
                }
                catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
