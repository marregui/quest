package io.crate.cli.connections;

import io.crate.cli.gui.common.HasKey;

import java.util.concurrent.atomic.AtomicLong;


public class SQLExecutionRequest implements HasKey {

    private static final AtomicLong MONOTONIC = new AtomicLong(0L);

    private final String key;
    private final long seqNo;
    private final SQLConnection sqlConnection;
    private final String command;

    public SQLExecutionRequest(String key, SQLConnection sqlConnection, String command) {
        this(key, MONOTONIC.getAndIncrement(), sqlConnection, command);
    }

    public SQLExecutionRequest(String key, long seqNo, SQLConnection sqlConnection, String command) {
        this.key = key;
        this.seqNo = seqNo;
        this.sqlConnection = sqlConnection;
        this.command = command;
    }

    public long getSeqNo() {
        return seqNo;
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
