package io.crate.cli.connections;

import java.util.List;


public class SQLExecutionResponse extends SQLExecutionRequest {

    private final List<SQLRowType> results;


    public SQLExecutionResponse(String key,
                                SQLConnection sqlConnection,
                                String command,
                                List<SQLRowType> results) {
        super(key, sqlConnection, command);
        this.results = results;
    }

    public List<SQLRowType> getResults() {
        return results;
    }
}
