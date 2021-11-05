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

package io.quest.frontend.results;

import java.awt.Color;
import java.awt.Component;
import java.util.function.Supplier;

import javax.swing.JTable;

import io.quest.common.GTk;
import io.quest.backend.SQLExecutor;
import io.quest.backend.SQLTable;
import io.quest.frontend.CellRenderer;


/**
 * Changes the cell background colour as a function of the column's SQL
 * {@link java.sql.Types}. Colours are defined in {@link SQLType}.
 * <p>
 * Column metadata are accessed through a {@link SQLTable} supplier. The table
 * is built by a {@link SQLExecutor} and thus it will be null until then SQL
 * query execution is started.
 * 
 * @see SQLType
 */
class SQLCellRenderer extends CellRenderer {

    private static final long serialVersionUID = 1L;

    private final Supplier<SQLTable> tableSupplier;

    SQLCellRenderer(Supplier<SQLTable> tableSupplier) {
        super(GTk.TABLE_CELL_FONT, Color.BLACK, Color.BLACK);
        this.tableSupplier = tableSupplier;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
        int rowIdx, int colIdx) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, rowIdx, colIdx);
        SQLTable sqlTable = tableSupplier.get();
        if (sqlTable != null && !isSelected && rowIdx >= 0 && rowIdx < table.getModel().getRowCount()) {
            if (colIdx >= 0) {
                int[] columnTypes = sqlTable.getColTypes();
                if (columnTypes != null) {
                    setForeground(SQLType.resolveColor(columnTypes[colIdx]));
                }
            }
        }
        return this;
    }
}
