package io.crate.cli.gui.widgets;

import io.crate.cli.connections.ConnectionDescriptorStore;
import io.crate.cli.connections.SQLConnection;
import io.crate.cli.gui.common.*;
import io.crate.cli.connections.AttributeName;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiFunction;


class SQLConnectionEditor extends JPanel {

    enum EventType {
        TEST,
        CONNECT,
        ADD_CONNECTION,
        REMOVE_CONNECTION,
        UPDATE_CONNECTION_ATTRIBUTES,
        RELOAD_CONNECTIONS,
        BACK
    }

    private static final int NAME_IDX = 0;
    private static final int HOST_IDX = 1;
    private static final int PORT_IDX = 2;
    private static final int USERNAME_IDX = 3;
    private static final int PASSWORD_IDX = 4;
    private static final int CONNECTED_IDX = 5;
    private static final String NAME = "name";
    private static final String CONNECTED = "connected";
    private static final String[] COLUMN_NAMES = {
            NAME,
            AttributeName.host.name(),
            AttributeName.port.name(),
            AttributeName.username.name(),
            AttributeName.password.name(),
            CONNECTED
    };
    private static final int[] COLUMN_WIDTHS = {
            200,
            400,
            100,
            200,
            200,
            100
    };
    private static final Class<?>[] COLUMN_CLASSES = {
            String.class,
            String.class,
            String.class,
            String.class,
            String.class,
            String.class
    };
    private static final BiFunction<SQLConnection, String, Object> ATTR_GETTER =
            (conn, attrName) -> {
                switch (attrName) {
                    case NAME:
                        return conn.getName();
                    case CONNECTED:
                        return String.valueOf(conn.isConnected());
                    default:
                        return conn.getAttribute(attrName);
                }
            };
    private static final TriFunction<SQLConnection, String, Object, Object> ATTR_SETTER =
            (conn, attrName, value) -> {
                switch (attrName) {
                    case NAME:
                        return conn.setName((String) value);
                    default:
                        return conn.setAttribute(attrName, (String) value);
                }
            };


    private final JButton testButton;
    private final JButton connectButton;
    private final JButton addButton;
    private final JButton removeButton;
    private final JButton reloadButton;
    private final JButton backButton;
    private final EventListener<SQLConnectionEditor, SQLConnection> eventListener;
    private final JTable table;
    private final ObjectTableModel<SQLConnection> tableModel;
    private final Set<String> existingNames;


