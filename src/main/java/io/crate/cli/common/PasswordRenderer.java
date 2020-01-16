package io.crate.cli.common;

import javax.swing.*;
import java.awt.*;

public class PasswordRenderer extends StringCellRenderer {

    private static final String PASSWORD = "*********";

    @Override
    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int rowIdx,
                                                   int colIdx) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIdx, colIdx);
        setValue(PASSWORD);
        return this;
    }
}