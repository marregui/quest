package io.crate.cli.common;

import io.crate.cli.backend.SQLRowType;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Locale;


public class StringCellRenderer extends DefaultTableCellRenderer {

    private static final Color BG_ROW_COLOR = new Color(230, 236, 255);
    private static final Color BG_ROW_COLOR_ALTERNATE = new Color(255, 247, 255);
    private static final Color SELECTED_BG_COLOR = GUIToolkit.CRATE_COLOR;
    private static final Color SELECTED_FG_COLOR = Color.BLACK;
    private static final Font SELECTED_FONT = GUIToolkit.TABLE_CELL_FONT; //new Font(GUIToolkit.MAIN_FONT_NAME, Font.BOLD, 16);

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
        ObjectTableModel<SQLRowType> tableModel = (ObjectTableModel<SQLRowType>) table.getModel();
        if (rowIdx >= 0 && rowIdx < tableModel.getRowCount()) {
            if (isSelected) {
                setFont(SELECTED_FONT);
                setForeground(SELECTED_FG_COLOR);
                setBackground(SELECTED_BG_COLOR);
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
