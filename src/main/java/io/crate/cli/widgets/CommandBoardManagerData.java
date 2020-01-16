package io.crate.cli.widgets;

import io.crate.cli.backend.SQLConnection;
import io.crate.cli.common.GUIFactory;
import io.crate.cli.persistence.CommandBoardStore;

import java.util.Arrays;


public class CommandBoardManagerData {


    private final CommandBoardStore<CommandBoardDescriptor> store;
    private final CommandBoardDescriptor[] descriptors;
    private int currentIdx;


    public CommandBoardManagerData() {
        int size = GUIFactory.NUM_BOARDS;
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
                int idx = GUIFactory.fromCommandBoardKey(di.getKey());
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
        return GUIFactory.toCommandBoardKey(currentIdx);
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
            descriptors[idx] = new CommandBoardDescriptor(GUIFactory.toCommandBoardKey(idx));
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
