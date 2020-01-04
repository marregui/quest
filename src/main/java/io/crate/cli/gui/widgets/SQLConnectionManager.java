package io.crate.cli.gui.widgets;

import io.crate.cli.connections.ConnectivityChecker;
import io.crate.cli.connections.SQLConnection;
import io.crate.cli.gui.CratedbSQL;
import io.crate.cli.gui.common.EventListener;
import io.crate.cli.gui.common.GUIFactory;
import io.crate.cli.connections.ConnectionDescriptorStore;

import io.crate.cli.gui.common.ObjectComboBoxModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.Closeable;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;


public class SQLConnectionManager extends JPanel implements Closeable {

    public enum EventType {
        CONNECTION_SELECTED,
        CONNECTION_ESTABLISHED,
        CONNECTION_LOST,
        CONNECTION_CLOSED,
        REPAINT_REQUIRED
    }

    private enum Mode {
        SELECTING, MANAGING
    }

    private static final Dimension SELECTING_HEIGHT = new Dimension(0, 40);
    private static final Dimension MANAGING_HEIGHT = new Dimension(0, 300);
    private static final Logger LOGGER = LoggerFactory.getLogger(SQLConnectionManager.class);
    private static final Font LIST_FONT = new Font("monospaced", Font.BOLD, 12);
    private static final Color LIST_COLOR = Color.DARK_GRAY;
    private static final int LIST_WIDTH = 600;


    private final EventListener<SQLConnectionManager, SQLConnection> eventListener;
    private final ConnectionDescriptorStore store;
    private final ObjectComboBoxModel<SQLConnection> connectionsListModel;
    private final JComboBox<SQLConnection> connectionsList;
    private final JButton manageButton;
    private final JButton connectButton;
    private final JPanel connectionSelectionPanel;
    private final SQLConnectionEditor sqlConnectionEditor;
    private final ConnectivityChecker connectivityChecker;
    private Mode mode;


    public SQLConnectionManager(EventListener<SQLConnectionManager, SQLConnection> eventListener) {
        this.eventListener = eventListener;
        store = new ConnectionDescriptorStore((Function<String, SQLConnection>) SQLConnection::new);
        connectionsListModel = new ObjectComboBoxModel<>();
        connectionsList = new JComboBox<>(connectionsListModel);
        connectionsList.addActionListener(this::toggleComponents);
        connectionsList.setEditable(false);
        connectionsList.setFont(LIST_FONT);
        connectionsList.setForeground(LIST_COLOR);
        Dimension connectionsListSize = new Dimension(LIST_WIDTH, 30);
        connectionsList.setSize(connectionsListSize);
        connectionsList.setPreferredSize(connectionsListSize);
        manageButton = new JButton("Manage");
        manageButton.addActionListener(this::onManageButtonEvent);
        connectButton = new JButton("Connect");
        connectButton.addActionListener(this::onConnectButtonEvent);
        connectionSelectionPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        connectionSelectionPanel.add(connectionsList);
        connectionSelectionPanel.add(manageButton);
        connectionSelectionPanel.add(connectButton);
        connectionSelectionPanel.setPreferredSize(SELECTING_HEIGHT);
        sqlConnectionEditor = new SQLConnectionEditor(this::onSQLConnectionEditorEvent);
        sqlConnectionEditor.setPreferredSize(MANAGING_HEIGHT);
        mode = Mode.SELECTING;
        connectivityChecker = new ConnectivityChecker(
                connectionsListModel::getElements,
                this::onLostConnectionsEvent);
        setBorder(BorderFactory.createTitledBorder("Connections"));
        setLayout(new BorderLayout());
        add(connectionSelectionPanel, BorderLayout.CENTER);
    }

