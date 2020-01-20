package io.crate.cli.backend;

import io.crate.cli.common.HasKey;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;


public class SQLExecutionRequest implements HasKey {

    private static final AtomicLong MONOTONIC_SEQNO = new AtomicLong(0L);


    private final String sourceId;
    private final String key;
    private final long seqNo;
    private final SQLConnection sqlConnection;
    private final String command;


    public SQLExecutionRequest(SQLExecutionRequest request) {
        this(request.sourceId, request.key, request.seqNo, request.sqlConnection, request.command);
    }

    public SQLExecutionRequest(String sourceId, SQLConnection sqlConnection, String command) {
        this(sourceId, UUID.randomUUID().toString(), MONOTONIC_SEQNO.getAndIncrement(), sqlConnection, command);
    }

    public SQLExecutionRequest(String sourceId, long seqNo, SQLConnection sqlConnection, String command) {
        this(sourceId, UUID.randomUUID().toString(), seqNo, sqlConnection, command);
    }

    public SQLExecutionRequest(String sourceId, String key, long seqNo, SQLConnection sqlConnection, String command) {
        this.sourceId = sourceId;
        this.key = key;
        this.seqNo = seqNo;
        this.sqlConnection = sqlConnection;
        this.command = command;
    }

    public String getSourceId() {
        return sourceId;
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
