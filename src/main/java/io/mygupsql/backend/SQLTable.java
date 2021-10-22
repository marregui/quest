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

package io.mygupsql.backend;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import io.mygupsql.WithKey;


/**
 * A table is identified by a key, has column metadata (names and types) and a
 * list of {@link Row}s in insertion order. Rows are identified by both their
 * key and the row's values. A table cannot contain duplicate rows. This data
 * structure is thread-safe.
 */
public class SQLTable implements WithKey {

    /**
     * A read only row within a {@link SQLTable}.
     */
    public static class Row implements WithKey {

        private final SQLTable parent;
        private final String rowKey;
        private final Object[] values;
        private final AtomicReference<String> toString;

        Row(SQLTable parent, String key, Object[] values) {
            this.parent = parent;
            this.rowKey = key;
            this.values = values;
            this.toString = new AtomicReference<>();
        }

        @Override
        public String getKey() {
            return rowKey;
        }

        /**
         * Every row is a child of a parent table.
         * 
         * @return the reference to the table the row belongs to
         */
        public SQLTable getParent() {
            return parent;
        }

        /**
         * @param colIdx the index of the column
         * @return the row's value at the specified column
         */
        public Object getValueAt(int colIdx) {
            return values[colIdx];
        }

        /**
         * @return in effect the number of columns
         */
        public int size() {
            return parent.colNameToColIdx.size();
        }

        /**
         * Based on row key and values.
         */
        @Override
        public int hashCode() {
            int result = 17 + ((rowKey == null) ? 0 : rowKey.hashCode());
            result = 17 * result + Arrays.deepHashCode(values);
            return result;
        }

