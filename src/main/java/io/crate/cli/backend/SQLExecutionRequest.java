package io.crate.cli.backend;

import io.crate.cli.common.HasKey;

import java.util.concurrent.atomic.AtomicLong;


public class SQLExecutionRequest implements HasKey {

    private static final AtomicLong MONOTONIC = new AtomicLong(0L);

    private final String key;
    private final long seqNo;
    private final SQLConnection sqlConnection;
    private final String command;
    private volatile boolean wasCancelled;

    public SQLExecutionRequest(SQLExecutionRequest request) {
        this(request.getKey(), request.getSeqNo(), request.getSQLConnection(), request.getCommand());
        wasCancelled = request.wasCancelled();
    }

    public SQLExecutionRequest(String key, SQLConnection sqlConnection, String command) {
        this(key, MONOTONIC.getAndIncrement(), sqlConnection, command);
    }

    public SQLExecutionRequest(String key, long seqNo, SQLConnection sqlConnection, String command) {
        this.key = key;
        this.seqNo = seqNo;
        this.sqlConnection = sqlConnection;
        this.command = command;
    }

    public void setWasCancelled(boolean wasCancelled) {
        this.wasCancelled = wasCancelled;
    }

    public boolean wasCancelled() {
        return this.wasCancelled;
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