    private void onLostConnectionsEvent(Set<SQLConnection> lostConnections) {
        if (lostConnections.isEmpty()) {
            return;
        }
        try {
            SwingUtilities.invokeAndWait(() -> {
                toggleComponents(null);
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
                if (null != eventListener) {
                    eventListener.onSourceEvent(this, EventType.CONNECTION_LOST, null);
                }
            });
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void start() {
        if (false == connectivityChecker.isRunning()) {
            connectivityChecker.start();
            onReloadButtonEvent(null);
        }
    }

    public File getStorePath() {
        return store.getPath();
    }

    public SQLConnection getSelectedItem() {
        return (SQLConnection) connectionsListModel.getSelectedItem();
    }

    public void setSelectedItem(SQLConnection connection) {
        SQLConnection conn = connection;
        if (null == conn) {
            if (0 == connectionsListModel.getSize()) {
                return;
            }
            conn = connectionsList.getItemAt(0);
        }
        connectionsListModel.setSelectedItem(conn);
        sqlConnectionEditor.setSelectedItem(conn);
        validate();
        repaint();
    }

    private void toggleComponents(ActionEvent event) {
        SQLConnection conn = getSelectedItem();
        connectButton.setText(null != conn && conn.isConnected() ? "Disconnect" : "Connect");
        sqlConnectionEditor.toggleComponents();
        if (null != event && null != conn) {
            eventListener.onSourceEvent(this, EventType.CONNECTION_SELECTED, conn);
        }
    }

    @Override
    public void close() {
        connectivityChecker.close();
        connectionsListModel.getElements().forEach(SQLConnection::close);
        connectionsListModel.clear();
        connectionsList.removeAllItems();
        sqlConnectionEditor.clear();
        connectionsList.removeAllItems();
    }

    private void onSQLConnectionEditorEvent(SQLConnectionEditor source,
                                            Enum<?> eventType,
                                            SQLConnection conn) {
        switch (SQLConnectionEditor.EventType.valueOf(eventType.name())) {
            case TEST:
                onTestButtonEvent(conn);
                break;

            case CONNECT:
                onConnectButtonEvent(conn);
                break;

            case ADD_CONNECTION:
                store.add(conn);
                connectionsList.addItem(conn);
                break;

            case REMOVE_CONNECTION:
                if (conn.isConnected()) {
                    conn.close();
                }
                store.remove(conn);
                connectionsList.removeItem(conn);
                break;

            case UPDATE_CONNECTION_ATTRIBUTES:
                if (conn.isConnected()) {
                    conn.close();
                }
                store.store();
                onReloadButtonEvent(null);
                break;

            case RELOAD_CONNECTIONS:
                onReloadButtonEvent(null);
                break;

            case BACK:
                onBackButtonEvent();
                break;
        }
    }

    private void onReloadButtonEvent(ActionEvent event) {
        store.load();
        connectionsList.removeAllItems();
        List<SQLConnection> conns = store.values();
        connectionsListModel.setElements(conns);
        sqlConnectionEditor.setSQLConnections(conns);
        setSelectedItem(null);
    }

    private void changeMode(Mode newMode) {
        mode = newMode;
        Component toRemove = null;
        Component toAdd = null;
        switch (newMode) {
            case SELECTING:
                toRemove = sqlConnectionEditor;
                toAdd = connectionSelectionPanel;
                break;

            case MANAGING:
                toRemove = connectionSelectionPanel;
                toAdd = sqlConnectionEditor;
                break;
        }
        remove(toRemove);
        add(toAdd, BorderLayout.CENTER);
        validate();
        repaint();
        eventListener.onSourceEvent(this, EventType.REPAINT_REQUIRED, null);
    }

    private void onBackButtonEvent() {
        changeMode(Mode.SELECTING);
    }

    private void onManageButtonEvent(ActionEvent event) {
        changeMode(Mode.MANAGING);
    }

    private void onTestButtonEvent(SQLConnection conn) {
        try {
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

    private void onConnectButtonEvent(SQLConnection conn) {
        if (false == conn.isConnected()) {
            connect(conn);
        } else {
            disconnect(conn);
        }
        toggleComponents(null);
    }

    private void onConnectButtonEvent(ActionEvent event) {
        onConnectButtonEvent(getSelectedItem());
    }

    private void connect(SQLConnection conn) {
        try {
            conn.open();
            if (null != eventListener) {
                eventListener.onSourceEvent(this, EventType.CONNECTION_ESTABLISHED, conn);
            }
        } catch (Exception e) {
            LOGGER.error("Connect", e);
            JOptionPane.showMessageDialog(
                    this,
                    e.getMessage(),
                    "Connection Failed",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void disconnect(SQLConnection conn) {
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
            if (null != eventListener) {
                eventListener.onSourceEvent(this, EventType.CONNECTION_CLOSED, conn);
            }
        }
    }

    public static void main(String[] args) {
        SQLConnectionManager mngr = new SQLConnectionManager(((source, eventType, eventData) -> {
            System.out.printf(
                    Locale.ENGLISH,
                    "from: %s, [%s]: %s\n",
                    source.getClass().getSimpleName(),
                    eventType,
                    eventData);
        }));
        GUIFactory.newFrame(
                "Connection Manager",
                80, 20,
                mngr)
                .setVisible(true);
        Runtime.getRuntime().addShutdownHook(new Thread(
                mngr::close,
                String.format(
                        Locale.ENGLISH,
                        "%s shutdown tasks",
                        CratedbSQL.class.getSimpleName())));
    }
}
