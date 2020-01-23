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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;


public class ConnectivityChecker implements Closeable {

    public static final int IS_CONNECTED_CHECK_TIMEOUT_SECS = 10;
    private static final int NUM_THREADS = 2;
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectivityChecker.class);


    private volatile ScheduledExecutorService scheduledES;
    private final AtomicBoolean isChecking;
    private final Supplier<List<SQLConnection>> connectionsSupplier;
    private final Consumer<Set<SQLConnection>> lostConnectionsConsumer;


    public ConnectivityChecker(Supplier<List<SQLConnection>> connectionsSupplier,
                               Consumer<Set<SQLConnection>> lostConnectionsConsumer) {
        this.connectionsSupplier = connectionsSupplier;
        this.lostConnectionsConsumer = lostConnectionsConsumer;
        isChecking = new AtomicBoolean();
    }

    public boolean isRunning() {
        return null != scheduledES && scheduledES.isTerminated();
    }

    public void start() {
        if (null != scheduledES) {
            throw new IllegalStateException("already started");
        }
        scheduledES = Executors.newScheduledThreadPool(NUM_THREADS);
        scheduledES.scheduleAtFixedRate(
                this::sqlConnectionStatusChecks,
                IS_CONNECTED_CHECK_TIMEOUT_SECS * 2,
                IS_CONNECTED_CHECK_TIMEOUT_SECS / 2,
                TimeUnit.SECONDS);
        LOGGER.info(String.format(
                Locale.ENGLISH,
                "Connectivity check every %d secs",
                IS_CONNECTED_CHECK_TIMEOUT_SECS / 2));
    }

    private void sqlConnectionStatusChecks() {
        if (false == isChecking.compareAndSet(false, true)) {
            return;
        }
        try {
            List<ScheduledFuture<?>> futureResults = connectionsSupplier.get()
                    .stream()
                    .filter(SQLConnection::isConnected)
                    .map(conn ->
                            scheduledES.schedule(() -> {
                                        boolean isConnected = conn.checkConnectivity();
                                        if (false == isConnected) {
                                            conn.close();
                                            return conn;
                                        }
                                        return null;
                                    },
                                    0, TimeUnit.SECONDS))
                    .collect(Collectors.toList());
            while (futureResults.size() > 0) {
                Set<SQLConnection> lostConnections = new HashSet<>();
                for (Iterator<ScheduledFuture<?>> futureIT = futureResults.iterator(); futureIT.hasNext(); ) {
                    ScheduledFuture<?> future = futureIT.next();
                    if (future.isDone()) {
                        try {
                            SQLConnection conn = (SQLConnection) future.get();
                            if (null != conn) {
                                lostConnections.add(conn);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            futureIT.remove();
                        }
                    } else if (future.isCancelled()) {
                        futureIT.remove();
                    }
                }
                if (lostConnections.size() > 0) {
                    lostConnectionsConsumer.accept(lostConnections);
                }
            }
        } finally {
            isChecking.set(false);
        }
    }

    @Override
    public void close() {
        if (null == scheduledES) {
            throw new IllegalStateException("not started");
        }
        scheduledES.shutdownNow();
        try {
            scheduledES.awaitTermination(200L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            scheduledES = null;
            LOGGER.info(String.format(
                    Locale.ENGLISH,
                    "Connectivity check stopped"));
        }
    }
}
