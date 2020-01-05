package io.crate.cli.gui;

import java.awt.*;
import java.util.*;
import java.util.List;

import io.crate.cli.connections.SQLConnection;
import io.crate.cli.connections.SQLExecution;
import io.crate.cli.connections.SQLExecutor;
import io.crate.cli.gui.widgets.CommandManager;
import io.crate.cli.gui.common.*;
import io.crate.cli.gui.widgets.SQLConnectionManager;

import javax.swing.*;
import javax.swing.event.TableModelEvent;


public class CratedbSQL {

    public static final String VERSION = "1.0.0";


    private final SQLExecutor sqlExecutor;
    private final SQLConnectionManager sqlConnectionManager;
    private final CommandManager commandManager;
    private final JTable table;
    private final ObjectTableModel<DefaultRowType> tableModel;
    private final JFrame frame;


    private CratedbSQL() {
        sqlExecutor = new SQLExecutor(this::onSourceEvent);
        sqlExecutor.start();
        commandManager = new CommandManager(this::onSourceEvent);
        sqlConnectionManager = new SQLConnectionManager(this::onSourceEvent);
        sqlConnectionManager.start();
        tableModel = new ObjectTableModel<>(new String[]{}, new Class<?>[]{}, Map::get, Map::put) {
            @Override
            public boolean isCellEditable(int rowIdx, int colIdx) {
                return false;
            }
        };
        tableModel.addTableModelListener(this::onTableModelEvent);
        table = GUIFactory.newTable(tableModel, null);
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(sqlConnectionManager, BorderLayout.NORTH);
        topPanel.add(commandManager, BorderLayout.CENTER);
        JScrollPane centerPane = new JScrollPane(table);
        centerPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        centerPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, false, topPanel, centerPane);
        JPanel mainManel = new JPanel(new BorderLayout());
        mainManel.add(splitPane, BorderLayout.CENTER);
        frame = GUIFactory.newFrame(String.format(
                Locale.ENGLISH,
                "CratedbSQL %s [store: %s]",
                VERSION,
                sqlConnectionManager.getStorePath().getAbsolutePath()),
                90, 90,
                mainManel);
    }


    private void onTableModelEvent(TableModelEvent event) {
        System.out.println(event);
    }

    private void onSourceEvent(Object source, Enum eventType, Object eventData) {
        if (source instanceof SQLConnectionManager) {
            SQLConnection conn = (SQLConnection) eventData;
            switch (SQLConnectionManager.EventType.valueOf(eventType.name())) {
                case CONNECTION_SELECTED:
                    commandManager.setSQLConnection(conn);
                    break;

                case CONNECTION_ESTABLISHED:
                case CONNECTION_CLOSED:
                    SQLConnection current = commandManager.getSQLConnection();
                    if (null != current && current.equals(conn)) {
                        commandManager.setSQLConnection(conn);
                    }
                    break;

                case CONNECTIONS_LOST:
                    // TODO
                    break;

                case REPAINT_REQUIRED:
                    frame.validate();
                    frame.repaint();
                    break;
            }
        } else if (source instanceof CommandManager) {
            switch (CommandManager.EventType.valueOf(eventType.name())) {
                case COMMAND_AVAILABLE:
                    String command = (String) eventData;
                    sqlExecutor.submit(
                            commandManager.getKey(),
                            commandManager.getSQLConnection(),
                            command);
                    break;

                case BUFFER_CHANGE:
                    SQLConnection conn = commandManager.getSQLConnection();
                    if (null != conn) {
                        sqlConnectionManager.setSelectedItem(conn);
                    }
                    break;
            }
        } else if (source instanceof SQLExecutor) {
            switch (SQLExecutor.EventType.valueOf(eventType.name())) {
                case RESULTS_AVAILABLE:
                    SQLExecution data = (SQLExecution) eventData;
                    List<DefaultRowType> results = data.getResults();
                    if (false == results.isEmpty()) {
                        DefaultRowType firstRow = results.get(0);
                        String[] columnNames = firstRow.keySet().toArray(new String[0]);
                        Class<?>[] columnTypes = new Class<?>[results.size()];
                        Arrays.fill(columnTypes, String.class);
                        try {
                            SwingUtilities.invokeAndWait(() -> {
                                tableModel.reset(columnNames, columnTypes, Map::get, Map::put);
                                tableModel.setRows(results);
                            });
                        } catch (Throwable throwable) {
                            throwable.printStackTrace();
                        }
                    }
                    break;
            }
        }
    }

    private void shutdown() {
        sqlExecutor.close();
        sqlConnectionManager.close();
    }

    private void setVisible(boolean isVisible) {
        frame.setVisible(isVisible);
    }

    public static void main(String [] args) {
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
