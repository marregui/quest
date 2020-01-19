package io.crate.cli.common;

import io.crate.cli.backend.SQLRowType;

import javax.swing.*;
import java.awt.*;


public class SQLCellRenderer extends StringCellRenderer {

    public SQLCellRenderer() {
        super(GUIToolkit.TABLE_CELL_FONT, Color.BLACK, Color.BLACK);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int rowIdx,
                                                   int colIdx) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIdx, colIdx);
        if (rowIdx >= 0 && rowIdx < table.getModel().getRowCount()) {
            if (colIdx >= 0) {
                Object maybeSqlRow = table.getModel().getValueAt(rowIdx, -1);
                if (maybeSqlRow instanceof SQLRowType) {
                    SQLRowType row = (SQLRowType) maybeSqlRow;
                    int[] colTypes = row.getColumnTypes();
                    if (null != colTypes) {
                        setForeground(SqlType.resolveColor(colTypes[colIdx]));
                    }
                }
            }
        }
        return this;
    }
}
