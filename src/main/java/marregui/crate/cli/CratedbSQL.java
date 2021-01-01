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
 * Copyright 2020, Miguel Arregui a.k.a. marregui
 */

package marregui.crate.cli;

import static marregui.crate.cli.GUITk.invokeLater;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.Closeable;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSplitPane;
import javax.swing.KeyStroke;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import marregui.crate.cli.backend.DBConn;
import marregui.crate.cli.backend.SQLExecRequest;
import marregui.crate.cli.backend.SQLExecResponse;
import marregui.crate.cli.backend.SQLExecutor;
import marregui.crate.cli.widgets.command.CommandBoard;
import marregui.crate.cli.widgets.conns.ConnectionsManager;
import marregui.crate.cli.widgets.results.SQLResultsTable;


/**
 * Application's Main.
 */
public class CratedbSQL implements Closeable {

    private static final Logger LOGGER = LoggerFactory.getLogger(CratedbSQL.class);

    private static final String VERSION = "2.0.0";
    private static final String BANNER = "\n" // https://patorjk.com/software/taag/#p=display&h=2&f=Ivrit&t=CratedbSQL
        + "   ____           _           _ _    ____   ___  _     \n"
        + "  / ___|_ __ __ _| |_ ___  __| | |__/ ___| / _ \\| |    \n"
        + " | |   | '__/ _` | __/ _ \\/ _` | '_ \\___ \\| | | | |    \n"
        + " | |___| | | (_| | ||  __/ (_| | |_) |__) | |_| | |___ \n"
        + "  \\____|_|  \\__,_|\\__\\___|\\__,_|_.__/____/ \\__\\_\\_____|" + "\n";

    private final ConnectionsManager conns;
    private final SQLExecutor executor;
    private final CommandBoard commands;
    private final SQLResultsTable results;
    private final JMenuItem toggleConnsWidget;
    private final JMenuItem toggleConn;

    private CratedbSQL() {
        LOGGER.info(BANNER);
        LOGGER.info("{} version {}", getClass().getSimpleName(), VERSION);

        JFrame frame = GUITk.createFrame();
        int width = frame.getWidth();
        int dividerHeight = (int) (frame.getHeight() * 0.6);
        conns = new ConnectionsManager(frame, this::dispatchEvent);
        commands = new CommandBoard(this::dispatchEvent);
        commands.setPreferredSize(new Dimension(0, dividerHeight));
        results = new SQLResultsTable(width, dividerHeight);
        frame.setTitle(String.format("%s %s [store: %s]", getClass().getSimpleName(), VERSION, conns.getStorePath()));

        // menu bar
        toggleConnsWidget = new JMenuItem();
        toggleConn = new JMenuItem();
        frame.setJMenuBar(createMenuBar());

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, commands, results);
        splitPane.setDividerLocation(dividerHeight);
        frame.add(splitPane, BorderLayout.CENTER);

        Runtime.getRuntime().addShutdownHook(new Thread(this::close, getClass().getSimpleName() + "-shutdown-hook"));
        conns.start();
        executor = new SQLExecutor();
        executor.start();

