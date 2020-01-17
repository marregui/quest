package io.crate.cli.widgets;

import io.crate.cli.backend.SQLConnection;
import io.crate.cli.common.GUIToolkit;
import io.crate.cli.common.HasKey;
import io.crate.cli.store.JsonStore;
import io.crate.cli.store.Store;

import java.util.Arrays;
import java.util.Locale;

import io.crate.cli.store.StoreItem;


public class CommandBoardManagerData {


    public enum AttributeName implements HasKey {
        connection_name,
        board_contents;

        @Override
        public String getKey() {
            return name();
        }
    }


    public static class BoardItem extends StoreItem {

        private transient SQLConnection sqlConnection;


        public BoardItem(StoreItem other) {
            super(other);
        }

        public BoardItem(String name) {
            super(name);
        }

        public void setSqlConnection(SQLConnection conn) {
            sqlConnection = conn;
        }

        public SQLConnection getSqlConnection() {
            return sqlConnection;
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


    private final BoardItem[] descriptors;
    private int currentIdx;
    private final Store<BoardItem> store;


    public CommandBoardManagerData() {
        int size = GUIToolkit.NUM_COMMAND_BOARDS;
        store = new JsonStore<>(GUIToolkit.COMMAND_BOARD_MANAGER_STORE, BoardItem.class);
        store.load();
        descriptors = new BoardItem[Math.max(size, store.size() % size)];
        Arrays.fill(descriptors, null);
        currentIdx = 0;
        store.values().toArray(descriptors);
        arrangeDescriptorsByKey();
    }

    private void arrangeDescriptorsByKey() {
        for (int i=0; i < descriptors.length; i++) {
            BoardItem di = descriptors[i];
            if (null != di) {
                int idx = GUIToolkit.fromCommandBoardKey(di.getKey());
                if (idx != i) {
                    BoardItem tmp = descriptors[i];
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
        return GUIToolkit.toCommandBoardKey(currentIdx);
    }

    public int getCurrentIdx() {
        return currentIdx;
    }

    public void setCurrentIdx(int idx) {
        currentIdx = idx;
    }

    private BoardItem current() {
        return current(currentIdx);
    }

    private BoardItem current(int idx) {
        if (null == descriptors[idx]) {
            descriptors[idx] = new BoardItem(GUIToolkit.toCommandBoardKey(idx));
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
            current().setAttribute(AttributeName.connection_name, conn.getName());
        }
    }

    public String getCurrentBoardContents() {
        return current().getAttribute(AttributeName.board_contents);
    }

    public void setCurrentBoardContents(String text) {
        current().setAttribute(AttributeName.board_contents, text);
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
