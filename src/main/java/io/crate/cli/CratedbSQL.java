package io.crate.cli;

import io.crate.cli.common.EventSpeaker;
import io.crate.cli.common.GUIToolkit;
import io.crate.cli.backend.SQLConnection;
import io.crate.cli.backend.SQLExecutionRequest;
import io.crate.cli.backend.SQLExecutionResponse;
import io.crate.cli.backend.SQLExecutor;
import io.crate.cli.widgets.CommandBoardManager;
import io.crate.cli.widgets.SQLConnectionManager;
import io.crate.cli.widgets.SQLResultsManager;

import javax.swing.*;
import java.awt.*;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;


public class CratedbSQL {

    public static final String VERSION = "1.0.0";


    private final SQLExecutor sqlExecutor;
    private final SQLConnectionManager sqlConnectionManager;
    private final CommandBoardManager commandBoardManager;
    private final SQLResultsManager[] sqlResultsManager;
    private final JFrame frame;
    private final JSplitPane sqlResultsManagerPanel;
    private Component currentSqlConnectionManager;


    private CratedbSQL() {
        sqlExecutor = new SQLExecutor(this::onSourceEvent);
        commandBoardManager = new CommandBoardManager(this::onSourceEvent);
        sqlConnectionManager = new SQLConnectionManager(this::onSourceEvent);
        sqlResultsManager = new SQLResultsManager[GUIToolkit.NUM_COMMAND_BOARDS];
        for (int i = 0; i < GUIToolkit.NUM_COMMAND_BOARDS; i++) {
            sqlResultsManager[i] = new SQLResultsManager();
        }
        currentSqlConnectionManager = sqlResultsManager[0];
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(sqlConnectionManager, BorderLayout.NORTH);
        topPanel.add(commandBoardManager, BorderLayout.CENTER);
        sqlResultsManagerPanel = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                false,
                topPanel,
                currentSqlConnectionManager);
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(sqlResultsManagerPanel, BorderLayout.CENTER);
        frame = GUIToolkit.newFrame(
                String.format(
                        Locale.ENGLISH,
                        "CratedbSQL %s [store: %s]",
                        VERSION,
                        sqlConnectionManager.getStorePath().getAbsolutePath()),
                GUIToolkit.FRAME_WIDTH_AS_PERCENT_OF_SCREEN_WIDTH,
                GUIToolkit.FRAME_HEIGHT_AS_PERCENT_OF_SCREEN_WIDTH,
                mainPanel);
        sqlConnectionManager.start();
        sqlExecutor.start();
    }

    private void switchSQLResultsManager(int offset) {
        sqlResultsManagerPanel.remove(currentSqlConnectionManager);
        currentSqlConnectionManager = sqlResultsManager[offset];
        sqlResultsManagerPanel.add(currentSqlConnectionManager);
        sqlResultsManagerPanel.validate();
        sqlResultsManagerPanel.repaint();
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
        int offset = GUIToolkit.fromCommandBoardKey(request.getKey());
        switch (event) {
            case COMMAND_AVAILABLE:
                switchSQLResultsManager(offset);
                sqlResultsManager[offset].showInfiniteProgressPanel();
                sqlExecutor.submit(request);
                commandBoardManager.store();
                break;

            case COMMAND_CANCEL:
                sqlExecutor.cancelSubmittedRequest(request);
                sqlResultsManager[offset].removeInfiniteProgressPanel();
                sqlResultsManager[offset].clear();
                break;

            case CONNECT_KEYBOARD_REQUEST:
                sqlConnectionManager.onConnectButtonEvent(request.getSQLConnection());
                break;

            case BOARD_CHANGE:
                SQLConnection conn = request.getSQLConnection();
                if (null != conn) {
                    sqlConnectionManager.setSelectedItem(conn);
                    switchSQLResultsManager(offset);
                }
                break;
        }
    }

    private void onSQLExecutorEvent(SQLExecutor.EventType event, SQLExecutionResponse response) {
        int offset = GUIToolkit.fromCommandBoardKey(response.getKey());
        GUIToolkit.addToSwingEventQueue(() -> sqlResultsManager[offset].updateStatus(event, response));
        switch (event) {
            case QUERY_STARTED:
            case QUERY_FETCHING:
                break;

            case QUERY_FAILURE:
                GUIToolkit.addToSwingEventQueue(
                        sqlResultsManager[offset]::removeInfiniteProgressPanel,
                        () -> sqlResultsManager[offset].displayError(response.getError()));
                break;

            case QUERY_CANCELLED:
                GUIToolkit.addToSwingEventQueue(
                        sqlResultsManager[offset]::removeInfiniteProgressPanel,
                        sqlResultsManager[offset]::clear,
                        () -> sqlResultsManager[offset].displayError(response.getError()));
                break;

            case RESULTS_AVAILABLE:
            case QUERY_COMPLETED:
                GUIToolkit.addToSwingEventQueue(sqlResultsManager[offset]::removeInfiniteProgressPanel);
                long seqNo = response.getSeqNo();
                boolean needsClearing = 0 == seqNo && sqlResultsManager[offset].getRowCount() > 0;
                boolean expectMore = SQLExecutor.EventType.RESULTS_AVAILABLE == event;
                GUIToolkit.addToSwingEventQueue(() ->
                        sqlResultsManager[offset].addRows(response.getResults(), needsClearing, expectMore)
                );
                break;
        }
    }

    private void shutdown() {
        sqlExecutor.close();
        sqlConnectionManager.close();
        commandBoardManager.close();
        Arrays.stream(sqlResultsManager).forEach(SQLResultsManager::close);
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
