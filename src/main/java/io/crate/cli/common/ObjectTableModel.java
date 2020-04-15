/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */
package io.crate.cli.common;

import javax.swing.table.AbstractTableModel;
import java.util.*;
import java.util.function.BiFunction;


public class ObjectTableModel<RowType extends HasKey> extends AbstractTableModel {

    private String[] attributeNames;
    private int[] attributeTypes;
    private final BiFunction<RowType, String, Object> attributeGetter;
    private final TriMethod<RowType, String, Object> attributeSetter;
    private final List<RowType> rows;


    public ObjectTableModel(String[] attributeNames,
                            BiFunction<RowType, String, Object> attributeGetter,
                            TriMethod<RowType, String, Object> attributeSetter) {
        this.attributeNames = attributeNames;
        this.attributeGetter = attributeGetter;
        this.attributeSetter = attributeSetter;
        rows = new ArrayList<>();
    }

    public void reset(String[] attributeNames, int[] attributeTypes) {
        this.attributeNames = attributeNames;
        this.attributeTypes = attributeTypes;
        fireTableStructureChanged();
        clear();
    }

    public int getRowIdx(String key) {
        if (null == key) {
            return -1;
        }
        int idx = -1;
        synchronized (rows) {
            for (int i=0; i < rows.size(); i++) {
                RowType row = rows.get(i);
                if (key.equals(row.getKey())) {
                    idx = i;
                    break;
                }
            }
        }
        return idx;
    }

    public boolean contains(RowType item) {
        boolean contains;
        synchronized (rows) {
            contains = rows.contains(item);
        }
        return contains;
    }

    public void clear() {
        synchronized (rows) {
            rows.clear();
        }
        fireTableDataChanged();
    }

    public List<RowType> getRows() {
        List<RowType> view;
        synchronized (rows) {
            view = Collections.unmodifiableList(rows);
        }
        return view;
    }

    public void setRows(List<RowType> newRows) {
        synchronized (rows) {
            rows.clear();
            rows.addAll(newRows);
        }
        fireTableDataChanged();
    }

    public void addRows(List<RowType> newRows) {
        int offset;
        synchronized (rows) {
            offset = rows.size();
            rows.addAll(newRows);
        }
        fireTableRowsInserted(offset, offset + newRows.size() - 1);
    }

    public int addRow(RowType row) {
        if (null == row) {
            return -1;
        }
        int offset;
        synchronized (rows) {
            rows.add(row);
            offset = rows.size() - 1;
        }
        fireTableRowsInserted(offset, offset);
        return offset;
    }

    public RowType removeRow(int rowIdx) {
        RowType row;
        synchronized (rows) {
            checkBounds("removeRow", rowIdx, rows.size());
            row = rows.remove(rowIdx);
        }
        fireTableRowsDeleted(rowIdx, rowIdx);
        return row;
    }

    @Override
    public int getRowCount() {
        int size;
        synchronized (rows) {
            size = rows.size();
        }
        return size;
    }

    @Override
    public int getColumnCount() {
        return null != attributeNames ? attributeNames.length : 0;
    }

    private static void checkBounds(String descriptor, int idx, int maxExcluded) {
        if (idx < 0 || idx >= maxExcluded) {
            throw new IndexOutOfBoundsException(String.format(
                    Locale.ENGLISH,
                    "idx %d not in [0..%d]: %s",
                    idx,
                    maxExcluded - 1,
                    descriptor));
        }
    }

    private static <T> T getValueAt(T[] source, int idx) {
        checkBounds("getValueAt", idx, source.length);
        return source[idx];
    }

    @Override
    public String getColumnName(int colIdx) {
        return String.format(Locale.ENGLISH,
                "%s%s",
                getValueAt(attributeNames, colIdx),
                null == attributeTypes ?
                        "" :
                        " [" + SqlType.resolveName(attributeTypes[colIdx]) + "]");
    }

    @Override
    public Class<?> getColumnClass(int colIdx) {
        return String.class;
    }

    @Override
    public Object getValueAt(int rowIdx, int colIdx) {
        RowType row;
        synchronized (rows) {
            checkBounds("getValueAt", rowIdx, rows.size());
            if (-1 == colIdx) {
                return rows.get(rowIdx);
            }
            row = rows.get(rowIdx);
        }
        String attributeName = getValueAt(attributeNames, colIdx);
        return attributeGetter.apply(row, attributeName);
    }

    @Override
    public void setValueAt(Object value, int rowIdx, int colIdx) {
        String attributeName = getValueAt(attributeNames, colIdx);
        synchronized (rows) {
            checkBounds("setValueAt", rowIdx, rows.size());
            attributeSetter.apply(rows.get(rowIdx), attributeName, value);
        }
        fireTableCellUpdated(rowIdx, colIdx);
    }

    @Override
    public boolean isCellEditable(int rowIdx, int colIdx) {
        return true;
    }

    public RowType getElementAt(int index) {
        return rows.get(index);
    }
}
