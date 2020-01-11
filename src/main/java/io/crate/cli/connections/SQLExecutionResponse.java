package io.crate.cli.connections;

import java.util.List;


public class SQLExecutionResponse extends SQLExecutionRequest {

    private final List<SQLRowType> results;
    private Throwable error;

    public SQLExecutionResponse(String key,
                                SQLConnection sqlConnection,
                                String command,
                                List<SQLRowType> results) {
        super(key, sqlConnection, command);
        this.results = results;
    }

    public SQLExecutionResponse(String key,
                                long seqNo,
                                SQLConnection sqlConnection,
                                String command,
                                List<SQLRowType> results) {
        super(key, seqNo, sqlConnection, command);
        this.results = results;
    }

    public SQLExecutionResponse(String key,
                                SQLConnection sqlConnection,
                                String command,
                                Throwable error) {
        super(key, sqlConnection, command);
        this.error = error;
        this.results = null;
    }

    public SQLExecutionResponse(String key,
                                long seqNo,
                                SQLConnection sqlConnection,
                                String command,
                                Throwable error) {
        super(key, seqNo, sqlConnection, command);
        this.error = error;
        this.results = null;
    }

    public List<SQLRowType> getResults() {
        return results;
    }

    public Throwable getError() {
        return error;
    }
}
