/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

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
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;


public class CratedbSQL {

    public static final String VERSION = "1.0.0";

    static {
        // antialiased fonts
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");

        //System.setProperty("apple.laf.useScreenMenuBar", "true");
    }

    private static final String LOGO_FILE_NAME = "/cratedb_logo.png";


    private final SQLConnectionManager sqlConnectionManager;
    private final CommandBoardManager commandBoardManager;
    private final JSplitPane sqlResultsManagerPanel;
    private final SQLResultsManager[] sqlResultsManager;
    private SQLResultsManager currentsqlResultsManager;
    private final SQLExecutor sqlExecutor;
    private final JMenuItem toggleConnectionsPannelMI;
    private final JMenuItem toggleConnectionMI;


    private CratedbSQL() {
        commandBoardManager = new CommandBoardManager(this::onSourceEvent);
        sqlConnectionManager = new SQLConnectionManager(this::onSourceEvent);
        sqlResultsManager = new SQLResultsManager[CommandBoardManager.NUM_COMMAND_BOARDS];
        for (int i = 0; i < sqlResultsManager.length; i++) {
            sqlResultsManager[i] = new SQLResultsManager();
        }
        currentsqlResultsManager = sqlResultsManager[0];
        sqlExecutor = new SQLExecutor();
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(sqlConnectionManager, BorderLayout.NORTH);
        topPanel.add(commandBoardManager, BorderLayout.CENTER);
        sqlResultsManagerPanel = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                false,
                topPanel,
                currentsqlResultsManager);
        JFrame frame = new JFrame();
        frame.setTitle(String.format(Locale.ENGLISH,
                "%s %s [store: %s]",
                CratedbSQL.class.getSimpleName(),
                VERSION,
                sqlConnectionManager.getStorePath().getAbsolutePath()));
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
        toggleConnectionsPannelMI = new JMenuItem("Show connections panel");
        toggleConnectionMI = new JMenuItem("Connect");
        frame.setJMenuBar(createMenuBar());
        Runtime.getRuntime().addShutdownHook(new Thread(
                CratedbSQL.this::shutdown,
                String.format(
                        Locale.ENGLISH,
                        "%s shutdown tasks",
                        CratedbSQL.class.getSimpleName())));
        sqlConnectionManager.start();
        sqlExecutor.start();
    }

    private JMenuBar createMenuBar() {
        JMenu connectionsMenu = new JMenu("Connections");
        toggleConnectionsPannelMI.setMnemonic(KeyEvent.VK_T);
        toggleConnectionsPannelMI.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_T, ActionEvent.CTRL_MASK));
        toggleConnectionsPannelMI.addActionListener(this::onToggleConnectionsPanelEvent);
        connectionsMenu.add(toggleConnectionsPannelMI);

        toggleConnectionMI.setMnemonic(KeyEvent.VK_O);
        toggleConnectionMI.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_O, ActionEvent.CTRL_MASK));
        toggleConnectionMI.addActionListener(this::onToggleConnectionEvent);
        connectionsMenu.add(toggleConnectionMI);

        JMenu commandMenu = new JMenu("Command board");
        JMenu switchBoard = new JMenu("Switch to board");
        int keyEvent = KeyEvent.VK_1;
        for (int i=0; i < CommandBoardManager.NUM_COMMAND_BOARDS; i++) {
            int offset = i;
            JMenuItem board = new JMenuItem(CommandBoardManager.toCommandBoardKey(offset));
            board.setMnemonic(keyEvent);
            board.setAccelerator(KeyStroke.getKeyStroke(
                    keyEvent, ActionEvent.CTRL_MASK));
            board.addActionListener(e -> {
                commandBoardManager.onBoardBufferEvent(offset);
            });
            switchBoard.add(board);
            keyEvent++;
        }
        commandMenu.add(switchBoard);
        JMenuItem clearBoard = new JMenuItem("Clear");
        clearBoard.setMnemonic(KeyEvent.VK_BACK_SPACE);
        clearBoard.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_BACK_SPACE, ActionEvent.CTRL_MASK));
        clearBoard.addActionListener(commandBoardManager::onClearButtonEvent);
        commandMenu.add(clearBoard);
        JMenuItem runLine = new JMenuItem("Run current line");
        runLine.setMnemonic(KeyEvent.VK_L);
        runLine.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_L, ActionEvent.CTRL_MASK));
        runLine.addActionListener(commandBoardManager::onRunCurrentLineEvent);
        commandMenu.add(runLine);
        JMenuItem run = new JMenuItem("Run board contents");
        run.setMnemonic(KeyEvent.VK_ENTER);
        run.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_ENTER, ActionEvent.CTRL_MASK));
        run.addActionListener(commandBoardManager::onRunEvent);
        commandMenu.add(run);
        JMenuItem cancel = new JMenuItem("Cancel board execution");
        cancel.setMnemonic(KeyEvent.VK_C);
        cancel.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_C, ActionEvent.CTRL_MASK));
        cancel.addActionListener(commandBoardManager::onCancelButtonEvent);
        commandMenu.add(cancel);
        JMenu resultsMenu = new JMenu("Results table");
        JMenuItem prevPage = new JMenuItem("PREV");
        prevPage.setMnemonic(KeyEvent.VK_N);
        prevPage.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_N, ActionEvent.CTRL_MASK));
        prevPage.addActionListener(currentsqlResultsManager::onPrevButtonEvent);
        resultsMenu.add(prevPage);
        JMenuItem nextPage = new JMenuItem("NEXT");
        nextPage.setMnemonic(KeyEvent.VK_M);
        nextPage.setAccelerator(KeyStroke.getKeyStroke(
                KeyEvent.VK_M, ActionEvent.CTRL_MASK));
        nextPage.addActionListener(currentsqlResultsManager::onNextButtonEvent);
        resultsMenu.add(nextPage);

        JMenu cratedbSqlMenu = new JMenu(String.format(
                Locale.ENGLISH,
                "%s",
                CratedbSQL.class.getSimpleName()));
        cratedbSqlMenu.setFont(GUIToolkit.TABLE_CELL_FONT);
        cratedbSqlMenu.add(connectionsMenu);
        cratedbSqlMenu.add(commandMenu);
        cratedbSqlMenu.add(resultsMenu);
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBorder(BorderFactory.createEtchedBorder());
        menuBar.add(cratedbSqlMenu);
        return menuBar;
    }

    private void onToggleConnectionEvent(ActionEvent event) {
        SQLConnection conn = commandBoardManager.getSQLConnection();
        boolean isConnected = null != conn && conn.isConnected();
        sqlConnectionManager.onConnectButtonEvent(conn);
        toggleConnectionMI.setText(isConnected ? "Connect" : "Disconnect");
    }

    private void onToggleConnectionsPanelEvent(ActionEvent event) {
        boolean isVisible = sqlConnectionManager.isVisible();
        sqlConnectionManager.setVisible(false == isVisible);
        int location = sqlResultsManagerPanel.getDividerLocation();
        int deltaY = GUIToolkit.SQL_CONNECTION_MANAGER_HEIGHT.height;
        int direction = isVisible ? -1 : 1;
        sqlResultsManagerPanel.setDividerLocation(location + direction * deltaY);
        toggleConnectionsPannelMI.setText(isVisible ?
                "Show connections panel" : "Hide connections panel");
    }

    private void shutdown() {
        sqlExecutor.close();
        sqlConnectionManager.close();
        commandBoardManager.close();
        Arrays.stream(sqlResultsManager).forEach(SQLResultsManager::close);
    }

    private void switchSQLResultsManager(int offset) {
        sqlResultsManagerPanel.remove(currentsqlResultsManager);
        currentsqlResultsManager = sqlResultsManager[offset];
        sqlResultsManagerPanel.add(currentsqlResultsManager);
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
                    toggleConnectionMI.setText(conn.isConnected() ?
                            "Disconnect" : "Connect");
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
        int offset = CommandBoardManager.fromCommandBoardKey(request.getSourceId());
        switch (event) {
            case COMMAND_AVAILABLE:
                switchSQLResultsManager(offset);
                sqlResultsManager[offset].clear();
                sqlResultsManager[offset].showInfiniteProgressPanel();
                sqlExecutor.submit(request, this::onSourceEvent);
                commandBoardManager.store();
                break;

            case COMMAND_CANCEL:
                sqlExecutor.cancelSubmittedRequest(request);
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
        int offset = CommandBoardManager.fromCommandBoardKey(response.getSourceId());
        GUIToolkit.invokeLater(() -> sqlResultsManager[offset].updateStatus(event, response));
        switch (event) {
            case QUERY_FAILURE:
            case QUERY_CANCELLED:
                GUIToolkit.invokeLater(
                        sqlResultsManager[offset]::clear,
                        () -> sqlResultsManager[offset].displayError(response.getError()));
                break;

            case RESULTS_AVAILABLE:
                GUIToolkit.invokeLater(() -> {
                    sqlResultsManager[offset].addRows(response.getResults(), true);
                });
                break;

            case QUERY_COMPLETED:
                GUIToolkit.invokeLater(() -> {
                    sqlResultsManager[offset].addRows(response.getResults(), false);
                    sqlResultsManager[offset].removeInfiniteProgressPanel();
                });
                break;
        }
    }

    public static void main(String[] args) {
        GUIToolkit.invokeLater(CratedbSQL::new);
    }
}
