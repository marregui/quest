package io.crate.cli.gui;

import java.awt.*;
import java.util.*;
import java.util.List;

import io.crate.cli.connections.SQLRowType;
import io.crate.cli.connections.SQLConnection;
import io.crate.cli.connections.SQLExecutionResponse;
import io.crate.cli.connections.SQLExecutor;
import io.crate.cli.gui.widgets.CommandManager;
import io.crate.cli.gui.common.*;
import io.crate.cli.gui.widgets.SQLConnectionManager;
import io.crate.cli.connections.SQLExecutionRequest;
import io.crate.cli.gui.widgets.SQLResultsTable;

import javax.swing.*;


public class CratedbSQL {

    public static final String VERSION = "1.0.0";


    private final SQLExecutor sqlExecutor;
    private final SQLConnectionManager sqlConnectionManager;
    private final CommandManager commandManager;
    private final SQLResultsTable sqlResultsTable;
    private final JFrame frame;


    private CratedbSQL() {
        sqlExecutor = new SQLExecutor(this::onSourceEvent);
        sqlExecutor.start();
        commandManager = new CommandManager(this::onSourceEvent);
        sqlConnectionManager = new SQLConnectionManager(this::onSourceEvent);
        sqlConnectionManager.start();
        sqlResultsTable = new SQLResultsTable();
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(sqlConnectionManager, BorderLayout.NORTH);
        topPanel.add(commandManager, BorderLayout.CENTER);
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                false,
                topPanel,
                sqlResultsTable
        );
        JPanel mainManel = new JPanel(new BorderLayout());
        mainManel.add(splitPane, BorderLayout.CENTER);
        frame = GUIFactory.newFrame(String.format(
                Locale.ENGLISH,
                "CratedbSQL %s [store: %s]",
                VERSION,
                sqlConnectionManager.getStorePath().getAbsolutePath()),
                90, 90,
                mainManel);
    }

    private void onSourceEvent(EventSpeaker source, Enum eventType, Object eventData) {
        if (source instanceof SQLConnectionManager) {
            onSQLConnectionManagerEvent(sqlConnectionManager.eventType(eventType), (SQLConnection) eventData);
        } else if (source instanceof CommandManager) {
            onCommandManagerEvent(commandManager.eventType(eventType), (SQLExecutionRequest) eventData);
        } else if (source instanceof SQLExecutor) {
            onSQLExecutorEvent(sqlExecutor.eventType(eventType), (SQLExecutionResponse) eventData);
        }
    }

    private void onSQLConnectionManagerEvent(SQLConnectionManager.EventType event, SQLConnection conn) {
        switch (event) {
            case CONNECTION_SELECTED:
                commandManager.setSQLConnection(conn);
                break;

            case CONNECTION_ESTABLISHED:
            case CONNECTION_CLOSED:
                SQLConnection current = commandManager.getSQLConnection();
                if (null != current && current.equals(conn)) {
                    commandManager.setSQLConnection(conn);
                }
                break;

            case CONNECTIONS_LOST:
                // TODO
                break;

            case REPAINT_REQUIRED:
                frame.validate();
                frame.repaint();
                break;
        }
    }

    private void onCommandManagerEvent(CommandManager.EventType event, SQLExecutionRequest request) {
        switch (event) {
            case COMMAND_AVAILABLE:
                sqlExecutor.submit(request);
                break;

            case BUFFER_CHANGE:
                SQLConnection conn = request.getSQLConnection();
                if (null != conn) {
                    sqlConnectionManager.setSelectedItem(conn);
                }
                break;
        }
    }

    private void onSQLExecutorEvent(SQLExecutor.EventType event, SQLExecutionResponse response) {
        switch (event) {
            case QUERY_FAILURE:
                sqlResultsTable.displayError(response.getError());
                break;

            case RESULTS_AVAILABLE:
            case RESULTS_COMPLETED:
                long seqNo = response.getSeqNo();
                boolean needsClearing = 0 == seqNo && sqlResultsTable.getRowCount() > 0;
                boolean expectMore = SQLExecutor.EventType.RESULTS_AVAILABLE == event;
                try {
                    SwingUtilities.invokeAndWait(() -> {
                        sqlResultsTable.addRows(response.getResults(), expectMore, needsClearing);
                    });
                } catch (Throwable throwable) {
                    throw new RuntimeException(throwable);
                }
                break;
        }
    }

    private void shutdown() {
        sqlExecutor.close();
        sqlConnectionManager.close();
        sqlResultsTable.close();
    }

    private void setVisible(boolean isVisible) {
        frame.setVisible(isVisible);
    }

    public static void main(String [] args) {
        CratedbSQL cratedbSql = new CratedbSQL();
        cratedbSql.setVisible(true);
        Runtime.getRuntime().addShutdownHook(new Thread(
                cratedbSql::shutdown,
                String.format(
                        Locale.ENGLISH,
                        "%s shutdown tasks",
                        CratedbSQL.class.getSimpleName())));
    }
}
