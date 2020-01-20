package io.crate.cli.common;

import io.crate.cli.backend.SQLTable.SQLTableRow;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Locale;


public class StringCellRenderer extends DefaultTableCellRenderer {

    private static final Color BG_ROW_COLOR = new Color(230, 236, 255);
    private static final Color BG_ROW_COLOR_ALTERNATE = new Color(255, 247, 255);

    protected final Color bgColor;
    protected final Color bgColorAlternate;
    protected final Font font;


    public StringCellRenderer() {
        this(GUIToolkit.TABLE_CELL_FONT, BG_ROW_COLOR, BG_ROW_COLOR_ALTERNATE);
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
        ObjectTableModel<SQLTableRow> tableModel = (ObjectTableModel<SQLTableRow>) table.getModel();
        if (rowIdx >= 0 && rowIdx < tableModel.getRowCount()) {
            if (isSelected) {
                setFont(GUIToolkit.TABLE_CELL_FONT);
                setForeground(Color.BLACK);
                setBackground(GUIToolkit.CRATE_COLOR);
            } else {
                setFont(font);
                setForeground(Color.BLACK);
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
