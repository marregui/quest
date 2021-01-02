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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import marregui.crate.cli.EventConsumer;
import marregui.crate.cli.EventProducer;
import marregui.crate.cli.GUITk;
import marregui.crate.cli.backend.Conn;
import marregui.crate.cli.backend.ConnsChecker;
import marregui.crate.cli.backend.Store;
import marregui.crate.cli.widgets.command.CommandBoard;


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
                return new Conn[] {
                    new Conn("default")
                };
            }
        };
        table = ConnsTableModel.createTable(this::onTableModelEvent, this::toggleComponents);
        tableModel = (ConnsTableModel) table.getModel();
        connsValidityChecker = new ConnsChecker(tableModel::getConns, this::onLostConnsEvent);
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
    public Conn getSelected() {
        int rowIdx = table.getSelectedRow();
        return rowIdx != -1 ? tableModel.getValueAt(rowIdx) : null;
    }

    /**
     * Sets the selected connection and fires the event CONNECTION_SELECTED.
     * 
     * @param conn connection to be selected
     */
    public void setSelected(Conn conn) {
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
            System.out.println("HORROR");
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
            Conn conn = getSelected();
            boolean isSetButNotOpen = conn != null && !conn.isOpen();
            assignButton.setEnabled(conn != null);
            cloneButton.setEnabled(conn != null);
            testButton.setEnabled(isSetButNotOpen);
            removeButton.setEnabled(isSetButNotOpen);
            connectButton.setText(conn != null && conn.isOpen() ? "Disconnect" : "Connect");
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

    private void onRemoveEvent(ActionEvent event) {
        int selectedRowIdx = table.getSelectedRow();
        if (-1 != selectedRowIdx) {
            Conn removed = tableModel.removeConn(selectedRowIdx);
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

    private void onCloneEvent(ActionEvent event) {
        Conn conn = getSelected();
        if (conn != null) {
            onAddConnEvent(conn);
        }
    }

    private void onAddEvent(ActionEvent event) {
        onAddConnEvent(null);
    }

    private void onLostConnsEvent(Set<Conn> lostConns) {
        StringBuilder sb = new StringBuilder();
        for (Conn conn : lostConns) {
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
        Conn selected = getSelected();
        store.loadEntriesFromFile();
        List<Conn> conns = store.entries();
        tableModel.setConns(conns);
        if (conns.size() > 0) {
            if (selectedIdx >= 0 && selectedIdx < conns.size()) {
                Conn conn = tableModel.getValueAt(selectedIdx);
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
        Conn conn = getSelected();
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
