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

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.URL;
import java.util.Locale;
import java.util.Set;


public class CratedbSQL {

    public static final String VERSION = "2.0.0";


    private final SQLConnectionManager sqlConnectionManager;
    private final CommandBoardManager commandBoardManager;
    private final SQLResultsManager sqlResultsManager;
    private final SQLExecutor sqlExecutor;
    private final JMenuItem toggleConnectionsPannelMI;
    private final JMenuItem toggleConnectionMI;


    private CratedbSQL() {
        JFrame frame = new JFrame();
        Dimension frameDimension = GUIToolkit.frameDimension();
        Dimension frameLocation = GUIToolkit.frameLocation(frameDimension);
        frame.setSize(frameDimension.width, frameDimension.height);
        frame.setLocation(frameLocation.width, frameLocation.height);
        int commandBoardManagerHeight = (int)(frameDimension.height * 0.6);
        commandBoardManager = new CommandBoardManager(commandBoardManagerHeight, this::onSourceEvent);
        sqlConnectionManager = new SQLConnectionManager(frame, this::onSourceEvent);
        sqlResultsManager = new SQLResultsManager();
        sqlExecutor = new SQLExecutor();
        JSplitPane sqlResultsManagerPanel = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                false,
                commandBoardManager,
                sqlResultsManager);
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
                sqlResultsManager.closeRowPeeker();
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
                sqlResultsManager.closeRowPeeker();
            }
        });
        frame.setLayout(new BorderLayout());
        JPanel centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(sqlResultsManagerPanel, BorderLayout.CENTER);
        frame.add(centerPanel, BorderLayout.CENTER);
        try {
            URL url = GUIToolkit.class.getResource("/cratedb_logo.png");
            ImageIcon logo = new ImageIcon(ImageIO.read(url));
            frame.setIconImage(logo.getImage());
        } catch (Exception e) {
            e.printStackTrace();
        }
        sqlResultsManagerPanel.setDividerLocation(commandBoardManagerHeight);
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
        frame.setVisible(true);
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
        commandBoardMenu.add(configureMenuItem("Clear", KeyEvent.VK_BACK_SPACE, commandBoardManager::onClearButtonEvent));
        commandBoardMenu.add(configureMenuItem("Run current line", KeyEvent.VK_L, commandBoardManager::onRunCurrentLineEvent));
        commandBoardMenu.add(configureMenuItem("Run board contents", KeyEvent.VK_ENTER, commandBoardManager::onRunEvent));
        commandBoardMenu.add(configureMenuItem("Cancel board execution", KeyEvent.VK_C, commandBoardManager::onCancelButtonEvent));

        JMenu resultsTableMenu = new JMenu("Results table");
        resultsTableMenu.add(configureMenuItem("PREV", KeyEvent.VK_N, sqlResultsManager::onPrevButtonEvent));
        resultsTableMenu.add(configureMenuItem("NEXT", KeyEvent.VK_M, sqlResultsManager::onNextButtonEvent));

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
        menuItem.setAccelerator(KeyStroke.getKeyStroke(keyEvent, InputEvent.CTRL_DOWN_MASK));
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
        sqlResultsManager.closeRowPeeker();
        boolean wasVisible = sqlConnectionManager.isVisible();
        sqlConnectionManager.setVisible(!wasVisible);
        toggleConnectionsPannelMI.setText(wasVisible ?
                "Show connections panel" : "Hide connections panel");
    }

    private void shutdown() {
        sqlExecutor.close();
        sqlConnectionManager.close();
        commandBoardManager.close();
        sqlResultsManager.close();
    }

    private void onSourceEvent(EventSpeaker<?> source, Enum<?> eventType, Object eventData) {
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
                @SuppressWarnings("unchecked")
                Set<SQLConnection> lostConnections = (Set<SQLConnection>) eventData;
                current = commandBoardManager.getSQLConnection();
                for (SQLConnection c : lostConnections) {
                    if (null != current && current.equals(c)) {
                        commandBoardManager.setSQLConnection(c);
                    }
                }
                break;

            case HIDE_REQUEST:
                onToggleConnectionsPanelEvent(null);
                break;
        }
    }

    private void onCommandManagerEvent(CommandBoardManager.EventType event, SQLExecutionRequest request) {
        switch (event) {
            case COMMAND_AVAILABLE:
                SQLConnection conn = commandBoardManager.getSQLConnection();
                if (null == conn || !conn.checkConnectivity()) {
                    onToggleConnectionEvent(null);
                }
                sqlResultsManager.clear();
                sqlResultsManager.showInfiniteProgressPanel();
                sqlExecutor.submit(request, this::onSourceEvent);
                commandBoardManager.store();
                break;

            case COMMAND_CANCEL:
                sqlExecutor.cancelSubmittedRequest(request);
                break;

            case CONNECTION_STATUS_CLICKED:
                onToggleConnectionsPanelEvent(null);
                break;
        }
    }

    private void onSQLExecutorEvent(SQLExecutor.EventType event, SQLExecutionResponse response) {
        invokeLater(() -> sqlResultsManager.updateStatus(event, response));
        switch (event) {
            case QUERY_FAILURE:
            case QUERY_CANCELLED:
                invokeLater(
                        sqlResultsManager::clear,
                        () -> sqlResultsManager.displayError(response.getError()));
                break;

            case RESULTS_AVAILABLE:
                invokeLater(() ->
                    sqlResultsManager.addRows(response.getResults(), true)
                );
                break;

            case QUERY_COMPLETED:
                invokeLater(() -> {
                    sqlResultsManager.addRows(response.getResults(), false);
                    sqlResultsManager.removeInfiniteProgressPanel();
                });
                break;
        }
    }

    private static void invokeLater(Runnable ... runnable) {
        if (EventQueue.isDispatchThread()) {
            for (Runnable r : runnable) {
                if (null != r) {
                    r.run();
                }
            }
        } else {
            try {
                EventQueue.invokeLater(() -> {
                    for (Runnable r : runnable) {
                        if (null != r) {
                            r.run();
                        }
                    }
                });
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

    public static void main(String[] args) {
        invokeLater(CratedbSQL::new);
    }
}
