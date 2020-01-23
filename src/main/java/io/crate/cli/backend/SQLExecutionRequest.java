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

import io.crate.cli.common.HasKey;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;


public class SQLExecutionRequest implements HasKey {

    private static final AtomicLong MONOTONIC_SEQNO = new AtomicLong(0L);


    private final String sourceId;
    private final String key;
    private final long seqNo;
    private final SQLConnection sqlConnection;
    private final String command;


    public SQLExecutionRequest(SQLExecutionRequest request) {
        this(request.sourceId, request.key, request.seqNo, request.sqlConnection, request.command);
    }

    public SQLExecutionRequest(String sourceId, SQLConnection sqlConnection, String command) {
        this(sourceId, UUID.randomUUID().toString(), MONOTONIC_SEQNO.getAndIncrement(), sqlConnection, command);
    }

    public SQLExecutionRequest(String sourceId, long seqNo, SQLConnection sqlConnection, String command) {
        this(sourceId, UUID.randomUUID().toString(), seqNo, sqlConnection, command);
    }

    public SQLExecutionRequest(String sourceId, String key, long seqNo, SQLConnection sqlConnection, String command) {
        this.sourceId = sourceId;
        this.key = key;
        this.seqNo = seqNo;
        this.sqlConnection = sqlConnection;
        this.command = command;
    }

    public String getSourceId() {
        return sourceId;
    }

    public long getSeqNo() {
        return seqNo;
    }

    public String getCommand() {
        return command;
    }

    public SQLConnection getSQLConnection() {
        return sqlConnection;
    }

    @Override
    public String getKey() {
        return key;
    }
}
