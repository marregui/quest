package io.crate.cli.gui.common;

import javax.swing.table.AbstractTableModel;
import java.util.*;
import java.util.function.BiFunction;


public class ObjectTableModel<RowType extends HasKey> extends AbstractTableModel {

    private String[] attributeNames;
    private final BiFunction<RowType, String, Object> attributeGetter;
    private final TriFunction<RowType, String, Object, Object> attributeSetter;
    private final List<RowType> rows;


    public ObjectTableModel(String[] attributeNames,
                            BiFunction<RowType, String, Object> attributeGetter,
                            TriFunction<RowType, String, Object, Object> attributeSetter) {
        this.attributeNames = attributeNames;
        this.attributeGetter = attributeGetter;
        this.attributeSetter = attributeSetter;
        rows = new ArrayList<>();
    }

    public void reset(String[] attributeNames) {
        this.attributeNames = attributeNames;
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
        synchronized (rows) {
            rows.addAll(newRows);
        }
        fireTableDataChanged();
    }

    public void addRow(RowType row) {
        if (null == row) {
            return;
        }
        int offset;
        synchronized (rows) {
            rows.add(row);
            offset = rows.size() - 1;
        }
        fireTableRowsInserted(offset, offset);
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
        return attributeNames.length;
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
        return getValueAt(attributeNames, colIdx);
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
}
