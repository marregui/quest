/* **
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2020, Miguel Arregui a.k.a. marregui
 */

package marregui.crate.cli.widgets.conns;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiFunction;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

import marregui.crate.cli.GUITk;
import marregui.crate.cli.backend.DBConn;
import marregui.crate.cli.backend.DBConnAttrs;
import marregui.crate.cli.widgets.CellRenderer;
import marregui.crate.cli.widgets.PasswordRenderer;


/**
 * Table model used by the {@link ConnectionsManager}.
 */
class ConnectionsTableModel extends AbstractTableModel implements Closeable {

    /**
     * Factory method, creates a table that has a {@link ConnectionsTableModel}
     * model.
     * 
     * @param onTableModelEvent called each time a change to the data model occurs
     * @param selectionListener called each time a change to the selection occurs
     * @return a new table
     */
    static JTable createTable(TableModelListener onTableModelEvent, ListSelectionListener selectionListener) {
        ConnectionsTableModel tableModel = new ConnectionsTableModel();
        tableModel.addTableModelListener(onTableModelEvent);
        JTable table = new JTable(tableModel);
        table.setAutoCreateRowSorter(false);
        table.setRowHeight(ROW_HEIGHT);
        table.setGridColor(GUITk.APP_THEME_COLOR.darker());
        table.setFont(GUITk.TABLE_CELL_FONT);
        table.setDefaultRenderer(String.class, new CellRenderer());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(selectionListener);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setFont(GUITk.TABLE_HEADER_FONT);
        header.setForeground(GUITk.TABLE_HEADER_FONT_COLOR);
        TableColumnModel colModel = table.getTableHeader().getColumnModel();
        colModel.setColumnSelectionAllowed(false);
        colModel.getColumn(NAME_COL_IDX).setPreferredWidth(COL_WIDTHS[NAME_COL_IDX]);
        colModel.getColumn(HOST_COL_IDX).setPreferredWidth(COL_WIDTHS[HOST_COL_IDX]);
        colModel.getColumn(PORT_COL_IDX).setPreferredWidth(COL_WIDTHS[PORT_COL_IDX]);
        colModel.getColumn(USERNAME_COL_IDX).setPreferredWidth(COL_WIDTHS[USERNAME_COL_IDX]);
        colModel.getColumn(PASSWORD_COL_IDX).setPreferredWidth(COL_WIDTHS[PASSWORD_COL_IDX]);
        colModel.getColumn(CONNECTED_COL_IDX).setPreferredWidth(COL_WIDTHS[CONNECTED_COL_IDX]);
        colModel.getColumn(PASSWORD_COL_IDX).setCellRenderer(new PasswordRenderer());
        return table;
    }

    private static final long serialVersionUID = 1L;
    private static final int NAME_COL_IDX = 0;
    private static final int HOST_COL_IDX = 1;
    private static final int PORT_COL_IDX = 2;
    private static final int USERNAME_COL_IDX = 3;
    private static final int PASSWORD_COL_IDX = 4;
    private static final int CONNECTED_COL_IDX = 5;
    private static final int ROW_HEIGHT = 22;
    private static final int[] COL_WIDTHS = {
        200, 400, 100, 200, 200, 200
    };

    static final String NAME_COL = "name";

    private final List<DBConn> conns;
    private final Map<String, Integer> rowKeyToIdx;

    private ConnectionsTableModel() {
        conns = new ArrayList<>();
        rowKeyToIdx = new TreeMap<>();
    }

    void setConnections(List<DBConn> newConns) {
        conns.clear();
        rowKeyToIdx.clear();
        if (newConns != null && !newConns.isEmpty()) {
            for (int i = 0; i < newConns.size(); i++) {
                DBConn conn = newConns.get(i);
                conns.add(conn);
                rowKeyToIdx.put(conn.getKey(), i);
            }
        }
        fireTableDataChanged();
    }

    List<DBConn> getConnections() {
        return conns;
    }

    boolean contains(DBConn conn) {
        if (conn != null) {
            String rowKey = conn.getKey();
            Integer idx = rowKeyToIdx.get(rowKey);
            if (idx != null) {
                DBConn internalConn = conns.get(idx);
                return internalConn != null && internalConn.equals(conn);
            }
        }
        return false;
    }

    int addConnection(DBConn conn) {
        if (conn == null) {
            return -1;
        }
        conns.add(conn);
        int offset = conns.size() - 1;
        fireTableRowsInserted(offset, offset);
        return offset;
    }

    DBConn removeConnection(int rowIdx) {
        DBConn conn = conns.remove(rowIdx);
        fireTableRowsDeleted(rowIdx, rowIdx);
        return conn;
    }

    int getRowIdx(String connKey) {
        if (connKey == null) {
            return -1;
        }
        Integer idx = rowKeyToIdx.get(connKey);
        return idx != null ? idx.intValue() : -1;
    }

    @Override
    public int getRowCount() {
        return conns.size();
    }

    @Override
    public int getColumnCount() {
        return colNames != null ? colNames.length : 0;
    }

    @Override
    public String getColumnName(int colIdx) {
        return colNames[colIdx];
    }

    @Override
    public Class<?> getColumnClass(int colIdx) {
        return String.class;
    }

    @Override
    public void setValueAt(Object value, int rowIdx, int colIdx) {
        colSetter.call(conns.get(rowIdx), colNames[colIdx], value);
        fireTableCellUpdated(rowIdx, colIdx);
    }

    DBConn getValueAt(int rowIndex) {
        return (DBConn) getValueAt(rowIndex, -1);
    }

    @Override
    public Object getValueAt(int rowIdx, int colIdx) {
        if (colIdx == -1) {
            return conns.get(rowIdx);
        }
        return colGetter.apply(conns.get(rowIdx), colNames[colIdx]);
    }

    @Override
    public boolean isCellEditable(int rowIdx, int colIdx) {
        return CONNECTED_COL_IDX != colIdx && NAME_COL_IDX != colIdx;
    }

    @Override
    public void close() {
        conns.clear();
        rowKeyToIdx.clear();
        fireTableDataChanged();
    }

    private static final String CONNECTED_COL = "connected";

    private static final String[] colNames = {
        NAME_COL, DBConnAttrs.AttrName.host.name(), DBConnAttrs.AttrName.port.name(),
        DBConnAttrs.AttrName.username.name(), DBConnAttrs.AttrName.password.name(), CONNECTED_COL
    };

    private static final BiFunction<DBConn, String, Object> colGetter = (conn, attrName) -> {
        switch (attrName) {
            case NAME_COL:
                return conn.getName();
            case CONNECTED_COL:
                return conn.isOpen() ? "Yes" : "No";
            default:
                return conn.getAttribute(attrName);
        }
    };

    @FunctionalInterface
    private static interface ThreeArgsMethod<T, U, V> {

        void call(T t, U u, V v);
    }

    private static final ThreeArgsMethod<DBConn, String, Object> colSetter = (conn, attrName, value) -> {
        if (!NAME_COL.equals(attrName)) {
            conn.setAttribute(attrName, (String) value, "");
        }
    };
}
