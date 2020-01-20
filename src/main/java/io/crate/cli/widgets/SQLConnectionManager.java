package io.crate.cli.widgets;

import io.crate.cli.backend.ConnectionItem;
import io.crate.cli.backend.ConnectivityChecker;
import io.crate.cli.backend.SQLConnection;
import io.crate.cli.common.*;

import io.crate.cli.store.JsonStore;
import io.crate.cli.store.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.Closeable;
import java.io.File;
import java.util.HashSet;
import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;


public class SQLConnectionManager extends JPanel implements EventSpeaker<SQLConnectionManager.EventType>, Closeable {

    public enum EventType {
        CONNECTION_SELECTED,
        CONNECTION_ESTABLISHED,
        CONNECTION_CLOSED,
        CONNECTIONS_LOST
    }

    private static final int ROW_HEIGHT = 22;
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
            ConnectionItem.AttributeName.host.name(),
            ConnectionItem.AttributeName.port.name(),
            ConnectionItem.AttributeName.username.name(),
            ConnectionItem.AttributeName.password.name(),
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
    private static final BiFunction<SQLConnection, String, Object> ATTR_GETTER =
            (conn, attrName) -> {
                switch (attrName) {
                    case NAME:
                        return conn.getName();
                    case CONNECTED:
                        return conn.isConnected() ? "Yes" : "No";
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
                        return conn.setAttribute(attrName, (String) value, "");
                }
            };


    private static final Logger LOGGER = LoggerFactory.getLogger(SQLConnectionManager.class);


    private final EventListener<SQLConnectionManager, Object> eventListener;
    private final Store<SQLConnection> store;
    private final JButton selectButton;
    private final JButton testButton;
    private final JButton connectButton;
    private final JButton addButton;
    private final JButton cloneButton;
    private final JButton removeButton;
    private final JButton reloadButton;
    private final JTable table;
    private final ObjectTableModel<SQLConnection> tableModel;
    private final Set<String> existingNamesInTableModel;
    private final ConnectivityChecker connectivityChecker;


