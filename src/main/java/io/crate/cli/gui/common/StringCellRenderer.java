package io.crate.cli.gui.common;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.util.Locale;


public class StringCellRenderer extends DefaultTableCellRenderer {

    private static final Color DEFAULT_BG_COLOR = new Color(230, 236, 255);
    private static final Color DEFAULT_BG_COLOR_ALTERNATE = new Color(255, 247, 255);
    private static final Font DEFAULT_FONT = new Font("monospaced", Font.PLAIN, 14);

    private static final Color SELECTED_BG_COLOR = new Color(102, 140, 255);
    private static final Color SELECTED_FG_COLOR = Color.WHITE;
    private static final Font SELECTED_FONT = new Font("monospaced", Font.BOLD, 14);


    private final Color bgColor;
    private final Color bgColorAlternate;
    private final Font font;


    public StringCellRenderer() {
        this(DEFAULT_FONT, DEFAULT_BG_COLOR, DEFAULT_BG_COLOR_ALTERNATE);
    }

    public StringCellRenderer(Font font) {
        this(font, DEFAULT_BG_COLOR, DEFAULT_BG_COLOR_ALTERNATE);
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
        if (rowIdx >= 0 && rowIdx < table.getModel().getRowCount()) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIdx, colIdx);
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
