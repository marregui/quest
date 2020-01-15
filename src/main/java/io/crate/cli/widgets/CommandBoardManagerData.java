package io.crate.cli.widgets;

import io.crate.cli.connections.SQLConnection;
import io.crate.cli.persistence.CommandBoardStore;

import java.util.Arrays;
import java.util.Locale;


public class CommandBoardManagerData {

    public static String toKey(int offset) {
        return String.valueOf((char) ('A' + offset));
    }

    public static int fromKey(String key, int max) {
        if (null == key || key.trim().length() < 1) {
            throw new IllegalArgumentException(String.format(
                    Locale.ENGLISH,
                    "key cannot be null, it must contain one non white char: %s",
                    key));
        }
        int offset = key.trim().charAt(0) - 'A';
        if (offset < 0 || offset >= max) {
            throw new IndexOutOfBoundsException(String.format(
                    Locale.ENGLISH,
                    "Key [%s] -> offset: %d (max: %d)",
                    key, offset, max));
        }
        return offset;
    }

    private final CommandBoardStore<CommandBoardDescriptor> store;
    private final CommandBoardDescriptor[] descriptors;
    private int currentIdx;


    public CommandBoardManagerData(int size) {
        store = new CommandBoardStore<>(CommandBoardDescriptor::new);
        store.load();
        descriptors = new CommandBoardDescriptor[Math.max(size, store.size() % size)];
        Arrays.fill(descriptors, null);
        currentIdx = 0;
        store.values().toArray(descriptors);
        arrangeDescriptorsByKey();
    }

    private void arrangeDescriptorsByKey() {
        for (int i=0; i < descriptors.length; i++) {
            CommandBoardDescriptor di = descriptors[i];
            if (null != di) {
                int idx = fromKey(di.getKey(), descriptors.length);
                if (idx != i) {
                    CommandBoardDescriptor tmp = descriptors[i];
                    descriptors[i] = descriptors[idx];
                    descriptors[idx] = tmp;
                }
            }
        }
    }

    public void store() {
        store.addAll(true, descriptors);
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
        if (null != conn) {
            current().setSqlConnectionName(conn.getName());
        }
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
