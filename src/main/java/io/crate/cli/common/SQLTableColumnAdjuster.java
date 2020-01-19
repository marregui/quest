package io.crate.cli.common;


import java.awt.*;
import java.beans.*;
import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;


public class SQLTableColumnAdjuster implements PropertyChangeListener, TableModelListener {

    private static final int DEFAULT_SPACING = 6;


    private final JTable table;
    private final Map<TableColumn, Integer> columnSizes;


    public SQLTableColumnAdjuster(JTable table) {
        this.table = table;
        columnSizes = new HashMap<>();
    }

    public void adjustColumns() {
        TableColumnModel tcm = table.getColumnModel();
        for (int i = 0; i < tcm.getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            int columnHeaderWidth = getColumnHeaderWidth(i);
            int columnDataWidth = getColumnDataWidth(i);
            int preferredWidth = Math.max(columnHeaderWidth, columnDataWidth);
            updateTableColumn(i, preferredWidth);
        }
    }

    private int getColumnHeaderWidth(int column) {
        TableColumn tableColumn = table.getColumnModel().getColumn(column);
        Object value = tableColumn.getHeaderValue();
        TableCellRenderer renderer = tableColumn.getHeaderRenderer();

        if (renderer == null) {
            renderer = table.getTableHeader().getDefaultRenderer();
        }

        Component c = renderer.getTableCellRendererComponent(table, value, false, false, -1, column);
        return c.getPreferredSize().width;
    }

    /*
     *  Calculate the width based on the widest cell renderer for the
     *  given column.
     */
    private int getColumnDataWidth(int column) {
        int preferredWidth = 0;
        int maxWidth = table.getColumnModel().getColumn(column).getMaxWidth();

        for (int row = 0; row < table.getRowCount(); row++) {
            preferredWidth = Math.max(preferredWidth, getCellDataWidth(row, column));

            //  We've exceeded the maximum width, no need to check other rows

            if (preferredWidth >= maxWidth)
                break;
        }

        return preferredWidth;
    }

    /*
     *  Get the preferred width for the specified cell
     */
    private int getCellDataWidth(int row, int column) {
        //  Inovke the renderer for the cell to calculate the preferred width

        TableCellRenderer cellRenderer = table.getCellRenderer(row, column);
        Component c = table.prepareRenderer(cellRenderer, row, column);
        int width = c.getPreferredSize().width + table.getIntercellSpacing().width;

        return width;
    }

    private void updateTableColumn(int columnIdx, int width) {
        final TableColumn column = table.getColumnModel().getColumn(columnIdx);
        width += DEFAULT_SPACING;
        width = Math.max(width, column.getPreferredWidth());
        columnSizes.put(column, column.getWidth());
        table.getTableHeader().setResizingColumn(column);
        column.setWidth(width);
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
        if ("model".equals(e.getPropertyName())) {
            TableModel model = (TableModel) e.getOldValue();
            model.removeTableModelListener(this);
            model = (TableModel) e.getNewValue();
            model.addTableModelListener(this);
            adjustColumns();
        }
    }

    @Override
    public void tableChanged(TableModelEvent e) {
        GUIToolkit.addToSwingEventQueue(() -> {
            int columnIdx = table.convertColumnIndexToView(e.getColumn());
            if (e.getType() == TableModelEvent.UPDATE && -1 != columnIdx) {
                int row = e.getFirstRow();
                TableColumn column = table.getColumnModel().getColumn(columnIdx);
                if (column.getResizable()) {
                    int width = getCellDataWidth(row, columnIdx);
                    updateTableColumn(columnIdx, width);
                }
            } else {
                adjustColumns();
            }
        });
    }
}