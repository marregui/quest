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

package marregui.crate.cli.backend;

import java.util.UUID;

import marregui.crate.cli.WithKey;


/**
 * A unit of work for the {@link SQLExecutor}.
 * <p>
 * Each request carries a SQL command and can be identified by a unique key.
 * Upon execution, the results are carried in one or many instances of
 * {@link SQLExecResponse}. Responses always carry an instance of
 * {@link SQLTable}, whether it be empty or filled with data. The potentially
 * many responses that result from a request contain the full results, whereas a
 * single response in that set will contain a partial view on the full results.
 */
public class SQLExecRequest implements WithKey {

    private final String sourceId;
    private final String key;
    private final DBConn conn;
    private final String sql;

    /**
     * Constructor used by {@link SQLExecResponse} to keep the relation between
     * a request and a response. This constructor is used to produce responses for
     * requests whose execution is successful.
     * 
     * @param request original request
     */
    public SQLExecRequest(String sourceId, DBConn conn, String command) {
        this(sourceId, UUID.randomUUID().toString(), conn, command);
    }

    SQLExecRequest(SQLExecRequest request) {
        this(request.sourceId, request.key, request.conn, request.sql);
    }

    private SQLExecRequest(String sourceId, String key, DBConn conn, String command) {
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
    public DBConn getConnection() {
        return conn;
    }

    @Override
    public String getKey() {
        return key;
    }
}