    public SQLConnectionManager(EventListener<SQLConnectionManager, Object> eventListener) {
        this.eventListener = eventListener;
        store = new JsonStore<>(GUIToolkit.SQL_CONNECTION_MANAGER_STORE, SQLConnection.class);
        existingNamesInTableModel = new HashSet<>();
        tableModel = new ObjectTableModel<>(COLUMN_NAMES, ATTR_GETTER, ATTR_SETTER) {
            @Override
            public boolean isCellEditable(int rowIdx, int colIdx) {
                return CONNECTED_IDX != colIdx;
            }
        };
        tableModel.addTableModelListener(this::onTableModelEvent);
        table = new JTable(tableModel);
        table.setAutoCreateRowSorter(false);
        table.setRowHeight(ROW_HEIGHT);
        table.setGridColor(GUIToolkit.CRATE_COLOR.darker());
        table.setFont(GUIToolkit.TABLE_CELL_FONT);
        table.setDefaultRenderer(String.class, new StringCellRenderer());
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(this::toggleComponents);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setFont(GUIToolkit.TABLE_HEADER_FONT);
        header.setForeground(GUIToolkit.TABLE_HEADER_FONT_COLOR);
        TableColumnModel columnModel = table.getTableHeader().getColumnModel();
        columnModel.setColumnSelectionAllowed(false);
        columnModel.getColumn(NAME_IDX).setPreferredWidth(COLUMN_WIDTHS[NAME_IDX]);
        columnModel.getColumn(HOST_IDX).setPreferredWidth(COLUMN_WIDTHS[HOST_IDX]);
        columnModel.getColumn(PORT_IDX).setPreferredWidth(COLUMN_WIDTHS[PORT_IDX]);
        columnModel.getColumn(USERNAME_IDX).setPreferredWidth(COLUMN_WIDTHS[USERNAME_IDX]);
        columnModel.getColumn(PASSWORD_IDX).setPreferredWidth(COLUMN_WIDTHS[PASSWORD_IDX]);
        columnModel.getColumn(PASSWORD_IDX).setCellRenderer(new PasswordRenderer());
        reloadButton = new JButton("Reload");
        reloadButton.addActionListener(this::onReloadButtonEvent);
        cloneButton = new JButton("Clone");
        cloneButton.addActionListener(this::onCloneButtonEvent);
        addButton = new JButton("Add");
        addButton.addActionListener(this::onAddButtonEvent);
        removeButton = new JButton("Remove");
        removeButton.addActionListener(this::onRemoveButtonEvent);
        testButton = new JButton("Test");
        testButton.addActionListener(this::onTestButtonEvent);
        selectButton = new JButton("ASSIGN");
        selectButton.setFont(GUIToolkit.REMARK_FONT);
        selectButton.addActionListener(this::setSelectedItem);
        connectButton = new JButton("Connect");
        connectButton.addActionListener(this::onConnectButtonEvent);
        JPanel manageButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        manageButtonsPanel.setBorder(BorderFactory.createEtchedBorder());
        manageButtonsPanel.add(reloadButton);
        manageButtonsPanel.add(cloneButton);
        manageButtonsPanel.add(addButton);
        manageButtonsPanel.add(removeButton);
        JPanel connectButtonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        connectButtonsPanel.setBorder(BorderFactory.createEtchedBorder());
        connectButtonsPanel.add(selectButton);
        connectButtonsPanel.add(testButton);
        connectButtonsPanel.add(connectButton);
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonsPanel.add(manageButtonsPanel);
        buttonsPanel.add(connectButtonsPanel);
        TitledBorder titleBorder = BorderFactory.createTitledBorder("Connections");
        titleBorder.setTitleFont(GUIToolkit.COMMAND_BOARD_HEADER_FONT);
        titleBorder.setTitleColor(GUIToolkit.TABLE_HEADER_FONT_COLOR);
        setBorder(titleBorder);
        setLayout(new BorderLayout());
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);
        setPreferredSize(GUIToolkit.SQL_CONNECTION_MANAGER_HEIGHT);
        connectivityChecker = new ConnectivityChecker(
                tableModel::getRows,
                this::onLostConnectionsEvent);
    }

    private void toggleComponents(ListSelectionEvent event) {
        toggleComponents();
    }

    private void toggleComponents() {
        if (0 == tableModel.getRowCount()) {
            testButton.setEnabled(false);
            selectButton.setEnabled(false);
            connectButton.setText("Connect");
            cloneButton.setEnabled(false);
            removeButton.setEnabled(false);
        } else {
            SQLConnection conn = getSelectedItem();
            testButton.setEnabled(null != conn && false == conn.isConnected());
            selectButton.setEnabled(null != conn);
            connectButton.setText(null != conn && conn.isConnected() ? "Disconnect" : "Connect");
            cloneButton.setEnabled(null != conn);
            removeButton.setEnabled(null != conn && false == conn.isConnected());
            reloadButton.setEnabled(false == tableModel.getRows().stream().anyMatch(SQLConnection::isConnected));
        }
        table.repaint();
    }

    private void onTableModelEvent(TableModelEvent event) {
        switch (event.getType()) {
            case TableModelEvent.UPDATE:
                int ri = event.getFirstRow();
                int ci = event.getColumn();
                if (ri >= 0 && ri < tableModel.getRowCount() &&
                        ci >= 0 && ci < tableModel.getColumnCount()) {
                    SQLConnection updated = tableModel.getElementAt(ri);
                    if (0 == ci) {
                        System.out.printf("existing: %s\n", existingNamesInTableModel);
                        System.out.printf("updated name: %s\n", existingNamesInTableModel);
                        if (existingNamesInTableModel.contains(updated.getName())) {
                            JOptionPane.showMessageDialog(this,
                                    "Name already exists, they must be unique",
                                    "Update name Fail",
                                    JOptionPane.ERROR_MESSAGE);
                            onReloadButtonEvent(null);
                            table.getSelectionModel().setSelectionInterval(ri, ri);
                            return;
                        }
                        updateExistingNames();
                    }
                    if (updated.isConnected()) {
                        updated.close();
                    }
                    toggleComponents();
                    store.store();
                    onReloadButtonEvent(null);
                }
                break;
        }
    }

    private void updateExistingNames() {
        existingNamesInTableModel.clear();
        for (int idx = 0; idx < tableModel.getRowCount(); idx++) {
            existingNamesInTableModel.add((String) tableModel.getValueAt(idx, 0));
        }
    }

    private void onRemoveButtonEvent(ActionEvent event) {
        int selectedRowIdx = table.getSelectedRow();
        if (-1 != selectedRowIdx) {
            SQLConnection removed = tableModel.removeRow(selectedRowIdx);
            existingNamesInTableModel.remove(removed.getName());
            if (removed.isConnected()) {
                removed.close();
            }
            toggleComponents();
            if (selectedRowIdx == 0) {
                table.getSelectionModel().setSelectionInterval(0, 0);
            } else {
                table.getSelectionModel().setSelectionInterval(selectedRowIdx - 1, selectedRowIdx - 1);
            }
            store.remove(removed);
        }
    }

    private SQLConnection addConnection(SQLConnection template) {
        String name = JOptionPane.showInputDialog(
                this,
                String.format(Locale.ENGLISH, "%s", NAME),
                "New Connection",
                JOptionPane.INFORMATION_MESSAGE);
        if (null == name || name.isEmpty()) {
            return null;
        }
        if (name.contains(" ") || name.contains("\t")) {
            JOptionPane.showMessageDialog(this,
                    "Name cannot contain whites",
                    "Add Fail",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
        if (existingNamesInTableModel.contains(name)) {
            JOptionPane.showMessageDialog(this,
                    "Name already exists",
                    "Add Fail",
                    JOptionPane.ERROR_MESSAGE);
            return null;
        }
        existingNamesInTableModel.add(name);
        SQLConnection added = new SQLConnection(name, template);
        int offset = tableModel.addRow(added);
        table.getSelectionModel().addSelectionInterval(offset, offset);
        store.addAll(false, added);
        toggleComponents();
        return added;
    }

    private void onCloneButtonEvent(ActionEvent event) {
        SQLConnection conn = getSelectedItem();
        if (null != conn) {
            addConnection(conn);
        }
    }

    private void onAddButtonEvent(ActionEvent event) {
        addConnection(null);
    }

    private void onLostConnectionsEvent(Set<SQLConnection> lostConnections) {
        try {
            EventQueue.invokeLater(() -> {
                toggleComponents();
                StringBuilder finalMsg = new StringBuilder();
                for (SQLConnection conn : lostConnections) {
                    String disconnectMessage = String.format(
                            Locale.ENGLISH,
                            "Lost connection with [%s] as '%s'",
                            conn.getUrl(),
                            conn.getUsername());
                    LOGGER.error(disconnectMessage);
                    finalMsg.append(disconnectMessage).append("\n");
                }
                JOptionPane.showMessageDialog(
                        this,
                        finalMsg.toString(),
                        "SQLException",
                        JOptionPane.ERROR_MESSAGE);
                eventListener.onSourceEvent(
                        this,
                        EventType.CONNECTIONS_LOST,
                        lostConnections);
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            LOGGER.error("REMOVE ME ASAP");
        }
    }

    private void onReloadButtonEvent(ActionEvent event) {
        int selectedRowIdx = table.getSelectedRow();
        SQLConnection selected = getSelectedItem();
        store.load();
        List<SQLConnection> conns = store.values();
        tableModel.setRows(conns);
        updateExistingNames();
        if (conns.size() > 0) {
            if (selectedRowIdx >= 0 && selectedRowIdx < conns.size()) {
                SQLConnection conn = tableModel.getElementAt(selectedRowIdx);
                if (null != selected && conn.equals(selected)) {
                    table.getSelectionModel().addSelectionInterval(selectedRowIdx, selectedRowIdx);
                }
            } else {
                setSelectedItem(tableModel.getElementAt(0));
            }
        }
    }

    public void start() {
        if (false == connectivityChecker.isRunning()) {
            onReloadButtonEvent(null);
            connectivityChecker.start();
        }
    }

    public File getStorePath() {
        return store.getPath();
    }

    public SQLConnection getSelectedItem() {
        int rowIdx = table.getSelectedRow();
        if (-1 != rowIdx) {
            return tableModel.getElementAt(rowIdx);
        }
        return null;
    }

    public void setSelectedItem(SQLConnection conn) {
        if (null == conn) {
            return;
        }
        int offset = tableModel.getRowIdx(conn.getKey());
        if (offset >= 0) {
            table.getSelectionModel().setSelectionInterval(offset, offset);
            eventListener.onSourceEvent(this, EventType.CONNECTION_SELECTED, conn);
        }
    }

    private void setSelectedItem(ActionEvent conn) {
        setSelectedItem(getSelectedItem());
    }

    @Override
    public void close() {
        connectivityChecker.close();
        tableModel.clear();
        existingNamesInTableModel.clear();
    }

    private void onTestButtonEvent(ActionEvent event) {
        try {
            SQLConnection conn = getSelectedItem();
            conn.testConnection();
            JOptionPane.showMessageDialog(
                    this,
                    "Connection Successful");
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    this,
                    e.getMessage(),
                    "Connection Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onConnectButtonEvent(ActionEvent event) {
        onConnectButtonEvent(getSelectedItem());
    }

    public void onConnectButtonEvent(SQLConnection conn) {
        if (null == conn) {
            JOptionPane.showMessageDialog(
                    this,
                    "no connection selected",
                    "Connection Failed",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (false == tableModel.contains(conn)) {
            return;
        }
        if (false == conn.isConnected()) {
            try {
                conn.open();
                eventListener.onSourceEvent(this, EventType.CONNECTION_ESTABLISHED, conn);
            } catch (Exception e) {
                LOGGER.error("Connect", e);
                JOptionPane.showMessageDialog(
                        this,
                        e.getMessage(),
                        "Connection Failed",
                        JOptionPane.ERROR_MESSAGE);
            }
        } else {
            try {
                conn.close();
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(
                        this,
                        e.getMessage(),
                        "SQLException",
                        JOptionPane.ERROR_MESSAGE);
                LOGGER.error("Disconnect", e);
            } finally {
                eventListener.onSourceEvent(this, EventType.CONNECTION_CLOSED, conn);
            }
        }
        toggleComponents();
    }
}
