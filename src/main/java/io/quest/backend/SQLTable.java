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

package io.quest.backend;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import io.quest.common.WithKey;

/**
 * A table is identified by a key, it has column metadata (column names and types) and
 * a list of {@link SQLRow}s in insertion order. Rows are identified by both their key
 * and the row's values. This data structure is thread-safe.
 */
public class SQLTable implements WithKey<String> {
    public static final String ROWID_COL_NAME = "#";

    private final ReadLock readLock;
    private final WriteLock writeLock;
    private final String key;
    private volatile String[] colNames;
    private volatile int[] colTypes;
    private final List<SQLRow> rows;
    private final ConcurrentMap<Long, Integer> rowKeyToIdx;
    private final ConcurrentMap<String, Integer> colNameToColIdx;

    public SQLTable(String key) {
        this.key = key;
        ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        readLock = lock.readLock();
        writeLock = lock.writeLock();
        rows = new ArrayList<>();
        colNameToColIdx = new ConcurrentHashMap<>();
        rowKeyToIdx = new ConcurrentHashMap<>();
    }

    @Override
    public String getKey() {
        return key;
    }

    public int getColCount() {
        int[] types = colTypes;
        return types != null ? types.length : 0;
    }

    public String getColName(int i) {
        String[] names = colNames;
        return names != null && i >= 0 && i < names.length ? names[i] : null;
    }

    public int getColType(int i) {
        int[] types = colTypes;
        return types != null && i >= 0 && i < types.length ? types[i] : Integer.MAX_VALUE;
    }

    public String[] getColNames() {
        return colNames;
    }

    public int[] getColTypes() {
        return colTypes; // java.sql.Types
    }

    public boolean hasColMetadata() {
        return !colNameToColIdx.isEmpty();
    }

    /**
     * Sets the column metadata (names and types) as defined by the result-set's
     * metadata, and clears the table's rows. It does not change the table's key.
     * <p>
     * This call needs to happen before {@link SQLTable#addRow(long, ResultSet)}
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
        String[] names = new String[colCount + 1];
        int[] types = new int[colCount + 1];
        Map<String, Integer> nameToIdx = new HashMap<>();
        names[0] = ROWID_COL_NAME;
        types[0] = Types.ROWID;
        nameToIdx.put(names[0], 0);
        for (int i = 1; i <= colCount; i++) {
            names[i] = metaData.getColumnName(i);
            types[i] = metaData.getColumnType(i);
            nameToIdx.put(names[i], i);
        }
        writeLock.lock();
        try {
            colNames = names;
            colTypes = types;
            colNameToColIdx.clear();
            colNameToColIdx.putAll(nameToIdx);
            lockFreeClearRows();
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Table rows are added by this method. The row must not exist in the table,
     * which is checked by method {@link SQLTable#containsRow(SQLRow)}.
     * <p>
     * A call to {@link SQLTable#setColMetadata(ResultSet)} needs to happen before
     * rows can be added to the table through this method.
     *
     * @param rowKey the key for the row, usually a monotonic-incremental number
     * @param rs     result-set in response to a SQL execution request
     * @throws SQLException could not access the result-set's data as defined by the
     *                      metadata
     */
    public void addRow(long rowKey, ResultSet rs) throws SQLException {
        int[] types = colTypes;
        if (types == null) {
            throw new IllegalArgumentException("column metadata (names, types) not defined");
        }
        Object[] values = new Object[types.length];
        values[0] = rowKey;
        for (int i = 1; i < types.length; i++) {
            values[i] = rs.getObject(i);
        }
        SQLRow row = new SQLRow(this, rowKey, values);
        if (!containsRow(row)) {
            writeLock.lock();
            try {
                rows.add(row);
                rowKeyToIdx.put(row.getKey(), rows.size() - 1);
            } finally {
                writeLock.unlock();
            }
        }
    }

    public boolean isSingleRowSingleVarcharCol() {
        return size() == 1 && getColCount() == 1 && getColType(0) == Types.VARCHAR;
    }

    public int size() {
        readLock.lock();
        try {
            return rows.size();
        } finally {
            readLock.unlock();
        }
    }

    public void clear() {
        writeLock.lock();
        try {
            colNames = null;
            colTypes = null;
            colNameToColIdx.clear();
            lockFreeClearRows();
        } finally {
            writeLock.unlock();
        }
    }

    private void lockFreeClearRows() {
        rows.forEach(SQLRow::clear);
        rows.clear();
        rowKeyToIdx.clear();
    }

    public int getRowIdx(long rowKey) {
        Integer i = rowKeyToIdx.get(rowKey);
        return i != null ? i : -1;
    }

    public boolean containsRow(SQLRow row) {
        if (row != null) {
            int idx = getRowIdx(row.getKey());
            if (idx != -1) {
                SQLRow internalRow = getRow(idx);
                return internalRow != null && internalRow.equals(row);
            }
        }
        return false;
    }

    public SQLRow getRow(int idx) {
        readLock.lock();
        try {
            return rows.get(idx);
        } finally {
            readLock.unlock();
        }
    }

    public Object getValueAt(int rowIdx, int colIdx) {
        SQLRow row = getRow(rowIdx);
        return row != null ? row.getValueAt(colIdx) : null;
    }
}
