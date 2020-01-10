package io.crate.cli.connections;

import java.util.List;


public class SQLExecutionResponse extends SQLExecutionRequest {

    private final List<SQLRowType> results;
    private Throwable error;


    public SQLExecutionResponse(String key,
                                long seqNo,
                                SQLConnection sqlConnection,
                                String command,
                                List<SQLRowType> results) {
        super(key, seqNo, sqlConnection, command);
        this.results = results;
        this.error = null;
    }

    public SQLExecutionResponse(String key,
                                long seqNo,
                                SQLConnection sqlConnection,
                                String command,
                                Throwable error) {
        super(key, seqNo, sqlConnection, command);
        this.results = null;
        this.error = error;
    }

    public List<SQLRowType> getResults() {
        return results;
    }

    public Throwable getError() {
        return error;
    }
}
