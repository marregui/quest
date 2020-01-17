package io.crate.cli.common;

import io.crate.cli.backend.SQLRowType;

import javax.swing.*;
import java.awt.*;
import java.util.Locale;


public class SQLCellRenderer extends StringCellRenderer {

    public SQLCellRenderer() {
        super();
    }

    @Override
    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int rowIdx,
                                                   int colIdx) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIdx, colIdx);
        ObjectTableModel<SQLRowType> tableModel = (ObjectTableModel<SQLRowType>) table.getModel();
        if (rowIdx >= 0 && rowIdx < tableModel.getRowCount()) {
            if (isSelected) {
                setFont(GUIToolkit.TABLE_CELL_SELECTED_FONT);
                setForeground(GUIToolkit.TABLE_CELL_SELECTED_FG_COLOR);
                setBackground(GUIToolkit.TABLE_CELL_SELECTED_BG_COLOR);
            } else {
                setFont(font);
                Color foreground = Color.WHITE;
                if (colIdx >= 0) {
                    Object maybeSqlRow = tableModel.getValueAt(rowIdx, -1);
                    if (maybeSqlRow instanceof SQLRowType) {
                        SQLRowType row = (SQLRowType) maybeSqlRow;
                        int[] colTypes = row.getColumnTypes();
                        if (null != colTypes) {
                            foreground = SqlType.resolveColor(colTypes[colIdx]);
                        }
                    }
                }
                setForeground(foreground);
                setBackground(Color.BLACK);
            }
            return this;
        }
        throw new IndexOutOfBoundsException(String.format(
                Locale.ENGLISH,
                "row %d does not exist, there are [0..%d] rows",
                rowIdx,
                table.getModel().getRowCount() - 1));
    }
}
