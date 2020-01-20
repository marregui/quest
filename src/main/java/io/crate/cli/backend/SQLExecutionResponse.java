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
