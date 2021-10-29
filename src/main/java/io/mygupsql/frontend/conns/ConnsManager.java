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
 * Copyright (c) 2019 - 2022, Miguel Arregui a.k.a. marregui
 */

package io.mygupsql.frontend.conns;

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

import io.mygupsql.EventConsumer;
import io.mygupsql.EventProducer;
import io.mygupsql.GTk;
import io.mygupsql.backend.Conn;
import io.mygupsql.backend.ConnsChecker;
import io.mygupsql.backend.Store;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.mygupsql.frontend.commands.CommandBoard;


/**
 * Dialog that presents a table where each row is a {@link Conn}, allowing the
 * user to test, connect, disconnect, edit, as well as to assign them to the
 * {@link CommandBoard}. Connections are loaded/saved from/to a {@link Store}.
 */
public class ConnsManager extends JDialog implements EventProducer<ConnsManager.EventType>, Closeable {

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
         * A set of connections, possibly only one, has been lost.
         */
        CONNECTIONS_LOST,
        /**
         * Request to hide the connection's manager.
         */
        HIDE_REQUEST
    }

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnsManager.class);
    private static final String STORE_FILE_NAME = "connections.json";

    private final EventConsumer<ConnsManager, Object> eventConsumer;
    private final Store<Conn> store;
    private final JButton assignButton;
    private final JButton testButton;
    private final JButton connectButton;
    private final JButton cloneButton;
    private final JButton removeButton;
    private final JButton reloadButton;
    private final JTable table;
    private final ConnsTableModel tableModel;
    private final ConnsChecker connsValidityChecker;

    /**
     * @param owner         reference to the main frame
     * @param eventConsumer receives the events fired as the user interacts
     */
    public ConnsManager(Frame owner, EventConsumer<ConnsManager, Object> eventConsumer) {
        super(owner, "Connections", false); // does not block use of the main app
        this.eventConsumer = eventConsumer;
        store = new Store<>(STORE_FILE_NAME, Conn.class) {

            @Override
            public Conn[] defaultStoreEntries() {
                return new Conn[]{
                        new Conn("QuestDB"),
                        new Conn("Postgres", "localhost", "5432", "postgres", "password"),
                        new Conn("CrateDB", "localhost", "5432", "crate", ""),
                        new Conn("TimescaleDB", "localhost", "5434", "postgres", "password")
                };
            }
        };
        table = ConnsTableModel.createTable(this::onTableModelEvent, this::onListSelectionEvent);
        tableModel = (ConnsTableModel) table.getModel();
        connsValidityChecker = new ConnsChecker(tableModel::getConns, this::onLostConnsEvent);
        reloadButton = GTk.createButton("Reload", GTk.Icon.RELOAD, "Reload last saved connections", this::onReloadEvent);
        cloneButton = GTk.createButton("Clone", GTk.Icon.CONN_CLONE, "Clone selected connection", this::onCloneEvent);
        JButton addButton = GTk.createButton("Add", GTk.Icon.CONN_ADD, "Add connection", this::onAddEvent);
        removeButton = GTk.createButton("Remove", GTk.Icon.CONN_REMOVE, "Remove selected connection", this::onRemoveEvent);
        testButton = GTk.createButton("Test", GTk.Icon.CONN_TEST, "Test selected connection", this::onTestEvent);
        connectButton = GTk.createButton("Connect", GTk.Icon.CONN_CONNECT, "Connect selected connection", this::onConnectEvent);
        assignButton = GTk.createButton("ASSIGN", GTk.Icon.CONN_ASSIGN, "Assigns the selected connection to the command panel", this::onAssignEvent);
        assignButton.setFont(new Font(GTk.MAIN_FONT_NAME, Font.BOLD, 16));
        JPanel buttons = GTk.createFlowPanel(GTk.createEtchedFlowPanel(reloadButton, cloneButton, addButton, removeButton),
                GTk.createEtchedFlowPanel(assignButton, testButton, connectButton));
        Container contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());
        contentPane.add(
                new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED),
                BorderLayout.CENTER);
        contentPane.add(buttons, BorderLayout.SOUTH);
        Dimension frameDim = GTk.frameDimension();
        Dimension dimension = new Dimension((int) (frameDim.width * 0.8), (int) (frameDim.height * 0.35));
        Dimension location = GTk.frameLocation(dimension);
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
                eventConsumer.onSourceEvent(ConnsManager.this, EventType.HIDE_REQUEST, null);
            }
        });
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
    public Conn getSelectedConn() {
        int rowIdx = table.getSelectedRow();
        return rowIdx != -1 ? tableModel.getValueAt(rowIdx) : null;
    }

    /**
     * Sets the selected connection and fires the event CONNECTION_SELECTED.
     *
     * @param conn connection to be selected
     */
    public void setSelectedConn(Conn conn) {
        if (conn == null) {
            return;
        }
        int rowIdx = tableModel.getRowIdx(conn.getKey());
        if (rowIdx >= 0) {
            table.getSelectionModel().setSelectionInterval(rowIdx, rowIdx);
            eventConsumer.onSourceEvent(this, EventType.CONNECTION_SELECTED, conn);
        }
    }

    /**
     * Loads the connections from the store and starts the connectivity checker.
     */
    public void start() {
        if (!connsValidityChecker.isRunning()) {
            onReloadEvent(null);
            connsValidityChecker.start();
        }
    }

    @Override
    public void close() {
        connsValidityChecker.close();
        tableModel.close();
        store.close();
    }

    public void onConnectEvent(Conn conn) {
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Connection not set", "Connection Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!tableModel.containsConn(conn)) {
            return;
        }
        if (!conn.isOpen()) {
            try {
                conn.open();
                eventConsumer.onSourceEvent(this, EventType.CONNECTION_ESTABLISHED, conn);
            } catch (Exception e) {
                LOGGER.error("Connect: {}", e.getMessage());
                JOptionPane.showMessageDialog(this, e.getMessage(), "Connection Failed", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            try {
                conn.close();
            } catch (RuntimeException e) {
                LOGGER.error("Disconnect", e);
            } finally {
                eventConsumer.onSourceEvent(this, EventType.CONNECTION_CLOSED, conn);
            }
        }
        toggleComponents();
    }

    private void onListSelectionEvent(ListSelectionEvent event) {
        toggleComponents();
    }

    private void toggleComponents() {
        if (0 == tableModel.getRowCount()) {
            testButton.setEnabled(false);
            assignButton.setEnabled(false);
            connectButton.setText("Connect");
            connectButton.setIcon(GTk.Icon.CONN_CONNECT.icon());
            cloneButton.setEnabled(false);
            removeButton.setEnabled(false);
        } else {
            Conn conn = getSelectedConn();
            boolean isSetButNotOpen = conn != null && !conn.isOpen();
            assignButton.setEnabled(conn != null);
            cloneButton.setEnabled(conn != null);
            testButton.setEnabled(isSetButNotOpen);
            removeButton.setEnabled(isSetButNotOpen);
            connectButton.setText(conn != null && conn.isOpen() ? "Disconnect" : "Connect");
            connectButton.setIcon((conn != null && conn.isOpen() ? GTk.Icon.CONN_DISCONNECT : GTk.Icon.CONN_CONNECT).icon());
            reloadButton.setEnabled(tableModel.getConns().stream().noneMatch(Conn::isOpen));
        }
        table.repaint();
        validate();
        repaint();
    }

    private void onTableModelEvent(TableModelEvent event) {
        if (event.getType() == TableModelEvent.UPDATE) {
            toggleComponents();
            store.asyncSaveToFile();
        }
    }

    private void onReloadEvent(ActionEvent event) {
        int selectedIdx = table.getSelectedRow();
        Conn selected = getSelectedConn();
        store.loadEntriesFromFile();
        List<Conn> conns = store.entries();
        tableModel.setConns(conns);
        if (conns.size() > 0) {
            if (selectedIdx >= 0 && selectedIdx < conns.size()) {
                Conn conn = tableModel.getValueAt(selectedIdx);
                if (conn.equals(selected)) {
                    table.getSelectionModel().addSelectionInterval(selectedIdx, selectedIdx);
                }
            } else {
                selectedIdx = 0;
                for (int i=0; i < conns.size(); i++) {
                    if (conns.get(i).isDefault()) {
                        if (selectedIdx == 0) {
                            selectedIdx = i;
                        } else {
                            throw new IllegalStateException("too many default connections");
                        }
                    }
                }
                setSelectedConn(tableModel.getValueAt(selectedIdx));
            }
        }
    }

    private void onCloneEvent(ActionEvent event) {
        Conn conn = getSelectedConn();
        if (conn != null) {
            onAddConnEvent(conn);
        }
    }

    private void onAddEvent(ActionEvent event) {
        onAddConnEvent(null);
    }

    private void onAddConnEvent(Conn template) {
        String name = JOptionPane.showInputDialog(this, "Name", "New Connection", JOptionPane.INFORMATION_MESSAGE);
        if (name == null || name.isEmpty()) {
            return;
        }
        if (name.contains(" ") || name.contains("\t")) {
            JOptionPane.showMessageDialog(this, "Name cannot contain whites", "Add Fail", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (tableModel.containsName(name)) {
            JOptionPane.showMessageDialog(this, "Name already exists", "Add Fail", JOptionPane.ERROR_MESSAGE);
            return;
        }
        Conn added = new Conn(name, template);
        int offset = tableModel.addConn(added);
        table.getSelectionModel().addSelectionInterval(offset, offset);
        store.addEntry(added, false);
        toggleComponents();
    }

    private void onRemoveEvent(ActionEvent event) {
        int rowIdx = table.getSelectedRow();
        if (-1 != rowIdx) {
            Conn removed = tableModel.removeConn(rowIdx);
            if (removed.isOpen()) {
                removed.close();
            }
            toggleComponents();
            if (rowIdx == 0) {
                table.getSelectionModel().setSelectionInterval(0, 0);
            } else {
                table.getSelectionModel().setSelectionInterval(rowIdx - 1, rowIdx - 1);
            }
            store.removeEntry(removed);
        }
    }

    private void onAssignEvent(ActionEvent event) {
        setSelectedConn(getSelectedConn());
    }

    private void onTestEvent(ActionEvent event) {
        Conn conn = getSelectedConn();
        try {
            if (conn != null) {
                conn.testConnectivity();
                JOptionPane.showMessageDialog(this, "Connection Successful");
            } else {
                JOptionPane.showMessageDialog(this, "Connection not set", "Connection Failed", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Connection Failed", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onConnectEvent(ActionEvent event) {
        onConnectEvent(getSelectedConn());
    }

    private void onLostConnsEvent(Set<Conn> lostConns) {
        StringBuilder sb = new StringBuilder();
        for (Conn conn : lostConns) {
            String msg = String.format("Lost connection with [%s] as '%s'", conn.getUri(), conn.getUsername());
            sb.append(msg).append("\n");
            LOGGER.error(msg);
        }
        GTk.invokeLater(() -> {
            toggleComponents();
            eventConsumer.onSourceEvent(this, EventType.CONNECTIONS_LOST, lostConns);
            JOptionPane.showMessageDialog(this, sb.toString(), "SQLException", JOptionPane.ERROR_MESSAGE);
        });
    }
}
