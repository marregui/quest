package io.crate.cli.connections;


import java.util.List;

public class SQLExecution {

    private final String key;
    private final String query;
    private final SQLConnection conn;
    private final List<SQLRowType> results;

    public SQLExecution(String key,
                        String query,
                        SQLConnection conn,
                        List<SQLRowType> results) {
        this.key = key;
        this.query = query;
        this.conn = conn;
        this.results = results;
    }

    public String getKey() {
        return key;
    }

    public String getQuery() {
        return query;
    }

    public SQLConnection getConn() {
        return conn;
    }

    public List<SQLRowType> getResults() {
        return results;
    }
}
