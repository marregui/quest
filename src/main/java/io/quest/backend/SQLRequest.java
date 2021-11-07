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
 * Copyright (c) 2019 - 2022, Miguel Arregui a.k.a. marregui
 */

package io.quest.backend;

import java.util.UUID;

import io.quest.common.WithKey;


/**
 * A unit of work for the {@link SQLExecutor}.
 * <p>
 * Each request carries a SQL command and is be identified by a unique key. Upon
 * execution, the results are returned by means of one or many callbacks delivering
 * instances of {@link SQLResponse}. Responses must be seen as update messages on
 * the loading state of a single instance of {@link SQLTable}.
 */
public class SQLRequest implements WithKey<String> {

    private final String sourceId;
    private final String key;
    private final Conn conn;
    private final String sql;

    /**
     * Constructor used by {@link SQLResponse} to keep the relation between
     * a request and a response. This constructor is used to produce responses for
     * requests whose execution is successful.
     * 
     * @param sourceId command source, or requester, id
     * @param conn will send the command down this connection
     * @param command SQL statement to execute
     */
    public SQLRequest(String sourceId, Conn conn, String command) {
        this(sourceId, UUID.randomUUID().toString(), conn, command);
    }

    SQLRequest(SQLRequest request) {
        this(request.sourceId, request.key, request.conn, request.sql);
    }

    private SQLRequest(String sourceId, String key, Conn conn, String command) {
        this.sourceId = sourceId;
        this.key = key;
        this.conn = conn;
        this.sql = command;
    }

    /**
     * @return the identity of the request's source
     */
    public String getSourceId() {
        return sourceId;
    }

    /**
     * @return the SQL to execute
     */
    public String getSQL() {
        return sql;
    }

    /**
     * @return database connection
     */
    public Conn getConnection() {
        return conn;
    }

    @Override
    public String getKey() {
        return key;
    }
}
