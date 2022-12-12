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
import java.util.function.Consumer;

import javax.swing.*;

import io.quest.backend.SQLExecutor;
import io.quest.backend.SQLExecutionRequest;
import io.quest.backend.SQLExecutionResponse;
import io.quest.frontend.meta.Meta;
import io.quest.model.*;
import io.quest.frontend.GTk;
import io.questdb.ServerMain;
import io.questdb.log.Log;
import io.questdb.log.LogFactory;
import io.questdb.std.Misc;

import io.quest.frontend.editor.QuestPanel;
import io.quest.frontend.conns.Conns;
import io.quest.frontend.results.SQLResultsTable;

import static io.quest.frontend.GTk.menuItem;
import static io.quest.frontend.GTk.Icon;


public final class Quest {
    public static final String NAME = "quest";
    private static final Log LOG = LogFactory.getLog(Quest.class);

    private final JFrame frame;
    private final QuestPanel commands;
    private final Conns conns;
    private final SQLResultsTable results;
    private final SQLExecutor executor;
    private final JMenuItem toggleConnsWidget;
    private final JMenuItem toggleQuestDB;
    private final JMenuItem toggleMetaExaminerWidget;
    private final JMenuItem toggleConn;
    private final Meta meta;
    private ServerMain questDb;

    private Quest() {
        frame = GTk.frame(String.format("%s [store: %s]", NAME, Store.ROOT_PATH));
        frame.setIconImage(Icon.QUEST.icon().getImage());
        int dividerHeight = (int) (frame.getHeight() * 0.6);
        executor = new SQLExecutor();
        meta = new Meta(frame, this::dispatchEvent);
        conns = new Conns(frame, this::dispatchEvent);
        commands = new QuestPanel(this::dispatchEvent);
        commands.setPreferredSize(new Dimension(0, dividerHeight));
        results = new SQLResultsTable(frame.getWidth(), dividerHeight);
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, commands, results);
        splitPane.setDividerLocation(dividerHeight);
        splitPane.setDividerSize(5);
        frame.add(splitPane, BorderLayout.CENTER);
        toggleConnsWidget = new JMenuItem();
        toggleConn = new JMenuItem();
        toggleQuestDB = new JMenuItem();
        toggleMetaExaminerWidget = new JMenuItem();
        frame.setJMenuBar(createMenuBar());
        Runtime.getRuntime().addShutdownHook(new Thread(this::close, "shutdown-hook"));
        LOG.info().$(GTk.BANNER).$();
        executor.start();
        conns.start();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        final String lookAndFeel = UIManager.getCrossPlatformLookAndFeelClassName();
        try {
            UIManager.setLookAndFeel(lookAndFeel);
        } catch (Exception e) {
            LOG.infoW().$("CrossPlatformLookAndFeel unavailable [name=").$(lookAndFeel).I$();
        }
        GTk.invokeLater(Quest::new);
    }

    private JMenuBar createMenuBar() {
        JMenu connsMenu = GTk.menu(Icon.CONNS, "Connections");
        connsMenu.add(menuItem(toggleConnsWidget, Icon.CONN_SHOW, "Connections", KeyEvent.VK_T, this::onToggleConnsWidget));
        connsMenu.add(menuItem(toggleConn, Icon.CONN_CONNECT, "Connect", KeyEvent.VK_O, this::onToggleConn));

        JMenu commandsMenu = GTk.menu(Icon.COMMANDS, "Commands");
        commandsMenu.add(GTk.menuItem(Icon.COMMAND_EXEC_LINE, "L.Exec", KeyEvent.VK_L, commands::onExecLine));
        commandsMenu.add(GTk.menuItem(Icon.COMMAND_EXEC, "Exec", KeyEvent.VK_ENTER, commands::onExec));
        commandsMenu.add(GTk.menuItem(Icon.COMMAND_EXEC_ABORT, "Abort", KeyEvent.VK_W, commands::fireCancelEvent));
        commandsMenu.addSeparator();
        commandsMenu.add(GTk.menuItem(Icon.COMMAND_FIND, "Find", KeyEvent.VK_F, e -> commands.onFind()));
        commandsMenu.add(GTk.menuItem(Icon.COMMAND_REPLACE, "Replace", KeyEvent.VK_R, e -> commands.onReplace()));

        JMenu resultsMenu = GTk.menu(Icon.RESULTS, "Results");
        resultsMenu.add(GTk.menuItem(Icon.RESULTS_PREV, "PREV", KeyEvent.VK_B, results::onPrevButton));
        resultsMenu.add(GTk.menuItem(Icon.RESULTS_NEXT, "NEXT", KeyEvent.VK_N, results::onNextButton));

        JMenu menu = GTk.menu(Icon.MENU);
        menu.add(commands.getQuestsMenu()); // Quests Menu
        menu.addSeparator();
        menu.add(menuItem(toggleQuestDB, Icon.ROCKET, "Run QuestDB", KeyEvent.VK_PERIOD, this::onToggleQuestDB));
        menu.addSeparator();
        menu.add(menuItem(toggleMetaExaminerWidget, Icon.META, "Meta Explorer", KeyEvent.VK_M, this::onToggleMetaWidget));
        menu.addSeparator();
        menu.add(connsMenu);
        menu.add(commandsMenu);
        menu.add(resultsMenu);
        menu.addSeparator();
        menu.add(GTk.menuItem(Icon.HELP, "QuestDB Docs", KeyEvent.VK_H, GTk::openQuestDBDocs));

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
        onToggleWidget(conns, wasVisible -> {
            toggleConnsWidget.setText(wasVisible ? "Connections" : "Hide Connections");
            toggleConnsWidget.setIcon((wasVisible ? Icon.CONN_SHOW : Icon.CONN_HIDE).icon());
        });
    }

    private void onToggleMetaWidget(ActionEvent event) {
        onToggleWidget(meta, wasVisible -> toggleMetaExaminerWidget.setText(wasVisible ? "Meta Explorer" : "Close Meta Explorer"));
    }

    private void onToggleWidget(JDialog dialog, Consumer<Boolean> consumer) {
        boolean wasVisible = dialog.isVisible();
        if (!wasVisible) {
            Dimension location = GTk.frameLocation(dialog.getSize());
            dialog.setLocation(location.width, location.height);
        }
        dialog.setVisible(!wasVisible);
        consumer.accept(wasVisible);
    }

    private void onToggleQuestDB(ActionEvent event) {
        if (questDb == null) {
            try {
                questDb = new ServerMain("-d", Store.ROOT_PATH.getAbsolutePath());
                questDb.start(false);
                toggleQuestDB.setText("Shutdown QuestDB");
                Conn conn = commands.getConnection();
                if (conn == null || !conn.isValid()) {
                    onToggleConn(null);
                }
            } finally {
                results.displayMessage("QuestDB is UP");
            }
        } else {
            if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
                    frame,
                    "Shutdown QuestDB?",
                    "Choice",
                    JOptionPane.YES_NO_OPTION)
            ) {
                questDb.close();
                questDb = null;
                toggleQuestDB.setText("Run QuestDB");
                results.displayMessage("QuestDB is DOWN");
                onToggleConn(null);
            }
        }
    }

    private void dispatchEvent(EventProducer source, Enum<?> event, Object data) {
        if (source instanceof QuestPanel) {
            onCommandBoardEvent(EventProducer.eventType(event), (SQLExecutionRequest) data);
        } else if (source instanceof SQLExecutor) {
            onSQLExecutorEvent(EventProducer.eventType(event), (SQLExecutionResponse) data);
        } else if (source instanceof Conns) {
            onConnsEvent(EventProducer.eventType(event), data);
        } else if (source instanceof Meta) {
            onMetaExaminerEvent(EventProducer.eventType(event));
        }
    }

    private void onCommandBoardEvent(QuestPanel.EventType event, SQLExecutionRequest req) {
        switch (event) {
            case COMMAND_AVAILABLE -> {
                Conn conn = commands.getConnection();
                if (conn == null || !conn.isValid()) {
                    onToggleConn(null);
                }
                results.close();
                executor.submit(req, this::dispatchEvent);
            }
            case COMMAND_CANCEL -> {
                executor.cancelExistingRequest(req);
                onToggleConn(null);
            }
            case CONNECTION_STATUS_CLICKED -> onToggleConnsWidget(null);
        }
    }

    private void onSQLExecutorEvent(SQLExecutor.EventType event, SQLExecutionResponse res) {
        GTk.invokeLater(() -> results.updateStats(event.name(), res));
        switch (event) {
            case STARTED -> GTk.invokeLater(results::showInfiniteSpinner);
            case RESULTS_AVAILABLE, COMPLETED -> GTk.invokeLater(() -> results.onRowsAdded(res));
            case CANCELLED -> GTk.invokeLater(results::close);
            case FAILURE -> GTk.invokeLater(results::close, () -> results.displayError(res.getError()));
        }
    }

    private void onMetaExaminerEvent(Meta.EventType event) {
        if (event == Meta.EventType.HIDE_REQUEST) {
            onToggleMetaWidget(null);
        }
    }

    private void onConnsEvent(Conns.EventType event, Object data) {
        switch (event) {
            case CONNECTION_SELECTED -> {
                commands.setConnection((Conn) data);
                if (conns.isVisible()) {
                    onToggleConnsWidget(null);
                }
            }
            case CONNECTION_ESTABLISHED, CONNECTION_CLOSED -> {
                Conn conn = (Conn) data;
                Conn current = commands.getConnection();
                if (current != null && current.equals(conn)) {
                    commands.setConnection(conn);
                    toggleConn.setText(conn.isOpen() ? "Disconnect" : "Connect");
                }
            }
            case CONNECTIONS_LOST -> {
                @SuppressWarnings("unchecked") Set<Conn> droppedConns = (Set<Conn>) data;
                Conn current = commands.getConnection();
                if (current != null) {
                    for (Conn dc : droppedConns) {
                        if (current.equals(dc)) {
                            commands.setConnection(dc);
                        }
                    }
                }
            }
            case HIDE_REQUEST -> onToggleConnsWidget(null);
        }
    }

    private void close() {
        Misc.free(executor);
        Misc.free(conns);
        Misc.free(commands);
        Misc.free(results);
        Misc.free(meta);
        Misc.free(questDb);
    }
}
