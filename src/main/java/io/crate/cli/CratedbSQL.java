package io.crate.cli;

import io.crate.cli.common.EventSpeaker;
import io.crate.cli.common.GUIFactory;
import io.crate.cli.connections.SQLConnection;
import io.crate.cli.connections.SQLExecutionRequest;
import io.crate.cli.connections.SQLExecutionResponse;
import io.crate.cli.connections.SQLExecutor;
import io.crate.cli.widgets.CommandBoardManager;
import io.crate.cli.widgets.SQLConnectionManager;
import io.crate.cli.widgets.SQLResultsManager;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;
import java.util.Set;


public class CratedbSQL {

    public static final String VERSION = "1.0.0";


    private final SQLExecutor sqlExecutor;
    private final SQLConnectionManager sqlConnectionManager;
    private final CommandBoardManager commandBoardManager;
    private final SQLResultsManager sqlResultsManager;
    private final JFrame frame;


    private CratedbSQL() {
        sqlExecutor = new SQLExecutor(this::onSourceEvent);
        commandBoardManager = new CommandBoardManager(this::onSourceEvent);
        sqlConnectionManager = new SQLConnectionManager(this::onSourceEvent);
        sqlResultsManager = new SQLResultsManager();
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(sqlConnectionManager, BorderLayout.NORTH);
        topPanel.add(commandBoardManager, BorderLayout.CENTER);
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(
                new JSplitPane(
                        JSplitPane.VERTICAL_SPLIT,
                        false,
                        topPanel,
                        sqlResultsManager),
                BorderLayout.CENTER);
        frame = GUIFactory.newFrame(
                String.format(
                        Locale.ENGLISH,
                        "CratedbSQL %s [store: %s]",
                        VERSION,
                        sqlConnectionManager.getStorePath().getAbsolutePath()),
                GUIFactory.FRAME_WIDTH_AS_PERCENT_OF_SCREEN_WIDTH,
                GUIFactory.FRAME_HEIGHT_AS_PERCENT_OF_SCREEN_WIDTH,
                mainPanel);
        sqlConnectionManager.start();
        sqlExecutor.start();
    }

    private void onSourceEvent(EventSpeaker source, Enum eventType, Object eventData) {
        if (source instanceof SQLConnectionManager) {
            onSQLConnectionManagerEvent(sqlConnectionManager.eventType(eventType), eventData);
        } else if (source instanceof CommandBoardManager) {
            onCommandManagerEvent(commandBoardManager.eventType(eventType), (SQLExecutionRequest) eventData);
        } else if (source instanceof SQLExecutor) {
            onSQLExecutorEvent(sqlExecutor.eventType(eventType), (SQLExecutionResponse) eventData);
        }
    }

    private void onSQLConnectionManagerEvent(SQLConnectionManager.EventType event, Object eventData) {
        switch (event) {
            case CONNECTION_SELECTED:
                commandBoardManager.setSQLConnection((SQLConnection) eventData);
                break;

            case CONNECTION_ESTABLISHED:
            case CONNECTION_CLOSED:
                SQLConnection conn = (SQLConnection) eventData;
                SQLConnection current = commandBoardManager.getSQLConnection();
                if (null != current && current.equals(conn)) {
                    commandBoardManager.setSQLConnection(conn);
                }
                break;

            case CONNECTIONS_LOST:
                Set<SQLConnection> lostConnections = (Set<SQLConnection>) eventData;
                current = commandBoardManager.getSQLConnection();
                for (SQLConnection c : lostConnections) {
                    if (null != current && current.equals(c)) {
                        commandBoardManager.setSQLConnection(c);
                    }
                }
                break;
        }
    }

    private void onCommandManagerEvent(CommandBoardManager.EventType event, SQLExecutionRequest request) {
        switch (event) {
            case COMMAND_AVAILABLE:
                sqlResultsManager.showInfiniteProgressPanel();
                sqlExecutor.submit(request);
                break;

            case COMMAND_CANCEL:
                sqlExecutor.cancelSubmittedRequest(request);
                break;

            case CONNECT_KEYBOARD_REQUEST:
                sqlConnectionManager.onConnectButtonEvent(request.getSQLConnection());
                break;

            case BOARD_CHANGE:
                SQLConnection conn = request.getSQLConnection();
                if (null != conn) {
                    sqlConnectionManager.setSelectedItem(conn);
                }
                break;
        }
    }

    private void onSQLExecutorEvent(SQLExecutor.EventType event, SQLExecutionResponse response) {
        GUIFactory.addToSwingEventQueue(() -> sqlResultsManager.updateStatus(event, response));
        switch (event) {
            case QUERY_STARTED:
            case QUERY_FETCHING:
                break;

            case QUERY_FAILURE:
                GUIFactory.addToSwingEventQueue(sqlResultsManager::removeInfiniteProgressPanel);
                GUIFactory.addToSwingEventQueue(() -> sqlResultsManager.displayError(response.getError()));
                break;

            case QUERY_CANCELLED:
                GUIFactory.addToSwingEventQueue(sqlResultsManager::removeInfiniteProgressPanel);
                GUIFactory.addToSwingEventQueue(sqlResultsManager::clear);
                break;

            case RESULTS_AVAILABLE:
            case QUERY_COMPLETED:
                GUIFactory.addToSwingEventQueue(sqlResultsManager::removeInfiniteProgressPanel);
                long seqNo = response.getSeqNo();
                boolean needsClearing = 0 == seqNo && sqlResultsManager.getRowCount() > 0;
                boolean expectMore = SQLExecutor.EventType.RESULTS_AVAILABLE == event;
                GUIFactory.addToSwingEventQueue(() -> {
                    sqlResultsManager.addRows(response.getResults(), needsClearing, expectMore);
                });
                break;
        }
    }

    private void shutdown() {
        sqlExecutor.close();
        sqlConnectionManager.close();
        sqlResultsManager.close();
        commandBoardManager.close();
    }

    private void setVisible(boolean isVisible) {
        frame.setVisible(isVisible);
    }

    public static void main(String[] args) {
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
