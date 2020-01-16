package io.crate.cli.common;

import io.crate.cli.backend.SQLRowType;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Locale;


public class StringCellRenderer extends DefaultTableCellRenderer {

    private final Color bgColor;
    private final Color bgColorAlternate;
    private final Font font;


    public StringCellRenderer() {
        this(GUIToolkit.TABLE_CELL_FONT,
                GUIToolkit.TABLE_BG_ROW_COLOR,
                GUIToolkit.TABLE_BG_ROW_COLOR_ALTERNATE);
    }

    public StringCellRenderer(Font font, Color bgColor, Color bgColorAlternate) {
        this.font = font;
        this.bgColor = bgColor;
        this.bgColorAlternate = bgColorAlternate;
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
                setForeground(GUIToolkit.SELECTED_FG_COLOR);
                setBackground(GUIToolkit.SELECTED_BG_COLOR);
            } else {
                setFont(font);
                Color foreground = Color.BLACK;
                if (colIdx >= 0) {
                    Object maybeSqlRow = tableModel.getValueAt(rowIdx, -1);
                    if (maybeSqlRow instanceof SQLRowType) {
                        SQLRowType row = (SQLRowType) maybeSqlRow;
                        int[] colTypes = row.getColumnTypes();
                        if (null != colTypes) {
                            foreground = GUIToolkit.resolveColorForSqlType(colTypes[colIdx]);
                        }
                    }
                }
                setForeground(foreground);
                setBackground(0 == rowIdx % 2 ? bgColor : bgColorAlternate);
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
