package io.crate.cli.backend;


public class SQLExecutionResponse extends SQLExecutionRequest {

    private final SQLTable results;
    private final long totalElapsedMs;
    private final long queryExecutionElapsedMs;
    private final long fetchResultsElapsedMs;
    private Throwable error;


    public SQLExecutionResponse(SQLExecutionRequest request,
                                long totalElapsedMs,
                                long queryExecutionElapsedMs,
                                long fetchResultsElapsedMs) {
        super(request);
        this.results = SQLTable.emptyTable(request.getKey());
        this.totalElapsedMs = totalElapsedMs;
        this.queryExecutionElapsedMs = queryExecutionElapsedMs;
        this.fetchResultsElapsedMs = fetchResultsElapsedMs;
    }

    public SQLExecutionResponse(String key,
                                SQLConnection sqlConnection,
                                String command) {
        super(key, sqlConnection, command);
        this.results = SQLTable.emptyTable(key);
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
                                SQLTable results) {
        super(key, seqNo, sqlConnection, command);
        this.results = results;
        this.totalElapsedMs = totalElapsedMs;
        this.queryExecutionElapsedMs = queryExecutionElapsedMs;
        this.fetchResultsElapsedMs = fetchResultsElapsedMs;
    }

    public SQLExecutionResponse(String key,
                                SQLConnection sqlConnection,
                                String command,
                                long totalElapsedMs,
                                long queryExecutionElapsedMs,
                                long fetchResultsElapsedMs,
                                Throwable error) {
        super(key, sqlConnection, command);
        this.error = error;
        this.results = null;
        this.totalElapsedMs = totalElapsedMs;
        this.queryExecutionElapsedMs = queryExecutionElapsedMs;
        this.fetchResultsElapsedMs = fetchResultsElapsedMs;
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
