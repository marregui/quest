package io.crate.cli.gui.widgets;

import io.crate.cli.connections.SQLRowType;
import io.crate.cli.gui.common.GUIFactory;
import io.crate.cli.gui.common.ObjectTableModel;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import java.awt.*;
import java.io.Closeable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;


public class SQLResultsTable extends JPanel implements Closeable {

    private static final int ROWS_PER_PAGE = 100;


    private final JTable windowTable;
    private final ObjectTableModel<SQLRowType> windowTableModel;
    private int currentPage;


    public SQLResultsTable() {
        windowTableModel = new ObjectTableModel<>(new String[]{}, new Class<?>[]{}, Map::get, Map::put) {
            @Override
            public boolean isCellEditable(int rowIdx, int colIdx) {
                return false;
            }
        };
        windowTableModel.addTableModelListener(this::onTableModelEvent);
        windowTable = GUIFactory.newTable(windowTableModel, null);
        currentPage = 0;
        JScrollPane centerPane = new JScrollPane(windowTable);
        centerPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        centerPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        setLayout(new BorderLayout());
        add(centerPane, BorderLayout.CENTER);
    }

    private void onTableModelEvent(TableModelEvent event) {
        System.out.println(event);
    }

    public void addRows(List<SQLRowType> rows) {
        SQLRowType firstRow = rows.get(0);
        String[] columnNames = firstRow.keySet().toArray(new String[0]);
        Class<?>[] columnTypes = new Class<?>[rows.size()];
        Arrays.fill(columnTypes, String.class);
        windowTableModel.reset(columnNames, columnTypes, Map::get, Map::put);
        windowTableModel.setRows(rows);
    }

    @Override
    public void close() {

    }
}
