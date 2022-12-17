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

import io.quest.conns.Conn;
import io.quest.executor.SQLExecutor;
import io.quest.executor.SQLExecutionRequest;
import io.quest.executor.SQLExecutionResponse;
import io.quest.meta.Meta;
import io.quest.store.Store;
import io.questdb.ServerMain;
import io.questdb.log.Log;
import io.questdb.log.LogFactory;
import io.questdb.std.Misc;

import io.quest.quests.QuestsPanel;
import io.quest.conns.Conns;
import io.quest.results.SQLResultsTable;

import static io.quest.GTk.menuItem;
import static io.quest.GTk.Icon;


public final class Quest {
    private static final Log LOG = LogFactory.getLog(Quest.class);

    private final JFrame frame;
    private final QuestsPanel commands;
    private final Conns conns;
    private final SQLResultsTable results;
    private final SQLExecutor executor;
    private final Meta meta;
    private final JMenuItem toggleConns;
    private final JMenuItem toggleQuestDB;
    private final JMenuItem toggleMeta;
    private final JMenuItem toggleAssignedConn;
    private ServerMain questDb;

    private Quest() {
        frame = GTk.frame(String.format("%s [store: %s]", GTk.QUEST_APP_NAME, Store.ROOT_PATH));
        frame.setIconImage(Icon.QUEST.icon().getImage());
        int dividerHeight = (int) (frame.getHeight() * 0.6);
        executor = new SQLExecutor();
        meta = new Meta(frame, this::dispatchEvent);
        conns = new Conns(frame, this::dispatchEvent);
        commands = new QuestsPanel(this::dispatchEvent);
        commands.setPreferredSize(new Dimension(0, dividerHeight));
        results = new SQLResultsTable(frame.getWidth(), dividerHeight);
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, true, commands, results);
        splitPane.setDividerLocation(dividerHeight);
        splitPane.setDividerSize(5);
        frame.add(splitPane, BorderLayout.CENTER);
        toggleConns = new JMenuItem();
        toggleAssignedConn = new JMenuItem();
        toggleQuestDB = new JMenuItem();
        toggleMeta = new JMenuItem();
        frame.setJMenuBar(createMenuBar());
        Runtime.getRuntime().addShutdownHook(new Thread(this::close, "shutdown-hook"));
        LOG.info().$(GTk.BANNER).$();
        executor.start();
        conns.start();
        frame.setVisible(true);
    }

    public static void main(String[] args) {
        GTk.invokeLater(Quest::new);
    }

    private JMenuBar createMenuBar() {
        JMenu connsMenu = GTk.menu(Icon.CONNS, "Connections");
        connsMenu.add(menuItem(toggleConns, Icon.CONN_SHOW, "Connections", KeyEvent.VK_T, this::onToggleConns));
        connsMenu.add(menuItem(toggleAssignedConn, Icon.CONN_CONNECT, "Connect", KeyEvent.VK_O, this::onToggleAssignedConn));

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
        menu.add(commands.getQuestsMenu());
        menu.addSeparator();
        menu.add(menuItem(toggleQuestDB, Icon.ROCKET, "Run QuestDB", KeyEvent.VK_PERIOD, this::onToggleQuestDB));
        menu.addSeparator();
        menu.add(menuItem(toggleMeta, Icon.META, "Meta Explorer", KeyEvent.VK_M, this::onToggleMeta));
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

    private void onToggleAssignedConn(ActionEvent event) {
        Conn conn = commands.getConnection();
        conns.onConnectEvent(conn);
        toggleAssignedConn.setText(conn != null && conn.isOpen() ? "Connect" : "Disconnect");
    }

    private void onToggleConns(ActionEvent event) {
        onToggleWidget(conns, wasVisible -> {
            toggleConns.setText(wasVisible ? "Connections" : "Hide Connections");
            toggleConns.setIcon((wasVisible ? Icon.CONN_SHOW : Icon.CONN_HIDE).icon());
        });
    }

    private void onToggleMeta(ActionEvent event) {
        onToggleWidget(meta, wasVisible -> toggleMeta.setText(wasVisible ? "Meta Explorer" : "Close Meta Explorer"));
    }

    private void onToggleWidget(JDialog widget, Consumer<Boolean> consumer) {
        boolean wasVisible = widget.isVisible();
        if (!wasVisible) {
            Dimension location = GTk.frameLocation(widget.getSize());
            widget.setLocation(location.width, location.height);
        }
        widget.setVisible(!wasVisible);
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
                    onToggleAssignedConn(null);
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
                onToggleAssignedConn(null);
            }
        }
    }

    private void dispatchEvent(EventProducer<?> source, Enum<?> event, Object data) {
        if (source instanceof QuestsPanel) {
            GTk.invokeLater(() -> onCommandEvent(EventProducer.eventType(event), (SQLExecutionRequest) data));
        } else if (source instanceof SQLExecutor) {
            GTk.invokeLater(() -> onSQLExecutorEvent(EventProducer.eventType(event), (SQLExecutionResponse) data));
        } else if (source instanceof Conns) {
            GTk.invokeLater(() -> onConnsEvent(EventProducer.eventType(event), data));
        } else if (source instanceof Meta) {
            GTk.invokeLater(() -> onMetaEvent(EventProducer.eventType(event)));
        }
    }

    private void onCommandEvent(QuestsPanel.EventType event, SQLExecutionRequest req) {
        switch (event) {
            case COMMAND_AVAILABLE -> {
                Conn conn = commands.getConnection();
                if (conn == null || !conn.isValid()) {
                    onToggleAssignedConn(null);
                }
                results.close();
                executor.submit(req, this::dispatchEvent);
            }
            case COMMAND_CANCEL -> {
                executor.cancelExistingRequest(req);
                onToggleAssignedConn(null);
            }
            case CONNECTION_STATUS_CLICKED -> onToggleConns(null);
        }
    }

    private void onSQLExecutorEvent(SQLExecutor.EventType event, SQLExecutionResponse res) {
        results.updateStats(event.name(), res);
        switch (event) {
            case STARTED -> results.showInfiniteSpinner();
            case RESULTS_AVAILABLE, COMPLETED -> results.onRowsAdded(res);
            case CANCELLED -> results.close();
            case FAILURE -> {
                results.close();
                results.displayError(res.getError());
            }
        }
    }

    private void onMetaEvent(Meta.EventType event) {
        if (event == Meta.EventType.HIDE_REQUEST) {
            onToggleMeta(null);
        }
    }

    private void onConnsEvent(Conns.EventType event, Object data) {
        switch (event) {
            case CONNECTION_SELECTED -> {
                commands.setConnection((Conn) data);
                if (conns.isVisible()) {
                    onToggleConns(null);
                }
            }
            case CONNECTION_ESTABLISHED, CONNECTION_CLOSED -> {
                Conn conn = (Conn) data;
                Conn current = commands.getConnection();
                if (current != null && current.equals(conn)) {
                    commands.setConnection(conn);
                    toggleAssignedConn.setText(conn.isOpen() ? "Disconnect" : "Connect");
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
            case CONNECTION_FAILED ->
                    results.displayError(String.format("Server is down: %s%n", data));
            case HIDE_REQUEST -> onToggleConns(null);
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
