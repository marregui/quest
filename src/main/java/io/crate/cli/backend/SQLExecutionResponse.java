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


public class SQLExecutionResponse extends SQLExecutionRequest {

    private final SQLTable results;
    private final long totalElapsedMs;
    private final long queryExecutionElapsedMs;
    private final long fetchResultsElapsedMs;
    private final Throwable error;


    public SQLExecutionResponse(SQLExecutionRequest request,
                                long totalElapsedMs,
                                Throwable error) {
        super(request);
        this.results = SQLTable.emptyTable(request.getKey());
        this.error = error;
        this.totalElapsedMs = totalElapsedMs;
        this.queryExecutionElapsedMs = -1L;
        this.fetchResultsElapsedMs = -1L;
    }

    public SQLExecutionResponse(String key,
                                long seqNo,
                                SQLConnection sqlConnection,
                                String command,
                                long totalElapsedMs,
                                long queryExecutionElapsedMs,
                                long fetchResultsElapsedMs,
                                SQLTable results) {
        super(key, seqNo, sqlConnection, command);
        this.results = results;
        this.error = null;
        this.totalElapsedMs = totalElapsedMs;
        this.queryExecutionElapsedMs = queryExecutionElapsedMs;
        this.fetchResultsElapsedMs = fetchResultsElapsedMs;
    }

    public SQLExecutionResponse(String sourceId, SQLConnection sqlConnection, String command) {
        super(sourceId,  sqlConnection, command);
        this.results = null;
        this.error = null;
        totalElapsedMs = -1;
        queryExecutionElapsedMs = -1;
        fetchResultsElapsedMs = -1;
    }

    public SQLTable getResults() {
        return results;
    }

    public Throwable getError() {
        return error;
    }

    public long getTotalElapsedMs() {
        return totalElapsedMs;
    }

    public long getQueryExecutionElapsedMs() {
        return queryExecutionElapsedMs;
    }

    public long getFetchResultsElapsedMs() {
        return fetchResultsElapsedMs;
    }
}
