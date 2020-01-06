package io.crate.cli.connections;

import io.crate.cli.gui.common.HasKey;


public class SQLExecutionRequest implements HasKey {

    private final String key;
    private final SQLConnection sqlConnection;
    private final String command;


    public SQLExecutionRequest(String key, SQLConnection sqlConnection, String command) {
        this.key = key;
        this.sqlConnection = sqlConnection;
        this.command = command;
    }

    public String getCommand() {
        return command;
    }

    public SQLConnection getSQLConnection() {
        return sqlConnection;
    }

    @Override
    public String getKey() {
        return key;
    }
}
