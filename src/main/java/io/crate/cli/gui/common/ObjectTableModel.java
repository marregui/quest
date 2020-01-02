package io.crate.cli.gui.common;

import javax.swing.table.AbstractTableModel;
import java.util.*;
import java.util.function.BiFunction;


public class ObjectTableModel<RowType extends HasKey> extends AbstractTableModel {

    private final List<RowType> rows;
    private String[] attributeNames;
    private Class<?>[] attributeClasses;
    private BiFunction<RowType, String, Object> attributeGetter;
    private TriFunction<RowType, String, Object, Object> attributeSetter;


    public ObjectTableModel(String[] attributeNames,
                            Class<?>[] attributeClasses,
                            BiFunction<RowType, String, Object> attributeGetter,
                            TriFunction<RowType, String, Object, Object> attributeSetter) {
        if (attributeNames.length != attributeClasses.length) {
            throw new IllegalArgumentException("lengths of attributeNames and attributeClasses don't match");
        }
        this.attributeNames = attributeNames;
        this.attributeClasses = attributeClasses;
        this.attributeGetter = attributeGetter;
        this.attributeSetter = attributeSetter;
        rows = new ArrayList<>();
    }

    public void reset(String[] attributeNames,
                      Class<?>[] attributeClasses,
                      BiFunction<RowType, String, Object> attributeGetter,
                      TriFunction<RowType, String, Object, Object> attributeSetter) {
        this.attributeNames = attributeNames;
        this.attributeClasses = attributeClasses;
        this.attributeGetter = attributeGetter;
        this.attributeSetter = attributeSetter;
        fireTableStructureChanged();
        clear();
    }

    public int getRowIdx(String key) {
        if (null == key) {
            return -1;
        }
        for (int idx=0; idx < rows.size(); idx++) {
            RowType row = rows.get(idx);
            if (key.equals(row.getKey())) {
                return idx;
            }
        }
        return -1;
    }

    public void clear() {
        rows.clear();
        fireTableDataChanged();
    }

    public List<RowType> getRows() {
        return rows;
    }

    public void setRows(List<RowType> newRows) {
        rows.clear();
        rows.addAll(newRows);
        fireTableDataChanged();
    }

    public void addRow(RowType row) {
        if (null == row) {
            return;
        }
        rows.add(row);
        fireTableRowsInserted(rows.size() - 1, rows.size() - 1);
    }

    public RowType removeRow(int rowIdx) {
        checkBounds("removeRow", rowIdx, rows.size());
        RowType row = rows.remove(rowIdx);
        fireTableRowsDeleted(rowIdx, rowIdx);
        return row;
    }

    @Override
    public int getRowCount() {
        return rows.size();
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
        return getValueAt(attributeClasses, colIdx);
    }

    @Override
    public Object getValueAt(int rowIdx, int colIdx) {
        checkBounds("getValueAt", rowIdx, rows.size());
        if (-1 == colIdx) {
            return rows.get(rowIdx);
        }
        RowType row = rows.get(rowIdx);
        String attributeName = getValueAt(attributeNames, colIdx);
        return attributeGetter.apply(row, attributeName);
    }

    @Override
    public void setValueAt(Object value, int rowIdx, int colIdx) {
        checkBounds("setValueAt", rowIdx, rows.size());
        String attributeName = getValueAt(attributeNames, colIdx);
        attributeSetter.apply(rows.get(rowIdx), attributeName, value);
        fireTableCellUpdated(rowIdx, colIdx);
    }

    @Override
    public boolean isCellEditable(int rowIdx, int colIdx) {
        return true;
    }
}
