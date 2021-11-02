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

package io.mygupsql.frontend.results;

import java.util.Objects;
import java.util.function.Supplier;

import javax.swing.table.AbstractTableModel;

import io.mygupsql.backend.SQLExecutor;
import io.mygupsql.backend.SQLTable;


/**
 * Adds paging to a default {@link javax.swing.table.TableModel} wrapping a
 * {@link SQLTable}. Column metadata are accessed through a table supplier. The
 * table is built by a {@link SQLExecutor} and thus it will be null until then
 * SQL query execution is started.
 */
class PagedSQLTableModel extends AbstractTableModel {

    private static final long serialVersionUID = 1L;
    private static final int PAGE_SIZE = 1000; // number of rows

    private final Supplier<SQLTable> tableSupplier;
    private int currentPage;
    private int maxPage;
    private int pageStartOffset;
    private int pageEndOffset;

    /**
     * Constructor.
     * 
     * @param tableSupplier provides access to the table, which may be null
     */
    PagedSQLTableModel(Supplier<SQLTable> tableSupplier) {
        this.tableSupplier = Objects.requireNonNull(tableSupplier);
    }

    /**
     * @return true if can increase the page number
     */
    boolean canIncrPage() {
        return currentPage < maxPage;
    }

    /**
     * @return true if can decrease the page number
     */
    boolean canDecrPage() {
        return currentPage > 0;
    }

    /**
     * Increases current page.
     */
    void incrPage() {
        if (canIncrPage()) {
            currentPage++;
            fireTableDataChanged();
        }
    }

    /**
     * Decreases current page.
     */
    void decrPage() {
        if (canDecrPage()) {
            currentPage--;
            fireTableDataChanged();
        }
    }

    @Override
    public void fireTableStructureChanged() {
        super.fireTableStructureChanged();
        fireTableDataChanged();
    }

    @Override
    public void fireTableDataChanged() {
        SQLTable table = tableSupplier.get();
        if (table != null) {
            int size = table.size();
            pageStartOffset = PAGE_SIZE * currentPage;
            pageEndOffset = pageStartOffset + Math.min(size, PAGE_SIZE);
            maxPage = (size / PAGE_SIZE) - 1;
            if (size % PAGE_SIZE > 0) {
                maxPage++;
            }
        }
        else {
            currentPage = 0;
            maxPage = 0;
            pageStartOffset = 0;
            pageEndOffset = 0;
        }
        super.fireTableDataChanged();
    }

    /**
     * @return the page's start offset within the table. Offsets start at 0.
     */
    public int getPageStartOffset() {
        return pageStartOffset;
    }

    /**
     * @return the page's end offset, excluded, within the table
     */
    public int getPageEndOffset() {
        return pageEndOffset;
    }

    /**
     * @return the number of rows in the current page
     */
    @Override
    public int getRowCount() {
        SQLTable table = tableSupplier.get();
        return table != null ? pageEndOffset - pageStartOffset : 0;
    }

    /**
     * @return the total size of the table, not just the number of rows in a page
     */
    public int getTableSize() {
        SQLTable table = tableSupplier.get();
        return table != null ? table.size() : 0;
    }

    @Override
    public int getColumnCount() {
        SQLTable table = tableSupplier.get();
        return table != null ? table.getColCount() : 0;
    }

    @Override
    public String getColumnName(int colIdx) {
        SQLTable table = tableSupplier.get();
        if (table == null) {
            return "";
        }
        return String.format("%s [%s]", table.getColName(colIdx), SQLType.resolveName(table.getColType(colIdx)));
    }

    @Override
    public Class<?> getColumnClass(int colIdx) {
        return String.class;
    }

    @Override
    public Object getValueAt(int rowIdx, int colIdx) {
        SQLTable table = tableSupplier.get();
        if (table == null) {
            return "";
        }
        int idx = pageStartOffset + rowIdx;
        if (idx < table.size()) {
            SQLTable.Row row = table.getRow(idx);
            return colIdx == -1 ? row : row.getValueAt(colIdx);
        }
        return null;
    }

    @Override
    public boolean isCellEditable(int rowIdx, int colIdx) {
        return false;
    }
}