        /**
         * Based on row key and values.
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (false == o instanceof Row) {
                return false;
            }
            Row that = (Row) o;
            if (rowKey == null) {
                if (that.rowKey != null) {
                    return false;
                }
            }
            else if (!rowKey.equals(that.rowKey)) {
                return false;
            }
            return Arrays.deepEquals(values, that.values);
        }

        /**
         * The row is immutable, so the first time this method is called, a string
         * representation of the instance is built and returned. Subsequent calls will
         * return the already built string.
         */
        @Override
        public String toString() {
            String str = toString.get();
            if (str == null) {
                synchronized (toString) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("key: ").append(rowKey).append(", values: ");
                    if (null != values) {
                        for (Object o : values) {
                            sb.append(null != o ? o : "null").append(", ");
                        }
                        if (values.length > 0) {
                            sb.setLength(sb.length() - 2);
                        }
                    }
                    str = sb.toString();
                    if (!toString.compareAndSet(null, str)) {
                        str = toString.get();
                    }
                }
            }
            return str;
        }
    }

    private final ReadLock readLock;
    private final WriteLock writeLock;
    private final String key;
    private volatile String[] colNames;
    private volatile int[] colTypes;
    private final List<Row> rows;
    private final ConcurrentMap<String, Integer> rowKeyToIdx;
    private final ConcurrentMap<String, Integer> colNameToColIdx;

    /**
     * Constructor.
     * 
     * @param key usually the key of the request
     */
    public SQLTable(String key) {
        this.key = key;
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        readLock = lock.readLock();
        writeLock = lock.writeLock();
        rows = new ArrayList<>();
        rowKeyToIdx = new ConcurrentHashMap<>();
        colNameToColIdx = new ConcurrentHashMap<>();
    }

    /**
     * The table's key, usually the {@link SQLExecRequest}'s key.
     */
    @Override
    public String getKey() {
        return key;
    }

    /**
     * @return the number of columns, or 0 if column metadata (names, types) have
     *         not been set
     */
    public int getColCount() {
        int[] types = colTypes;
        return types != null ? types.length : 0;
    }

    /**
     * @return the name of the column at position i, or null if column metadata
     *         (names, types) have not been set or the index is out of bounds
     * @param i index of the column
     */
    public String getColName(int i) {
        String[] names = colNames;
        return names != null && i >= 0 && i < names.length ? names[i] : null;
    }

    /**
     * @return the designated SQL type of the column at position i, or
     *         Integer.MAX_VALUE if column metadata (names, types) have not been set
     *         or the index is out of bounds
     * @param i index of the column
     * @see java.sql.Types
     */
    public int getColType(int i) {
        int[] types = colTypes;
        return types != null && i >= 0 && i < types.length ? types[i] : Integer.MAX_VALUE;
    }

    /**
     * @return the column names, or null if column metadata (names, types) have not
     *         been set
     */
    public String[] getColNames() {
        return colNames;
    }

    /**
     * @return the designated column's SQL types, or null if column metadata (names,
     *         types) have not been set
     * @see java.sql.Types
     */
    public int[] getColTypes() {
        return colTypes;
    }

    /**
     * @return true if column metadata (names, types) have been set
     */
    public boolean hasColMetadata() {
        return !colNameToColIdx.isEmpty();
    }

    /**
     * Sets the column metadata (names and types) as defined by the result-set's
     * metadata, and clears the table's rows. It does not change the table's key.
     * <p>
     * This call needs to happen before {@link SQLTable#addRow(String, ResultSet)}
     * can be called.
     * 
     * @param rs result-set in response to a SQL execution request
     * @throws SQLException could not access the result-set's metadata
     */
    public void setColMetadata(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int colCount = metaData.getColumnCount();
        if (colCount <= 0) {
            throw new IllegalArgumentException("no column metadata (names, types) were found");
        }
        String[] names = new String[colCount];
        int[] types = new int[colCount];
        Map<String, Integer> nameToIdx = new HashMap<>();
        for (int i = 0; i < colCount; i++) {
            names[i] = metaData.getColumnName(i + 1);
            types[i] = metaData.getColumnType(i + 1);
            nameToIdx.put(names[i], i);
        }
        writeLock.lock();
        try {
            colNames = names;
            colTypes = types;
            colNameToColIdx.clear();
            colNameToColIdx.putAll(nameToIdx);
            lockFreeClearRows();
        }
        finally {
            writeLock.unlock();
        }
    }

    /**
     * Table rows are added by this method. The row must not exist in the table,
     * which is checked by method {@link SQLTable#containsRow(Row)}.
     * <p>
     * A call to {@link SQLTable#setColMetadata(ResultSet)} needs to happen before
     * rows can be added to the table through this method.
     * 
     * @param rowKey the key for the row, usually a monotonic-incremental number
     * @param rs     result-set in response to a SQL execution request
     * @throws SQLException could not access the result-set's data as defined by the
     *                      metadata
     */
    public void addRow(String rowKey, ResultSet rs) throws SQLException {
        int[] types = colTypes;
        if (types == null) {
            throw new IllegalArgumentException("column metadata (names, types) not defined");
        }
        Object[] values = new Object[types.length];
        for (int i = 0; i < types.length; i++) {
            values[i] = rs.getObject(i + 1);
        }
        Row row = new Row(this, rowKey, values);
        if (!containsRow(row)) {
            writeLock.lock();
            try {
                rows.add(row);
                rowKeyToIdx.put(row.getKey(), rows.size() - 1);
            }
            finally {
                writeLock.unlock();
            }
        }
    }

    /**
     * @return true if the table has only one row, with one column of type VARCHAR
     */
    public boolean isSingleRowSingleVarcharCol() {
        return size() == 1 && getColCount() == 1 && getColType(0) == Types.VARCHAR;
    }

    /**
     * @return number of rows
     */
    public int size() {
        readLock.lock();
        try {
            return rows.size();
        }
        finally {
            readLock.unlock();
        }
    }

    /**
     * Clears the table's column metadata (names, types) and rows.
     */
    public void clear() {
        writeLock.lock();
        try {
            colNames = null;
            colTypes = null;
            colNameToColIdx.clear();
            lockFreeClearRows();
        }
        finally {
            writeLock.unlock();
        }
    }

    private void lockFreeClearRows() {
        rows.forEach(row -> {
            Arrays.fill(row.values, null);
            row.toString.set(null);
        });
        rows.clear();
        rowKeyToIdx.clear();
    }

    /**
     * @param rowKey row key
     * @return the index (array offset) of the row within the table, or -1 rowKey is
     *         null or column metadata (names, types) have not been set, or the
     *         rowKey is not found in the table
     */
    public int getRowIdx(String rowKey) {
        if (rowKey == null) {
            return -1;
        }
        Integer i = rowKeyToIdx.get(rowKey);
        return i != null ? i.intValue() : -1;
    }

    /**
     * The row is in the table if there is a row matching the key and the values.
     * 
     * @param row the row to check
     * @return true if the row is in the table
     */
    public boolean containsRow(Row row) {
        if (row != null) {
            int idx = getRowIdx(row.getKey());
            if (idx != -1) {
                Row internalRow = getRow(idx);
                return internalRow != null && internalRow.equals(row);
            }
        }
        return false;
    }

    /**
     * @param idx index of the row within the table
     * @return the row at offset i
     */
    public Row getRow(int idx) {
        readLock.lock();
        try {
            return rows.get(idx);
        }
        finally {
            readLock.unlock();
        }
    }

    /**
     * @param rowIdx index of the row within the table
     * @param colIdx index of the column within the row
     * @return the value in the table's cell at rowIdx, colIdx
     */
    public Object getValueAt(int rowIdx, int colIdx) {
        Row row = getRow(rowIdx);
        return row != null ? row.getValueAt(colIdx) : null;
    }
}
