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

/**
 * The class embodying the responses emitted by the {@link SQLExecutor} as it progresses
 * through its query execution life cycle.
 * <p>
 * Each request carries a SQL query. When it is executed, the progress is progressively
 * notified to the listener by means of instances of this class. Responses contain a
 * reference to a unique instance of {@link SQLTable}.
 */
public class SQLResponse extends SQLRequest {

    private final SQLTable table;
    private final long totalMs;
    private final long executionMs;
    private final long fetchMs;
    private final Throwable error;

    SQLResponse(SQLRequest request, long totalMs, long execMs, long fetchMs, SQLTable table) {
        super(request);
        this.table = table;
        this.error = null;
        this.totalMs = totalMs;
        this.executionMs = execMs;
        this.fetchMs = fetchMs;
    }

    SQLResponse(SQLRequest request, long totalMs, Throwable error, SQLTable table) {
        super(request);
        this.totalMs = totalMs;
        this.error = error;
        this.table = table;
        this.executionMs = -1L;
        this.fetchMs = -1L;
    }

    /**
     * @return the results table
     */
    public SQLTable getTable() {
        return table;
    }

    /**
     * @return the error, null if none
     */
    public Throwable getError() {
        return error;
    }

    /**
     * @return total time elapsed since the start of the execution up until the
     *         moment the response is emitted
     */
    public long getTotalMs() {
        return totalMs;
    }

    /**
     * @return total time it took to execute the SQL query
     */
    public long getExecMs() {
        return executionMs;
    }

    /**
     * @return total time it took to fetch the results of executing the SQL query
     */
    public long getFetchMs() {
        return fetchMs;
    }
}
