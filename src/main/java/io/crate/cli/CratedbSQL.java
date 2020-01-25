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
import java.awt.event.*;
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
        adjustDividerLocation();
        JFrame frame = new JFrame();
        frame.setTitle(String.format(Locale.ENGLISH,
                "%s %s [store: %s]",
                CratedbSQL.class.getSimpleName(),
                VERSION,
                sqlConnectionManager.getStorePath().getAbsolutePath()));
        frame.setType(Window.Type.NORMAL);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.addWindowFocusListener(new WindowFocusListener() {

            @Override
            public void windowGainedFocus(WindowEvent e) {
                currentsqlResultsManager.closeRowPeeker();
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                currentsqlResultsManager.closeRowPeeker();
            }
        });
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
        toggleConnectionsPannelMI = new JMenuItem();
        toggleConnectionMI = new JMenuItem();
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
        connectionsMenu.add(configureMenuItem(
                toggleConnectionsPannelMI,
                "Show connections panel",
                KeyEvent.VK_T,
                this::onToggleConnectionsPanelEvent));
        connectionsMenu.add(configureMenuItem(
                toggleConnectionMI,
                "Connect",
                KeyEvent.VK_O,
                this::onToggleConnectionEvent));

        JMenu commandBoardMenu = new JMenu("Command board");
        JMenu switchBoard = new JMenu("Switch to board");
        int keyEvent = KeyEvent.VK_1;
        for (int i = 0; i < CommandBoardManager.NUM_COMMAND_BOARDS; i++) {
            int offset = i;
            switchBoard.add(configureMenuItem(
                    CommandBoardManager.toCommandBoardKey(offset),
                    keyEvent,
                    e -> commandBoardManager.onBoardBufferEvent(offset)
            ));
            keyEvent++;
        }
        commandBoardMenu.add(switchBoard);
        commandBoardMenu.add(configureMenuItem("Clear", KeyEvent.VK_BACK_SPACE, commandBoardManager::onClearButtonEvent));
        commandBoardMenu.add(configureMenuItem("Run current line", KeyEvent.VK_L, commandBoardManager::onRunCurrentLineEvent));
        commandBoardMenu.add(configureMenuItem("Run board contents", KeyEvent.VK_ENTER, commandBoardManager::onRunEvent));
        commandBoardMenu.add(configureMenuItem("Cancel board execution", KeyEvent.VK_C, commandBoardManager::onCancelButtonEvent));

        JMenu resultsTableMenu = new JMenu("Results table");
        resultsTableMenu.add(configureMenuItem("PREV", KeyEvent.VK_N, currentsqlResultsManager::onPrevButtonEvent));
        resultsTableMenu.add(configureMenuItem("NEXT", KeyEvent.VK_M, currentsqlResultsManager::onNextButtonEvent));

        JMenu cratedbSqlMenu = new JMenu(CratedbSQL.class.getSimpleName());
        cratedbSqlMenu.setFont(GUIToolkit.TABLE_CELL_FONT);
        cratedbSqlMenu.add(connectionsMenu);
        cratedbSqlMenu.add(commandBoardMenu);
        cratedbSqlMenu.add(resultsTableMenu);

        JMenuBar menuBar = new JMenuBar();
        menuBar.setBorder(BorderFactory.createEtchedBorder());
        menuBar.add(cratedbSqlMenu);
        return menuBar;
    }

    private static JMenuItem configureMenuItem(JMenuItem menuItem,
                                               String title,
                                               int keyEvent,
                                               ActionListener actionListener) {
        menuItem.setText(title);
        menuItem.setMnemonic(keyEvent);
        menuItem.setAccelerator(KeyStroke.getKeyStroke(keyEvent, ActionEvent.CTRL_MASK));
        menuItem.addActionListener(actionListener);
        return menuItem;
    }

    private static JMenuItem configureMenuItem(String title,
                                               int keyEvent,
                                               ActionListener actionListener) {
        return configureMenuItem(new JMenuItem(), title, keyEvent, actionListener);
    }

    private void onToggleConnectionEvent(ActionEvent event) {
        SQLConnection conn = commandBoardManager.getSQLConnection();
        boolean isConnected = null != conn && conn.isConnected();
        sqlConnectionManager.onConnectButtonEvent(conn);
        toggleConnectionMI.setText(isConnected ? "Connect" : "Disconnect");
    }

    private void onToggleConnectionsPanelEvent(ActionEvent event) {
        boolean wasVisible = sqlConnectionManager.isVisible();
        sqlConnectionManager.setVisible(false == wasVisible);
        toggleConnectionsPannelMI.setText(wasVisible ?
                "Show connections panel" : "Hide connections panel");
        if (wasVisible) {
            sqlResultsManagerPanel.setDividerLocation(
                    GUIToolkit.COMMAND_BOARD_MANAGER_HEIGHT.height);
        } else {
            sqlResultsManagerPanel.setDividerLocation(
                    GUIToolkit.COMMAND_BOARD_MANAGER_HEIGHT.height + GUIToolkit.SQL_CONNECTION_MANAGER_HEIGHT.height);
        }
    }

    private void shutdown() {
        sqlExecutor.close();
        sqlConnectionManager.close();
        commandBoardManager.close();
        Arrays.stream(sqlResultsManager).forEach(SQLResultsManager::close);
    }

    private void switchSQLResultsManager(int offset) {
        currentsqlResultsManager.closeRowPeeker();
        sqlResultsManagerPanel.remove(currentsqlResultsManager);
        currentsqlResultsManager = sqlResultsManager[offset];
        sqlResultsManagerPanel.add(currentsqlResultsManager);
        sqlResultsManagerPanel.validate();
        sqlResultsManagerPanel.repaint();
        adjustDividerLocation();
    }

    private void adjustDividerLocation() {
        int y = sqlConnectionManager.isVisible() ? GUIToolkit.SQL_CONNECTION_MANAGER_HEIGHT.height : 0;
        int dividerMinLocation = y + GUIToolkit.COMMAND_BOARD_MANAGER_HEIGHT.height;
        int dividerLocation = y + sqlResultsManagerPanel.getDividerLocation();
        if (dividerLocation < dividerMinLocation) {
            sqlResultsManagerPanel.setDividerLocation(dividerMinLocation);
        }
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
