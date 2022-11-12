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
 * Copyright (c) 2019 - 2022, Miguel Arregui a.k.a. marregui
 */

package io.quest;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.Set;

import javax.swing.*;

import io.quest.backend.SQLExecutor;
import io.quest.backend.SQLExecutionRequest;
import io.quest.backend.SQLExecutionResponse;
import io.quest.frontend.meta.MetaExaminer;
import io.quest.model.*;
import io.quest.frontend.GTk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quest.frontend.editor.QuestPanel;
import io.quest.frontend.conns.ConnsManager;
import io.quest.frontend.results.SQLResultsTable;

import static io.quest.frontend.GTk.configureMenuItem;


public final class Quest {

    private static final Logger LOGGER = LoggerFactory.getLogger(Quest.class);
    public static final String NAME = "quest";
    public static final String VERSION = "1.0";


    private final ConnsManager conns;
    private final SQLExecutor executor;
    private final QuestPanel commands;
    private final SQLResultsTable results;
    private final MetaExaminer metaExaminer;
    private final JMenuItem toggleConnsWidget;
    private final JMenuItem toggleMetaExaminerWidget;
    private final JMenuItem toggleConn;

    private Quest() {
        JFrame frame = GTk.createFrame(null, this::close);
        frame.setIconImage(GTk.Icon.APPLICATION.icon().getImage());
        int width = frame.getWidth();
        int dividerHeight = (int) (frame.getHeight() * 0.6);
        executor = new SQLExecutor(); // input/output
        conns = new ConnsManager(frame, this::dispatchEvent); // input
        metaExaminer = new MetaExaminer(frame, this::dispatchEvent);
        commands = new QuestPanel(this::dispatchEvent); // input
        commands.setPreferredSize(new Dimension(0, dividerHeight));
        results = new SQLResultsTable(width, dividerHeight); // output
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, commands, results);
        splitPane.setDividerLocation(dividerHeight);
        frame.add(splitPane, BorderLayout.CENTER);
        frame.setTitle(String.format("%s %s [store: %s]", NAME, VERSION, conns.getStorePath()));
        toggleConnsWidget = new JMenuItem();
        toggleConn = new JMenuItem();
        toggleMetaExaminerWidget = new JMenuItem();
        frame.setJMenuBar(createMenuBar());
        Runtime.getRuntime().addShutdownHook(new Thread(this::close, "shutdown-hook"));
        LOGGER.info(GTk.BANNER + "  Version " + VERSION + "\n");
        executor.start();
        conns.start();
        frame.setVisible(true);
    }

    private JMenuBar createMenuBar() {
        // Connections
        JMenu connsMenu = new JMenu("Connections");
        connsMenu.setFont(GTk.MENU_FONT);
        connsMenu.setIcon(GTk.Icon.CONNS.icon());
        connsMenu.add(configureMenuItem(
                toggleConnsWidget,
                GTk.Icon.CONN_SHOW,
                "Show connections",
                KeyEvent.VK_T,
                this::onToggleConnsWidget
        ));
        connsMenu.add(configureMenuItem(
                toggleConn,
                GTk.Icon.CONN_CONNECT,
                "Connect",
                KeyEvent.VK_O,
                this::onToggleConn
        ));
        // Commands
        JMenu commandsMenu = new JMenu("Commands");
        commandsMenu.setFont(GTk.MENU_FONT);
        commandsMenu.setIcon(GTk.Icon.COMMANDS.icon());
        JMenu commandBoardMenu = commands.getQuestsMenu();
        commandBoardMenu.setText("uest");
        commandBoardMenu.setIcon(GTk.Icon.COMMAND_QUEST.icon());
        commandsMenu.add(commandBoardMenu);
        commandsMenu.addSeparator();
        commandsMenu.add(configureMenuItem(
                new JMenuItem(),
                GTk.Icon.COMMAND_EXEC_LINE,
                "L.Exec",
                KeyEvent.VK_L,
                commands::onExecLine
        ));
        commandsMenu.add(configureMenuItem(
                new JMenuItem(),
                GTk.Icon.COMMAND_EXEC,
                "Exec",
                KeyEvent.VK_ENTER,
                commands::onExec
        ));
        commandsMenu.add(configureMenuItem(
                new JMenuItem(),
                GTk.Icon.COMMAND_EXEC_ABORT,
                "Abort",
                KeyEvent.VK_W,
                commands::fireCancelEvent
        ));
        commandsMenu.addSeparator();
        commandsMenu.add(configureMenuItem(
                new JMenuItem(),
                GTk.Icon.COMMAND_FIND,
                "Find",
                KeyEvent.VK_F,
                e -> commands.onFind()
        ));
        commandsMenu.add(configureMenuItem(
                new JMenuItem(),
                GTk.Icon.COMMAND_REPLACE,
                "Replace",
                KeyEvent.VK_R,
                e -> commands.onReplace()
        ));
        // Results
        JMenu resultsMenu = new JMenu("Results");
        resultsMenu.setFont(GTk.MENU_FONT);
        resultsMenu.setIcon(GTk.Icon.RESULTS.icon());
        resultsMenu.add(configureMenuItem(new JMenuItem(),
                GTk.Icon.RESULTS_PREV,
                "PREV",
                KeyEvent.VK_B,
                results::onPrevButton
        ));
        resultsMenu.add(configureMenuItem(
                new JMenuItem(),
                GTk.Icon.RESULTS_NEXT,
                "NEXT",
                KeyEvent.VK_N,
                results::onNextButton
        ));

        JMenu menu = new JMenu("Menu");
        menu.setFont(GTk.MENU_FONT);
        menu.add(connsMenu);
        menu.add(commandsMenu);
        menu.add(resultsMenu);
        menu.addSeparator();
        menu.add(configureMenuItem(
                toggleMetaExaminerWidget,
                GTk.Icon.META,
                "Show MetaExplorer",
                KeyEvent.VK_M,
                this::onToggleMetaExaminerWidget
        ));
        menu.add(configureMenuItem(
                new JMenuItem(),
                GTk.Icon.HELP,
                "QuestDB Docs",
                GTk.NO_KEY_EVENT,
                GTk::openQuestDBDocumentation
        ));
        JMenuBar menuBar = new JMenuBar();
        menuBar.setBorder(BorderFactory.createLoweredSoftBevelBorder());
        menuBar.add(menu);
        return menuBar;
    }

    private void onToggleConn(ActionEvent event) {
        Conn conn = commands.getConnection();
        conns.onConnectEvent(conn);
        toggleConn.setText(conn != null && conn.isOpen() ? "Connect" : "Disconnect");
    }

    private void onToggleConnsWidget(ActionEvent event) {
        boolean wasVisible = conns.isVisible();
        if (!wasVisible) {
            conns.setLocation(MouseInfo.getPointerInfo().getLocation());
        }
        conns.setVisible(!wasVisible);
        toggleConnsWidget.setText(wasVisible ? "Show connections" : "Hide connections");
        toggleConnsWidget.setIcon((wasVisible ? GTk.Icon.CONN_SHOW : GTk.Icon.CONN_HIDE).icon());
    }

    private void onToggleMetaExaminerWidget(ActionEvent event) {
        boolean wasVisible = metaExaminer.isVisible();
        if (!wasVisible) {
            metaExaminer.setLocation(MouseInfo.getPointerInfo().getLocation());
        }
        metaExaminer.setVisible(!wasVisible);
        toggleMetaExaminerWidget.setText(wasVisible ? "Show MetaExplorer" : "Hide MetaExplorer");
    }

    private void dispatchEvent(EventProducer<?> source, Enum<?> event, Object data) {
        if (source instanceof QuestPanel) {
            onCommandBoardEvent(EventProducer.eventType(event), (SQLExecutionRequest) data);
        } else if (source instanceof SQLExecutor) {
            onSQLExecutorEvent(EventProducer.eventType(event), (SQLExecutionResponse) data);
        } else if (source instanceof ConnsManager) {
            onDBConnectionManagerEvent(EventProducer.eventType(event), data);
        } else if (source instanceof MetaExaminer) {
            onMetaExaminerEvent(EventProducer.eventType(event), data);
        }
    }

    private void onCommandBoardEvent(QuestPanel.EventType event, SQLExecutionRequest req) {
        switch (event) {
            case COMMAND_AVAILABLE:
                Conn conn = commands.getConnection();
                if (conn == null || !conn.isValid()) {
                    onToggleConn(null);
                }
                results.close();
                executor.submit(req, this::dispatchEvent);
                break;

            case COMMAND_CANCEL:
                executor.cancelExistingRequest(req);
                onToggleConn(null);
                break;

            case CONNECTION_STATUS_CLICKED:
                onToggleConnsWidget(null);
                break;
        }
    }

    private void onSQLExecutorEvent(SQLExecutor.EventType event, SQLExecutionResponse res) {
        GTk.invokeLater(() -> results.updateStats(event.name(), res));
        switch (event) {
            case STARTED:
                GTk.invokeLater(results::showInfiniteSpinner);
                break;

            case RESULTS_AVAILABLE:
            case COMPLETED:
                GTk.invokeLater(() -> results.onRowsAdded(res));
                break;

            case CANCELLED:
                GTk.invokeLater(results::close);
                break;

            case FAILURE:
                GTk.invokeLater(results::close, () -> results.displayError(res.getError()));
                break;
        }
    }

    private void onMetaExaminerEvent(MetaExaminer.EventType event, Object data) {
        switch (event) {
            case HIDE_REQUEST:
                onToggleMetaExaminerWidget(null);
                break;
        }
    }

    private void onDBConnectionManagerEvent(ConnsManager.EventType event, Object data) {
        switch (event) {
            case CONNECTION_SELECTED:
                commands.setConnection((Conn) data);
                if (conns.isVisible()) {
                    onToggleConnsWidget(null);
                }
                break;

            case CONNECTION_ESTABLISHED:
            case CONNECTION_CLOSED:
                Conn conn = (Conn) data;
                Conn current = commands.getConnection();
                if (current != null && current.equals(conn)) {
                    commands.setConnection(conn);
                    toggleConn.setText(conn.isOpen() ? "Disconnect" : "Connect");
                }
                break;

            case CONNECTIONS_LOST:
                @SuppressWarnings("unchecked")
                Set<Conn> droppedConns = (Set<Conn>) data;
                current = commands.getConnection();
                if (current != null) {
                    for (Conn dc : droppedConns) {
                        if (current.equals(dc)) {
                            commands.setConnection(dc);
                        }
                    }
                }
                break;

            case HIDE_REQUEST:
                onToggleConnsWidget(null);
                break;
        }
    }

    private void close() {
        commands.close();
        executor.close();
        conns.close();
        results.close();
        metaExaminer.close();
    }

    public static void main(String[] args) {
        final String lookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();
        try {
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (Exception e) {
            LOGGER.warn("CrossPlatformLookAndFeel [{}] unavailable", lookAndFeel);
        }
        GTk.invokeLater(Quest::new);
    }
}
