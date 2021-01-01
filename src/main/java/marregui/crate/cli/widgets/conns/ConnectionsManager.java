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

import static marregui.crate.cli.GUITk.createEtchedFlowPanel;
import static marregui.crate.cli.GUITk.createFlowPanel;
import static marregui.crate.cli.widgets.conns.ConnectionsTableModel.NAME_COL;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.Closeable;
import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.TableModelEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import marregui.crate.cli.EventConsumer;
import marregui.crate.cli.EventProducer;
import marregui.crate.cli.GUITk;
import marregui.crate.cli.backend.DBConn;
import marregui.crate.cli.backend.DBConnsValidityChecker;
import marregui.crate.cli.persistence.Store;
import marregui.crate.cli.widgets.command.CommandBoard;


/**
 * Presents a table where each row is a {@link DBConn}, allowing the user
 * to act on each (test, connect, disconnect), as well as to assign them to a
 * {@link CommandBoard}. Connections are loaded/saved from/to a {@link Store}.
 */
public class ConnectionsManager extends JDialog implements EventProducer<ConnectionsManager.EventType>, Closeable {

    public enum EventType {
        /**
         * A connection has been selected.
         */
        CONNECTION_SELECTED,
        /**
         * A connection has been established.
         */
        CONNECTION_ESTABLISHED,
        /**
         * A connection has been closed.
         */
        CONNECTION_CLOSED,
        /**
         * A connection has been lost.
         */
        CONNECTIONS_LOST,
        /**
         * Request to hide the connection's manager.
         */
        HIDE_REQUEST
    }

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionsManager.class);
    private static final String STORE_FILE_NAME = "db-connections.json";

    private final EventConsumer<ConnectionsManager, Object> eventConsumer;
    private final Store<DBConn> store;
    private final JButton assignButton;
    private final JButton testButton;
    private final JButton connectButton;
    private final JButton cloneButton;
    private final JButton removeButton;
    private final JButton reloadButton;
    private final JTable table;
    private final ConnectionsTableModel tableModel;
    private final Set<String> existingNamesInTableModel;
    private final DBConnsValidityChecker connsValidityChecker;

    /**
     * @param owner         reference to the main frame
     * @param eventConsumer receives the events fired as the user interacts
     */
    public ConnectionsManager(Frame owner, EventConsumer<ConnectionsManager, Object> eventConsumer) {
        super(owner, "Connections", false);
        this.eventConsumer = eventConsumer;
        existingNamesInTableModel = new HashSet<>();
        store = new Store<>(STORE_FILE_NAME, DBConn.class) {

            @Override
            public DBConn[] defaultStoreEntries() {
                return new DBConn[] {
                    new DBConn("default")
                };
            }
        };

        table = ConnectionsTableModel.createTable(this::onTableModelEvent, this::toggleComponents);
        tableModel = (ConnectionsTableModel) table.getModel();
        reloadButton = GUITk.createButton("Reload", this::onReloadEvent);
        cloneButton = GUITk.createButton("Clone", this::onCloneEvent);
        JButton addButton = GUITk.createButton("Add", this::onAddEvent);
        removeButton = GUITk.createButton("Remove", this::onRemoveEvent);
        testButton = GUITk.createButton("Test", this::onTestEvent);
        connectButton = GUITk.createButton("Connect", this::onConnectEvent);
        assignButton = GUITk.createButton("ASSIGN", this::setSelected);
        assignButton.setFont(new Font(GUITk.MAIN_FONT_NAME, Font.BOLD, 16));
        JPanel buttons = createFlowPanel(createEtchedFlowPanel(reloadButton, cloneButton, addButton, removeButton),
            createEtchedFlowPanel(assignButton, testButton, connectButton));
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(
            new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
            BorderLayout.CENTER);
        contentPane.add(buttons, BorderLayout.SOUTH);
        Dimension frameDim = GUITk.frameDimension();
        Dimension dimension = new Dimension((int) (frameDim.width * 0.8), (int) (frameDim.height * 0.35));
        Dimension location = GUITk.frameLocation(dimension);
        setPreferredSize(dimension);
        setSize(dimension);
        setLocation(location.width, location.height);
        setVisible(false);
        setAlwaysOnTop(true);
        setModal(false);
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent we) {
                eventConsumer.onSourceEvent(ConnectionsManager.this, EventType.HIDE_REQUEST, null);
            }
        });
        connsValidityChecker = new DBConnsValidityChecker(tableModel::getConnections, this::onLostConnsEvent);
    }

    /**
     * @return the database connection's store's root path
     */
    public File getStorePath() {
        return store.getRootPath().getAbsoluteFile();
    }

    /**
     * @return the selected connection, or null
     */
    public DBConn getSelected() {
        int rowIdx = table.getSelectedRow();
        return rowIdx != -1 ? tableModel.getValueAt(rowIdx) : null;
    }

    /**
     * Sets the selected connection and fires the event CONNECTION_SELECTED.
     * 
     * @param conn connection to be selected
     */
    public void setSelected(DBConn conn) {
        if (conn == null) {
            return;
        }
        int idx = tableModel.getRowIdx(conn.getKey());
        if (idx >= 0) {
            table.getSelectionModel().setSelectionInterval(idx, idx);
            eventConsumer.onSourceEvent(this, EventType.CONNECTION_SELECTED, conn);
        }
    }

    private void setSelected(ActionEvent conn) {
        setSelected(getSelected());
    }

    /**
     * Loads the database connections from the store and starts the connectivity
     * checker.
     */
    public void start() {
        if (!connsValidityChecker.isRunning()) {
            onReloadEvent(null);
            connsValidityChecker.start();
        }
    }

    @Override
    public void close() {
        store.close();
        connsValidityChecker.close();
        tableModel.close();
        existingNamesInTableModel.clear();
    }

    public void onConnectEvent(DBConn conn) {
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Connection not set", "Connection Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!tableModel.contains(conn)) {
            return;
        }
        if (!conn.isOpen()) {
            try {
                conn.open();
                eventConsumer.onSourceEvent(this, EventType.CONNECTION_ESTABLISHED, conn);
            }
            catch (Exception e) {
                LOGGER.error("Connect: {}", e.getMessage());
                JOptionPane.showMessageDialog(this, e.getMessage(), "Connection Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
        else {
            try {
                conn.close();
            }
            catch (RuntimeException e) {
                JOptionPane.showMessageDialog(this, e.getMessage(), "SQLException", JOptionPane.ERROR_MESSAGE);
                LOGGER.error("Disconnect", e);
            }
            finally {
                eventConsumer.onSourceEvent(this, EventType.CONNECTION_CLOSED, conn);
            }
        }
        toggleComponents();
    }

    private void toggleComponents(ListSelectionEvent event) {
        toggleComponents();
    }

    private void toggleComponents() {
        if (0 == tableModel.getRowCount()) {
            testButton.setEnabled(false);
            assignButton.setEnabled(false);
            connectButton.setText("Connect");
            cloneButton.setEnabled(false);
            removeButton.setEnabled(false);
        }
        else {
            DBConn conn = getSelected();
            boolean isSetButNotOpen = conn != null && !conn.isOpen();
            assignButton.setEnabled(conn != null);
            cloneButton.setEnabled(conn != null);
            testButton.setEnabled(isSetButNotOpen);
            removeButton.setEnabled(isSetButNotOpen);
            connectButton.setText(conn != null && conn.isOpen() ? "Disconnect" : "Connect");
            reloadButton.setEnabled(tableModel.getConnections().stream().noneMatch(DBConn::isOpen));
        }
        table.repaint();
        validate();
        repaint();
    }

    private void onTableModelEvent(TableModelEvent event) {
        if (event.getType() == TableModelEvent.UPDATE) {
            int ri = event.getFirstRow();
            int ci = event.getColumn();
            if (ri >= 0 && ri < tableModel.getRowCount() && ci >= 0 && ci < tableModel.getColumnCount()) {
                DBConn updated = tableModel.getValueAt(ri);
                if (0 == ci) {
                    if (existingNamesInTableModel.contains(updated.getName())) {
                        JOptionPane.showMessageDialog(this, "Name already exists, they must be unique", "Update name Fail",
                            JOptionPane.ERROR_MESSAGE);
                        onReloadEvent(null);
                        table.getSelectionModel().setSelectionInterval(ri, ri);
                        return;
                    }
                    updateExistingNames();
                }
                if (updated.isOpen()) {
                    updated.close();
                }
                toggleComponents();
                store.asyncSaveToFile();
            }
        }
    }

    private void updateExistingNames() {
        existingNamesInTableModel.clear();
        for (int idx = 0; idx < tableModel.getRowCount(); idx++) {
            existingNamesInTableModel.add((String) tableModel.getValueAt(idx, 0));
        }
    }

    private void onRemoveEvent(ActionEvent event) {
        int selectedRowIdx = table.getSelectedRow();
        if (-1 != selectedRowIdx) {
            DBConn removed = tableModel.removeConnection(selectedRowIdx);
            existingNamesInTableModel.remove(removed.getName());
            if (removed.isOpen()) {
                removed.close();
            }
            toggleComponents();
            if (selectedRowIdx == 0) {
                table.getSelectionModel().setSelectionInterval(0, 0);
            }
            else {
                table.getSelectionModel().setSelectionInterval(selectedRowIdx - 1, selectedRowIdx - 1);
            }
            store.removeEntry(removed);
        }
    }

    private void addConnection(DBConn template) {
        String name = JOptionPane.showInputDialog(this, NAME_COL, "New Connection", JOptionPane.INFORMATION_MESSAGE);
        if (name == null || name.isEmpty()) {
            return;
        }
        if (name.contains(" ") || name.contains("\t")) {
            JOptionPane.showMessageDialog(this, "Name cannot contain whites", "Add Fail", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (existingNamesInTableModel.contains(name)) {
            JOptionPane.showMessageDialog(this, "Name already exists", "Add Fail", JOptionPane.ERROR_MESSAGE);
            return;
        }
        existingNamesInTableModel.add(name);
        DBConn added = new DBConn(name, template);
        int offset = tableModel.addConnection(added);
        table.getSelectionModel().addSelectionInterval(offset, offset);
        store.addEntry(added, false);
        toggleComponents();
    }

    private void onCloneEvent(ActionEvent event) {
        DBConn conn = getSelected();
        if (conn != null) {
            addConnection(conn);
        }
    }

    private void onAddEvent(ActionEvent event) {
        addConnection(null);
    }

    private void onLostConnsEvent(Set<DBConn> lostConns) {
        StringBuilder sb = new StringBuilder();
        for (DBConn conn : lostConns) {
            String msg = String.format("Lost connection with [%s] as '%s'", conn.getUri(), conn.getUsername());
            sb.append(msg).append("\n");
            LOGGER.error(msg);
        }
        GUITk.invokeLater(() -> {
            toggleComponents();
            eventConsumer.onSourceEvent(this, EventType.CONNECTIONS_LOST, lostConns);
            JOptionPane.showMessageDialog(this, sb.toString(), "SQLException", JOptionPane.ERROR_MESSAGE);
        });
    }

    private void onReloadEvent(ActionEvent event) {
        int selectedIdx = table.getSelectedRow();
        DBConn selected = getSelected();
        store.loadEntriesFromFile();
        List<DBConn> conns = store.entries();
        tableModel.setConnections(conns);
        updateExistingNames();
        if (conns.size() > 0) {
            if (selectedIdx >= 0 && selectedIdx < conns.size()) {
                DBConn conn = tableModel.getValueAt(selectedIdx);
                if (conn.equals(selected)) {
                    table.getSelectionModel().addSelectionInterval(selectedIdx, selectedIdx);
                }
            }
            else {
                setSelected(tableModel.getValueAt(0));
            }
        }
    }

    private void onTestEvent(ActionEvent event) {
        DBConn conn = getSelected();
        try {
            if (conn != null) {
                conn.testConnectivity();
                JOptionPane.showMessageDialog(this, "Connection Successful");
            }
            else {
                JOptionPane.showMessageDialog(this, "Connection not set", "Connection Failed", JOptionPane.ERROR_MESSAGE);
            }
        }
        catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Connection Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onConnectEvent(ActionEvent event) {
        onConnectEvent(getSelected());
    }
}
