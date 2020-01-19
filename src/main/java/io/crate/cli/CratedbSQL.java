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


    static {
        // antialiased fonts
        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext", "true");
    }
    private static final String LOGO_FILE_NAME = "/cratedb_logo.png";



    private final SQLExecutor sqlExecutor;
    private final SQLConnectionManager sqlConnectionManager;
    private final CommandBoardManager commandBoardManager;
    private final SQLResultsManager[] sqlResultsManager;
    private final JSplitPane sqlResultsManagerPanel;
    private Component currentSqlConnectionManager;

    private CratedbSQL() {
        sqlExecutor = new SQLExecutor(this::onSourceEvent);
        commandBoardManager = new CommandBoardManager(this::onSourceEvent);
        sqlConnectionManager = new SQLConnectionManager(this::onSourceEvent);
        sqlResultsManager = new SQLResultsManager[CommandBoardManager.NUM_COMMAND_BOARDS];
        for (int i = 0; i < sqlResultsManager.length; i++) {
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
        JFrame frame = new JFrame();
        frame.setTitle(String.format(Locale.ENGLISH,"CratedbSQL %s [store: %s]", VERSION, sqlConnectionManager.getStorePath().getAbsolutePath()));
        frame.setType(Window.Type.NORMAL);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(sqlResultsManagerPanel, BorderLayout.CENTER);
        frame.add(centerPanel, BorderLayout.CENTER);
        ImageIcon logo = new ImageIcon(GUIToolkit.class.getResource(LOGO_FILE_NAME));
        frame.setIconImage(logo.getImage());
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        int width = (int) (screenSize.getWidth() * 0.9);
        int height = (int) (screenSize.getHeight() * 0.9);
        int x = (int) (screenSize.getWidth() - width) / 2;
        int y = (int) (screenSize.getHeight() - height) / 2;
        frame.setSize(width, height);
        frame.setLocation(x, y);
        frame.setVisible(true);
        sqlConnectionManager.start();
        sqlExecutor.start();
        Runtime.getRuntime().addShutdownHook(new Thread(
                CratedbSQL.this::shutdown,
                String.format(
                        Locale.ENGLISH,
                        "%s shutdown tasks",
                        CratedbSQL.class.getSimpleName())));
    }

    private void shutdown() {
        sqlExecutor.close();
        sqlConnectionManager.close();
        commandBoardManager.close();
        Arrays.stream(sqlResultsManager).forEach(SQLResultsManager::close);
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
        int offset = CommandBoardManager.fromCommandBoardKey(request.getKey());
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
                commandBoardManager.requestFocus();
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
                commandBoardManager.requestFocus();
                break;
        }
    }

    private void onSQLExecutorEvent(SQLExecutor.EventType event, SQLExecutionResponse response) {
        int offset = CommandBoardManager.fromCommandBoardKey(response.getKey());
        GUIToolkit.invokeLater(() -> sqlResultsManager[offset].updateStatus(event, response));
        switch (event) {
            case QUERY_STARTED:
            case QUERY_FETCHING:
                break;

            case QUERY_FAILURE:
                GUIToolkit.invokeLater(
                        sqlResultsManager[offset]::removeInfiniteProgressPanel,
                        () -> sqlResultsManager[offset].displayError(response.getError()));
                break;

            case QUERY_CANCELLED:
                GUIToolkit.invokeLater(
                        sqlResultsManager[offset]::removeInfiniteProgressPanel,
                        sqlResultsManager[offset]::clear,
                        () -> sqlResultsManager[offset].displayError(response.getError()),
                        commandBoardManager::requestFocus);
                break;

            case RESULTS_AVAILABLE:
            case QUERY_COMPLETED:
                GUIToolkit.invokeLater(sqlResultsManager[offset]::removeInfiniteProgressPanel);
                long seqNo = response.getSeqNo();
                boolean needsClearing = 0 == seqNo && sqlResultsManager[offset].getRowCount() > 0;
                boolean expectMore = SQLExecutor.EventType.RESULTS_AVAILABLE == event;
                GUIToolkit.invokeLater(() ->
                        sqlResultsManager[offset].addRows(response.getResults(), needsClearing, expectMore),
                        () -> { if (false == expectMore) commandBoardManager.requestFocus(); }
                );

                break;
        }
    }

    public static void main(String[] args) {
        new CratedbSQL();
    }
}
