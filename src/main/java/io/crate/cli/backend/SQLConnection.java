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

import io.crate.cli.store.StoreItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.sql.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;


public class SQLConnection extends ConnectionStoreItem implements Closeable {

    private final transient Logger logger;
    private final transient AtomicBoolean isConnected;
    private transient java.sql.Connection sqlConnection;


    public SQLConnection(StoreItem other) {
        super(other);
        logger = LoggerFactory.getLogger(String.format(
                Locale.ENGLISH,
                "%s [%s]",
                SQLConnection.class.getSimpleName(),
                getKey()));
        isConnected = new AtomicBoolean();
    }

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
    
    public Connection getConnection() {
        return sqlConnection;
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
        sqlConnection.setAutoCommit(true);
        isConnected.set(true);
        logger.info("Connected");
        return sqlConnection;
    }

    @Override
    public synchronized void close() {
        try {
            if (null != sqlConnection && !sqlConnection.isClosed()) {
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
            if (!connection.isValid(10)) {
                throw new SQLException(String.format(
                        Locale.ENGLISH,
                        "connection with %s is not valid (tried for %d secs)",
                        this,
                        10));
            }
        } finally {
            if (null != connection) {
                try {
                    if (!connection.isClosed()) {
                        connection.close();
                    }
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
