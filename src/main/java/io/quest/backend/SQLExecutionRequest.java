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

import io.quest.model.Conn;
import io.quest.model.Table;
import io.quest.model.UniqueId;

import java.util.UUID;


/**
 * {@link SQLExecutor}'s unit of work.
 * <p>
 * Each request comes from a source, carries a SQL statement, and is identified by a unique
 * id. On execution, the results are returned by means of one, or many, callbacks delivering
 * instances of {@link SQLExecutionResponse}. Responses must be seen as delta updates on the
 * loading state of a single instance of {@link Table} updated by the executor.
 */
public class SQLExecutionRequest implements UniqueId<String> {
    private final String sourceId;
    private final String uniqueId;
    private final Conn conn;
    private final String sqlCommand;

    /**
     * Constructor used by {@link SQLExecutionResponse} to keep the relation between
     * a request and a response. This constructor is used to produce responses for
     * requests which execution are successful.
     *
     * @param sourceId   command source, or requester, id
     * @param conn       will send the command down this connection
     * @param sqlCommand SQL command to execute
     */
    public SQLExecutionRequest(String sourceId, Conn conn, String sqlCommand) {
        this(sourceId, UUID.randomUUID().toString(), conn, sqlCommand);
    }

    SQLExecutionRequest(SQLExecutionRequest request) {
        this(request.sourceId, request.uniqueId, request.conn, request.sqlCommand);
    }

    private SQLExecutionRequest(String sourceId, String uniqueId, Conn conn, String sqlCommand) {
        this.sourceId = sourceId;
        this.uniqueId = uniqueId;
        this.conn = conn;
        this.sqlCommand = sqlCommand;
    }

    public String getSourceId() {
        return sourceId;
    }

    public String getSqlCommand() {
        return sqlCommand;
    }

    public Conn getConnection() {
        return conn;
    }

    @Override
    public String getUniqueId() {
        return uniqueId;
    }
}
