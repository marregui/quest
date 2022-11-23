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
import io.quest.frontend.editor.meta.Meta;
import io.quest.model.*;
import io.quest.frontend.GTk;
import io.questdb.ServerMain;
import io.questdb.std.Misc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.quest.frontend.editor.QuestPanel;
import io.quest.frontend.conns.ConnsManager;
import io.quest.frontend.results.SQLResultsTable;

import static io.quest.frontend.GTk.NO_KEY_EVENT;
import static io.quest.frontend.GTk.configureMenuItem;


public final class Quest {
    private static final Logger LOGGER = LoggerFactory.getLogger(Quest.class);
    public static final String NAME = "quest";
    public static final String VERSION = "1.0";


    private final ConnsManager conns;
    private final SQLExecutor executor;
    private final QuestPanel commands;
    private final SQLResultsTable results;
    private final Meta meta;
    private final JMenuItem toggleConnsWidget;
    private final JMenuItem toggleQuestDB;
    private final JMenuItem toggleMetaExaminerWidget;
    private final JMenuItem toggleConn;

    private ServerMain questdb;

    private Quest() {
        JFrame frame = GTk.frame(null, this::close);
        frame.setIconImage(GTk.Icon.APPLICATION.icon().getImage());
        int width = frame.getWidth();
        int dividerHeight = (int) (frame.getHeight() * 0.6);
        executor = new SQLExecutor();
        conns = new ConnsManager(frame, this::dispatchEvent);
        commands = new QuestPanel(this::dispatchEvent);
        commands.setPreferredSize(new Dimension(0, dividerHeight));
        results = new SQLResultsTable(width, dividerHeight);
        meta = new Meta(frame, this::dispatchEvent);
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, commands, results);
        splitPane.setDividerLocation(dividerHeight);
        frame.add(splitPane, BorderLayout.CENTER);
        frame.setTitle(String.format("%s %s [store: %s]", NAME, VERSION, Store.ROOT_PATH));
        toggleConnsWidget = new JMenuItem();
        toggleConn = new JMenuItem();
        toggleQuestDB = new JMenuItem();
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
        JMenu connsMenu = GTk.jmenu("Connections", GTk.Icon.CONNS);
        connsMenu.add(configureMenuItem(
                toggleConnsWidget,
                GTk.Icon.CONN_SHOW,
                "Connections",
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
        JMenu commandsMenu = GTk.jmenu("Commands", GTk.Icon.COMMANDS);
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
        JMenu resultsMenu = GTk.jmenu("Results", GTk.Icon.RESULTS);
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

        JMenu menu = GTk.jmenu("Menu", GTk.Icon.NO_ICON);
        menu.add(commands.getQuestsMenu()); // Quests Menu
        menu.addSeparator();
        menu.add(configureMenuItem(
                toggleQuestDB,
                GTk.Icon.ROCKET,
                "Run QuestDB",
                KeyEvent.VK_PERIOD,
                this::onToggleQuestDB
        ));
        menu.addSeparator();
        menu.add(configureMenuItem(
                toggleMetaExaminerWidget,
                GTk.Icon.META,
                "Meta Explorer",
                KeyEvent.VK_M,
                this::onToggleMetaExaminerWidget
        ));
        menu.addSeparator();
        menu.add(connsMenu);
        menu.add(commandsMenu);
        menu.add(resultsMenu);
        menu.addSeparator();
        menu.add(configureMenuItem(
                new JMenuItem(),
                GTk.Icon.HELP,
                "QuestDB Docs",
                NO_KEY_EVENT,
                GTk::openQuestDBDocs
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
        toggleConnsWidget.setText(wasVisible ? "Connections" : "Hide Connections");
        toggleConnsWidget.setIcon((wasVisible ? GTk.Icon.CONN_SHOW : GTk.Icon.CONN_HIDE).icon());
    }

    private void onToggleMetaExaminerWidget(ActionEvent event) {
        boolean wasVisible = meta.isVisible();
        if (!wasVisible) {
            meta.setLocation(MouseInfo.getPointerInfo().getLocation());
        }
        meta.setVisible(!wasVisible);
        toggleMetaExaminerWidget.setText(wasVisible ? "Meta Explorer" : "Close Meta Explorer");
    }

    private void onToggleQuestDB(ActionEvent event) {
        if (questdb == null) {
            questdb = new ServerMain("-d", Store.ROOT_PATH.getAbsolutePath());
            questdb.start(false);
            toggleQuestDB.setText("Shutdown QuestDB");
            results.displayMessage("QuestDB is UP");
            Conn conn = commands.getConnection();
            if (conn == null || !conn.isValid()) {
                onToggleConn(null);
            }
        } else {
            questdb.close();
            questdb = null;
            toggleQuestDB.setText("Run QuestDB");
            results.displayMessage("QuestDB is DOWN");
            onToggleConn(null);
        }
    }

    private void dispatchEvent(EventProducer<?> source, Enum<?> event, Object data) {
        if (source instanceof QuestPanel) {
            onCommandBoardEvent(EventProducer.eventType(event), (SQLExecutionRequest) data);
        } else if (source instanceof SQLExecutor) {
            onSQLExecutorEvent(EventProducer.eventType(event), (SQLExecutionResponse) data);
        } else if (source instanceof ConnsManager) {
            onDBConnectionManagerEvent(EventProducer.eventType(event), data);
        } else if (source instanceof Meta) {
            onMetaExaminerEvent(EventProducer.eventType(event));
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

    private void onMetaExaminerEvent(Meta.EventType event) {
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
        Misc.free(commands);
        Misc.free(executor);
        Misc.free(conns);
        Misc.free(results);
        Misc.free(meta);
        Misc.free(questdb);
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