    SQLConnectionEditor(EventListener<SQLConnectionEditor, SQLConnection> eventListener) {
        this.eventListener = eventListener;
        existingNames = new HashSet<>();
        tableModel = new ObjectTableModel<>(COLUMN_NAMES, COLUMN_CLASSES, ATTR_GETTER, ATTR_SETTER) {
            @Override
            public boolean isCellEditable(int rowIdx, int colIdx) {
                return CONNECTED_IDX != colIdx;
            }
        };
        tableModel.addTableModelListener(this::onTableModelEvent);
        table = GUIFactory.newTable(tableModel, this::toggleComponents);
        TableColumnModel columnModel = table.getTableHeader().getColumnModel();
        columnModel.getColumn(NAME_IDX).setPreferredWidth(COLUMN_WIDTHS[NAME_IDX]);
        columnModel.getColumn(HOST_IDX).setPreferredWidth(COLUMN_WIDTHS[HOST_IDX]);
        columnModel.getColumn(PORT_IDX).setPreferredWidth(COLUMN_WIDTHS[PORT_IDX]);
        columnModel.getColumn(USERNAME_IDX).setPreferredWidth(COLUMN_WIDTHS[USERNAME_IDX]);
        columnModel.getColumn(PASSWORD_IDX).setPreferredWidth(COLUMN_WIDTHS[PASSWORD_IDX]);
        columnModel.getColumn(CONNECTED_IDX).setCellRenderer(new PasswordRenderer(GUIFactory.TABLE_CELL_FONT));

        testButton = new JButton("Test");
        testButton.addActionListener(this::onTestButtonEvent);
        connectButton = new JButton("Connect");
        connectButton.addActionListener(this::onConnectButtonEvent);
        addButton = new JButton("Add");
        addButton.addActionListener(this::onAddButtonEvent);
        removeButton = new JButton("Remove");
        removeButton.addActionListener(this::onRemoveButtonEvent);

        backButton = new JButton("Back");
        backButton.addActionListener(this::onBackButtonEvent);
        reloadButton = new JButton("Reload");
        reloadButton.addActionListener(this::onReloadButtonEvent);
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonsPanel.add(testButton);
        buttonsPanel.add(connectButton);
        buttonsPanel.add(removeButton);
        buttonsPanel.add(addButton);
        buttonsPanel.add(reloadButton);
        buttonsPanel.add(backButton);

        setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);
    }

    void setSQLConnections(List<SQLConnection> connections) {
        existingNames.clear();
        for (SQLConnection conn : connections) {
            existingNames.add(conn.getName());
        }
        tableModel.setRows(connections);
    }

    void clear() {
        tableModel.clear();
    }

    public void setSelectedItem(SQLConnection connection) {
        int rowIdx = tableModel.getRowIdx(connection.getKey());
        table.getSelectionModel().addSelectionInterval(rowIdx, rowIdx);
        toggleComponents();
    }

    public SQLConnection getSelectedItem() {
        int rowIdx = table.getSelectedRow();
        if (-1 != rowIdx) {
            return (SQLConnection) tableModel.getValueAt(rowIdx, -1);
        }
        return null;
    }

    private void toggleComponents() {
        if (0 == tableModel.getRowCount()) {
            testButton.setEnabled(false);
            connectButton.setEnabled(false);
            removeButton.setEnabled(false);
        } else {
            SQLConnection conn = getSelectedItem();
            connectButton.setText(null != conn && conn.isConnected() ? "Disconnect" : "Connect");
            connectButton.setEnabled(null != conn);
            testButton.setEnabled(null != conn && false == conn.isConnected());
            removeButton.setEnabled(null != conn && false == conn.isConnected());
            reloadButton.setEnabled(false == tableModel.getRows().stream().anyMatch(SQLConnection::isConnected));
            validate();
            repaint();
        }
    }

    private void onConnectButtonEvent(ActionEvent event) {
        SQLConnection conn = getSelectedItem();
        if (null != conn) {
            toggleComponents();
            eventListener.onSourceEvent(this, EventType.CONNECT, conn);
        }
    }

    private void onTestButtonEvent(ActionEvent event) {
        SQLConnection connection = getSelectedItem();
        if (null != connection) {
            eventListener.onSourceEvent(this, EventType.TEST, connection);
        }
    }

    private void onReloadButtonEvent(ActionEvent event) {
        eventListener.onSourceEvent(this, EventType.RELOAD_CONNECTIONS, null);
    }

    private void onBackButtonEvent(ActionEvent event) {
        eventListener.onSourceEvent(this, EventType.BACK, null);
    }

    private void onRemoveButtonEvent(ActionEvent event) {
        int rowIdx = table.getSelectedRow();
        if (-1 != rowIdx) {
            SQLConnection removed = tableModel.removeRow(rowIdx);
            existingNames.remove(removed.getName());
            toggleComponents();
            eventListener.onSourceEvent(this, EventType.REMOVE_CONNECTION, removed);
        }
    }

    private void onAddButtonEvent(ActionEvent event) {
        String name = JOptionPane.showInputDialog(
                this,
                String.format(Locale.ENGLISH, "%s", NAME),
                "New Connection",
                JOptionPane.INFORMATION_MESSAGE);
        if (null != name && false == name.isEmpty() && false == existingNames.contains(name)) {
            existingNames.add(name);
            SQLConnection added = new SQLConnection(name);
            tableModel.addRow(added);
            toggleComponents();
            eventListener.onSourceEvent(this, EventType.ADD_CONNECTION, added);
        } else if (null != name && existingNames.contains(name)) {
            JOptionPane.showMessageDialog(this,
                    "Name already exists",
                    "Add Fail",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onTableModelEvent(TableModelEvent event) {
        switch (event.getType()) {
            case TableModelEvent.UPDATE:
                int ri = event.getFirstRow();
                int ci = event.getColumn();
                if (ri >= 0 && ri < tableModel.getRowCount() &&
                        ci >= 0 && ci < tableModel.getColumnCount()) {
                    SQLConnection updated = (SQLConnection) tableModel.getValueAt(ri, -1);
                    if (0 == ci) {
                        existingNames.clear();
                        for (int idx = 0; idx < tableModel.getRowCount(); idx++) {
                            existingNames.add((String) tableModel.getValueAt(idx, 0));
                        }
                    }
                    toggleComponents();
                    eventListener.onSourceEvent(this, EventType.UPDATE_CONNECTION_ATTRIBUTES, updated);
                }
                break;
        }
    }

    public static void main(String[] args) {
        ConnectionDescriptorStore<SQLConnection> store = new ConnectionDescriptorStore<>(SQLConnection::new);
        store.load();

        SQLConnectionEditor mngr = new SQLConnectionEditor(((source, eventType, eventData) -> {
            System.out.println(eventType);
        }));
        mngr.setSQLConnections(store.values());
        JFrame frame = GUIFactory.newFrame("Connection Manager", 80, 20, mngr);
        frame.setVisible(true);
    }
}
