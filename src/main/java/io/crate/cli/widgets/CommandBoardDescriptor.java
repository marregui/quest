package io.crate.cli.widgets;

import io.crate.cli.persistence.HasKey;
import io.crate.cli.persistence.StoreItemDescriptor;
import io.crate.cli.connections.SQLConnection;

import java.util.*;


public class CommandBoardDescriptor extends StoreItemDescriptor {


    public enum AttributeName implements HasKey {
        connection_name,
        board_contents;

        @Override
        public String getKey() {
            return name();
        }
    }


    private SQLConnection sqlConnection;


    public CommandBoardDescriptor(String name) {
        super(name);
        attributes.put(getStoreItemAttributeKey(AttributeName.connection_name), "");
        attributes.put(getStoreItemAttributeKey(AttributeName.board_contents), "");
    }

    public void setSqlConnection(SQLConnection conn) {
        sqlConnection = conn;
    }

    public SQLConnection getSqlConnection() {
        return sqlConnection;
    }

    public String getSqlConnectionName() {
        return getAttribute(AttributeName.connection_name);
    }

    public void setSqlConnectionName(String sqlConnectionName) {
        setAttribute(AttributeName.connection_name, sqlConnectionName, "");
    }

    public String getBoardContents() {
        return getAttribute(AttributeName.board_contents);
    }

    public void setBoardContents(String boardContents) {
        setAttribute(AttributeName.board_contents, boardContents, "");
    }

    @Override
    public final String getKey() {
        return String.format(
                Locale.ENGLISH,
                "%s %s",
                name,
                attributes.get(getStoreItemAttributeKey(AttributeName.connection_name)));
    }
}
