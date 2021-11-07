/* **
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) 2019 - 2022, Miguel Arregui a.k.a. marregui
 */

package io.quest.frontend;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;

import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;


/**
 * Renders rows in alternative colours.
 */
public class CellRenderer extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;
    private static final Color BG_ROW_COLOR = new Color(230, 236, 255);
    private static final Color BG_ROW_COLOR_ALTERNATE = new Color(255, 247, 255);

    private final Color bgColor;
    private final Color bgColorAlternate;
    private final Font font;

    public CellRenderer() {
        this(GTk.TABLE_CELL_FONT, BG_ROW_COLOR, BG_ROW_COLOR_ALTERNATE);
    }

    protected CellRenderer(Font font, Color bgColor, Color bgColorAlternate) {
        this.font = font;
        this.bgColor = bgColor;
        this.bgColorAlternate = bgColorAlternate;
    }

    @Override
    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int rowIdx,
            int colIdx
    ) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIdx, colIdx);
        if (rowIdx >= 0 && rowIdx < table.getModel().getRowCount()) {
            if (isSelected) {
                setFont(GTk.TABLE_CELL_FONT);
                setForeground(Color.WHITE);
                setBackground(GTk.APP_THEME_COLOR);
            } else {
                setFont(font);
                setForeground(Color.BLACK);
                setBackground(0 == rowIdx % 2 ? bgColor : bgColorAlternate);
            }
            return this;
        }
        throw new IndexOutOfBoundsException(
                String.format("row %d does not exist, there are [0..%d] rows", rowIdx, table.getModel().getRowCount() - 1));
    }
}
