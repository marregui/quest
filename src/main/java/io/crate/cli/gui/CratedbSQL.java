package io.crate.cli.gui;

import java.awt.*;
import java.util.*;
import java.util.List;

import io.crate.cli.connections.SQLConnection;
import io.crate.cli.gui.widgets.CommandManager;
import io.crate.cli.gui.common.*;
import io.crate.cli.gui.common.EventListener;
import io.crate.cli.gui.widgets.SQLConnectionManager;

import javax.swing.*;
import javax.swing.event.TableModelEvent;


public class CratedbSQL extends JPanel implements EventListener {

    public static final String VERSION = "1.0.0";


    private final SQLConnectionManager sqlConnectionManager;
    private final CommandManager commandManager;
    private final JTable table;
    private final ObjectTableModel<DefaultRowType> tableModel;


    private CratedbSQL() {
        commandManager = new CommandManager(this);
        sqlConnectionManager = new SQLConnectionManager(this);
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
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        setLayout(new BorderLayout());
        add(topPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }


    private void onTableModelEvent(TableModelEvent event) {
        System.out.println(event);
    }

    @Override
    public void onSourceEvent(Object source, Enum eventType, Object eventData) {
        if (source instanceof SQLConnectionManager) {
            switch (SQLConnectionManager.EventType.valueOf(eventType.name())) {
                case CONNECTION_SELECTED:
                case CONNECTION_ESTABLISHED:
                case CONNECTION_LOST:
                case CONNECTION_CLOSED:
                    commandManager.setSQLConnection((SQLConnection) eventData);
                    break;

                case REPAINT_REQUIRED:
                    validate();
                    repaint();
                    break;
            }
        } else if (source instanceof CommandManager) {
            switch (CommandManager.EventType.valueOf(eventType.name())) {
                case COMMAND_RESULTS:
                    List<DefaultRowType> data = (List<DefaultRowType>) eventData;
                    if (false == data.isEmpty()) {
                        DefaultRowType firstRow = data.get(0);
                        String[] columnNames = firstRow.keySet().toArray(new String[0]);
                        Class<?>[] columnTypes = new Class<?>[data.size()];
                        Arrays.fill(columnTypes, String.class);
                        tableModel.reset(columnNames, columnTypes, Map::get, Map::put);
                        tableModel.setRows(data);
                    }
                    break;

                case BUFFER_CHANGE:
                    SQLConnection conn = commandManager.getSQLConnection();
                    if (null != conn) {
                        sqlConnectionManager.setSelectedItem(conn);
                    }
                    break;
            }
        }
    }

    private void shutdown() {
        sqlConnectionManager.close();
    }

    public static void main(String [] args) {
        CratedbSQL cratedbSql = new CratedbSQL();
        GUIFactory.newFrame(String.format(Locale.ENGLISH, "CratedbSQL %s", VERSION),
                80, 80,
                cratedbSql).setVisible(true);
        Runtime.getRuntime().addShutdownHook(new Thread(
                cratedbSql::shutdown,
                String.format(
                        Locale.ENGLISH,
                        "%s shutdown tasks",
                        CratedbSQL.class.getSimpleName())));
    }
}
