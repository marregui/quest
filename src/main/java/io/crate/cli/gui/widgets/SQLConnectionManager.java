package io.crate.cli.gui.widgets;

import io.crate.cli.connections.ConnectivityChecker;
import io.crate.cli.connections.SQLConnection;
import io.crate.cli.gui.CratedbSQL;
import io.crate.cli.gui.common.EventListener;
import io.crate.cli.gui.common.EventSpeaker;
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
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;


public class SQLConnectionManager extends JPanel implements EventSpeaker<SQLConnectionManager.EventType>, Closeable {

    public enum EventType {
        CONNECTION_SELECTED,
        CONNECTION_ESTABLISHED,
        CONNECTION_CLOSED,
        CONNECTIONS_LOST,
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
    private static final Dimension LIST_DIMENSION = new Dimension(600, 30);


    private final EventListener<SQLConnectionManager, Object> eventListener;
    private final ConnectionDescriptorStore store;
    private final ObjectComboBoxModel<SQLConnection> connectionsListModel;
    private final JButton manageButton;
    private final JButton connectButton;
    private final JPanel connectionSelectionPanel;
    private final SQLConnectionEditor sqlConnectionEditor;
    private final ConnectivityChecker connectivityChecker;
    private Mode mode;


    public SQLConnectionManager(EventListener<SQLConnectionManager, Object> eventListener) {
        this.eventListener = eventListener;
        store = new ConnectionDescriptorStore((Function<String, SQLConnection>) SQLConnection::new);
        connectionsListModel = new ObjectComboBoxModel<>();
        JComboBox<SQLConnection> connectionsList = new JComboBox<>(connectionsListModel);
        connectionsList.addActionListener(this::onSelectSQLConnectionEvent);
        connectionsList.setEditable(false);
        connectionsList.setFont(LIST_FONT);
        connectionsList.setForeground(LIST_COLOR);
        connectionsList.setSize(LIST_DIMENSION);
        connectionsList.setPreferredSize(LIST_DIMENSION);
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

    private void onLostConnectionsEvent(Set<SQLConnection> lostConnections) {
        try {
            EventQueue.invokeAndWait(() -> {
                toggleConnectButtonText();
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
                    eventListener.onSourceEvent(
                            this,
                            EventType.CONNECTIONS_LOST,
                            lostConnections);
                }
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }
    }

    private void onSelectSQLConnectionEvent(ActionEvent event) {
        SQLConnection conn = getSelectedItem();
        sqlConnectionEditor.setSelectedItem(conn);
        toggleConnectButtonText();
        eventListener.onSourceEvent(this, EventType.CONNECTION_SELECTED, conn);
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
            conn = connectionsListModel.getElementAt(0);
        }
        connectionsListModel.setSelectedItem(conn);
        sqlConnectionEditor.setSelectedItem(conn);
    }

    private void toggleConnectButtonText() {
        SQLConnection conn = getSelectedItem();
        connectButton.setText(null != conn && conn.isConnected() ? "Disconnect" : "Connect");
    }

    @Override
    public void close() {
        connectivityChecker.close();
        connectionsListModel.getElements().forEach(SQLConnection::close);
        connectionsListModel.clear();
        sqlConnectionEditor.clear();
    }

    private void onSQLConnectionEditorEvent(SQLConnectionEditor source,
                                            Enum<?> eventType,
                                            SQLConnection conn) {
        switch (source.eventType(eventType)) {
            case TEST:
                onTestButtonEvent(conn);
                break;

            case ADD_CONNECTION:
                store.add(conn);
                connectionsListModel.addElement(conn);
                break;

            case REMOVE_CONNECTION:
                if (conn.isConnected()) {
                    conn.close();
                }
                store.remove(conn);
                connectionsListModel.addElement(conn);
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
        List<SQLConnection> conns = store.values();
        connectionsListModel.setElements(conns);
        sqlConnectionEditor.setSQLConnections(conns);
        setSelectedItem(null);
    }

    private void onBackButtonEvent() {
        changeMode(Mode.SELECTING);
    }

    private void onManageButtonEvent(ActionEvent event) {
        SQLConnection conn = getSelectedItem();
        if (null != conn) {
            sqlConnectionEditor.setSelectedItem(conn);
        }
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

    private void onConnectButtonEvent(ActionEvent event) {
        onConnectButtonEvent(getSelectedItem());
    }

    public void onConnectButtonEvent(SQLConnection conn) {
        if (false == connectionsListModel.contains(conn)) {
            return;
        }
        if (false == conn.isConnected()) {
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
                if (null != eventListener) {
                    eventListener.onSourceEvent(this, EventType.CONNECTION_CLOSED, conn);
                }
            }
        }
        toggleConnectButtonText();
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
