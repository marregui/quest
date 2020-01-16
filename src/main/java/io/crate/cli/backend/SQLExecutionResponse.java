package io.crate.cli.backend;

import java.util.Collections;
import java.util.List;


public class SQLExecutionResponse extends SQLExecutionRequest {

    private final List<SQLRowType> results;
    private final long totalElapsedMs;
    private final long queryExecutionElapsedMs;
    private final long fetchResultsElapsedMs;
    private Throwable error;


    public SQLExecutionResponse(SQLExecutionRequest request,
                                long totalElapsedMs,
                                long queryExecutionElapsedMs,
                                long fetchResultsElapsedMs) {
        super(request);
        this.results = Collections.emptyList();
        this.totalElapsedMs = totalElapsedMs;
        this.queryExecutionElapsedMs = queryExecutionElapsedMs;
        this.fetchResultsElapsedMs = fetchResultsElapsedMs;
    }

    public SQLExecutionResponse(String key,
                                SQLConnection sqlConnection,
                                String command) {
        super(key, sqlConnection, command);
        this.results = Collections.emptyList();
        totalElapsedMs = -1;
        queryExecutionElapsedMs = -1;
        fetchResultsElapsedMs = -1;
    }

    public SQLExecutionResponse(String key,
                                long seqNo,
                                SQLConnection sqlConnection,
                                String command,
                                long totalElapsedMs,
                                long queryExecutionElapsedMs,
                                long fetchResultsElapsedMs,
                                List<SQLRowType> results) {
        super(key, seqNo, sqlConnection, command);
        this.results = results;
        this.totalElapsedMs = totalElapsedMs;
        this.queryExecutionElapsedMs = queryExecutionElapsedMs;
        this.fetchResultsElapsedMs = fetchResultsElapsedMs;
    }

    public SQLExecutionResponse(String key,
                                SQLConnection sqlConnection,
                                String command,
                                Throwable error) {
        super(key, sqlConnection, command);
        this.error = error;
        this.results = null;
        totalElapsedMs = -1;
        queryExecutionElapsedMs = -1;
        fetchResultsElapsedMs = -1;
    }

    public List<SQLRowType> getResults() {
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
