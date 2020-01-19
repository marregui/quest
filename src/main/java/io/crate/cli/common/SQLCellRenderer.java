package io.crate.cli.common;

import io.crate.cli.backend.SQLTable;

import javax.swing.*;
import java.awt.*;


public class SQLCellRenderer extends StringCellRenderer {

    private final SQLTable table;


    public SQLCellRenderer(SQLTable table) {
        super(GUIToolkit.TABLE_CELL_FONT, Color.BLACK, Color.BLACK);
        this.table = table;
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
                int [] columnTypes = this.table.getColumnTypes();
                if (null != columnTypes) {
                    setForeground(SqlType.resolveColor(columnTypes[colIdx]));
                }
            }
        }
        return this;
    }
}
