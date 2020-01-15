package io.crate.cli.widgets;

import io.crate.cli.connections.SQLConnection;
import io.crate.cli.persistence.CommandBoardStore;

import java.util.Arrays;


public class CommandBoardManagerData {

    public static String toKey(int offset) {
        return String.valueOf((char) ('A' + offset));
    }

    private final CommandBoardStore<CommandBoardDescriptor> store;
    private final CommandBoardDescriptor[] descriptors;
    private int currentIdx;


    public CommandBoardManagerData(int size) {
        store = new CommandBoardStore<>(CommandBoardDescriptor::new);
        store.load();
        descriptors = new CommandBoardDescriptor[Math.max(size, store.size())];
        Arrays.fill(descriptors, null);
        currentIdx = 0;
        store.values().toArray(descriptors);
        Arrays.sort(descriptors);
    }

    public void store() {
//        store.clear();
//        for (int i = 0; i < descriptors.length; i++) {
//            CommandBoardDescriptor descriptor = descriptors[i];
//            if (null != descriptor) {
//                store.store(descriptor);
//            }
//        }
//        store.store();
    }

    public String getCurrentKey() {
        return toKey(currentIdx);
    }

    public int getCurrentIdx() {
        return currentIdx;
    }

    public void setCurrentIdx(int idx) {
        currentIdx = idx;
    }

    private CommandBoardDescriptor current() {
        return current(currentIdx);
    }

    private CommandBoardDescriptor current(int idx) {
        if (null == descriptors[idx]) {
            descriptors[idx] = new CommandBoardDescriptor(toKey(idx));
        }
        return descriptors[idx];
    }

    public SQLConnection getCurrentSQLConnection() {
        SQLConnection conn = current().getSqlConnection();
        if (null == conn) {
            conn = findFirstConnection(true);
            if (null == conn) {
                conn = findFirstConnection(false);
            }
            current().setSqlConnection(conn);
        }
        return conn;
    }

    public void setCurrentSQLConnection(SQLConnection conn) {
        current().setSqlConnection(conn);
    }

    public String getCurrentBoardContents() {
        return current().getBoardContents();
    }

    public void setCurrentBoardContents(String text) {
        current().setBoardContents(text);
    }

    private SQLConnection findFirstConnection(boolean checkIsConnected) {
        for (int i = 0; i < descriptors.length; i++) {
            SQLConnection conn = current(i).getSqlConnection();
            if (null != conn) {
                if (checkIsConnected) {
                    if (conn.isConnected()) {
                        return conn;
                    }
                } else {
                    return conn;
                }
            }
        }
        return null;
    }
}