        frame.setVisible(true);
    }

    private JMenuBar createMenuBar() {
        Font font = new Font(GUITk.MAIN_FONT_NAME, Font.PLAIN, 14);
        JMenu connsMenu = new JMenu("Connections");
        connsMenu.add(
            configureMenuItem(toggleConnsWidget, font, "Show connections", KeyEvent.VK_T, this::onToggleConnsWidgetEvent));
        connsMenu.add(configureMenuItem(toggleConn, font, "Connect", KeyEvent.VK_O, this::onToggleConnEvent));
        connsMenu.setFont(font);

        JMenu commandsMenu = new JMenu("Commands");
        commandsMenu.add(configureMenuItem(new JMenuItem(), font, "L.Exec", KeyEvent.VK_L, commands::onExecLineEvent));
        commandsMenu.add(configureMenuItem(new JMenuItem(), font, "Exec", KeyEvent.VK_ENTER, commands::onExecEvent));
        commandsMenu.add(configureMenuItem(new JMenuItem(), font, "Cancel", KeyEvent.VK_C, commands::onCancelEvent));
        commandsMenu.setFont(font);

        JMenu resultsMenu = new JMenu("Results");
        resultsMenu.add(configureMenuItem(new JMenuItem(), font, "Prev Page", KeyEvent.VK_B, results::onPrevButtonEvent));
        resultsMenu.add(configureMenuItem(new JMenuItem(), font, "Next Page", KeyEvent.VK_N, results::onNextButtonEvent));
        resultsMenu.setFont(font);

        JMenu menu = new JMenu("Menu");
        menu.setFont(font);
        menu.add(connsMenu);
        menu.add(commandsMenu);
        menu.add(resultsMenu);

        JMenuBar menuBar = new JMenuBar();
        menuBar.setBorder(BorderFactory.createLoweredSoftBevelBorder());
        menuBar.add(menu);
        return menuBar;
    }

    private static JMenuItem configureMenuItem(JMenuItem item, Font font, String title, int keyEvent,
        ActionListener listener) {
        item.setFont(font);
        item.setText(title);
        item.setMnemonic(keyEvent);
        item.setAccelerator(KeyStroke.getKeyStroke(keyEvent, InputEvent.CTRL_DOWN_MASK));
        item.addActionListener(listener);
        return item;
    }

    private void onToggleConnEvent(ActionEvent event) {
        DBConn conn = commands.getConnection();
        conns.onConnectEvent(conn);
        toggleConn.setText(conn != null && conn.isOpen() ? "Connect" : "Disconnect");
    }

    private void onToggleConnsWidgetEvent(ActionEvent event) {
        boolean wasVisible = conns.isVisible();
        conns.setVisible(!wasVisible);
        toggleConnsWidget.setText(wasVisible ? "Show connections" : "Hide connections");
    }

    private void dispatchEvent(EventProducer<?> source, Enum<?> eventType, Object eventData) {
        if (source instanceof CommandBoard) {
            onCommandBoardEvent(eventType(eventType), (SQLExecRequest) eventData);
        }
        else if (source instanceof SQLExecutor) {
            onSQLExecutorEvent(eventType(eventType), (SQLExecResponse) eventData);
        }
        else if (source instanceof ConnectionsManager) {
            onDBConnectionManagerEvent(eventType(eventType), eventData);
        }
    }

    @SuppressWarnings("unchecked")
    private static <EventType extends Enum<?>> EventType eventType(Enum<?> eventType) {
        return (EventType) EventType.valueOf(eventType.getClass(), eventType.name());
    }

    private void onCommandBoardEvent(CommandBoard.EventType event, SQLExecRequest req) {
        switch (event) {
            case COMMAND_AVAILABLE:
                DBConn conn = commands.getConnection();
                if (conn == null || !conn.isValid()) {
                    onToggleConnEvent(null);
                }
                results.close();
                executor.submit(req, this::dispatchEvent);
                break;

            case COMMAND_CANCEL:
                executor.cancelSubmittedRequest(req);
                break;

            case CONNECTION_STATUS_CLICKED:
                onToggleConnsWidgetEvent(null);
                break;
        }
    }

    private void onSQLExecutorEvent(SQLExecutor.EventType event, SQLExecResponse res) {
        invokeLater(() -> {
            results.updateStats(event.name(), res);
        });
        switch (event) {
            case STARTED:
                invokeLater(results::showInfiniteSpinner);
                break;

            case RESULTS_AVAILABLE:
            case COMPLETED:
                invokeLater(() -> {
                    results.onRowsAddedEvent(res);
                });
                break;

            case CANCELLED:
                invokeLater(results::close);
                break;

            case FAILURE:
                invokeLater(results::close, () -> results.displayError(res.getError()));
                break;
        }
    }

    private void onDBConnectionManagerEvent(ConnectionsManager.EventType event, Object eventData) {
        switch (event) {
            case CONNECTION_SELECTED:
                commands.setConnection((DBConn) eventData);
                break;

            case CONNECTION_ESTABLISHED:
            case CONNECTION_CLOSED:
                DBConn conn = (DBConn) eventData;
                DBConn current = commands.getConnection();
                if (current != null && current.equals(conn)) {
                    commands.setConnection(conn);
                    toggleConn.setText(conn.isOpen() ? "Disconnect" : "Connect");
                }
                break;

            case CONNECTIONS_LOST:
                @SuppressWarnings("unchecked")
                Set<DBConn> droppedConns = (Set<DBConn>) eventData;
                current = commands.getConnection();
                if (current != null) {
                    for (DBConn dc : droppedConns) {
                        if (current.equals(dc)) {
                            commands.setConnection(dc);
                        }
                    }
                }
                break;

            case HIDE_REQUEST:
                onToggleConnsWidgetEvent(null);
                break;
        }
    }

    @Override
    public void close() {
        commands.close();
        executor.close();
        conns.close();
        results.close();
    }

    /**
     * Starts the application.
     * 
     * @param args none required
     */
    public static void main(String[] args) {
        invokeLater(CratedbSQL::new);
    }
}
