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

package io.quest.model;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * A table is identified by a key, it has column metadata (column names and types) and
 * a list of {@link SQLRow}s in insertion order. Rows are identified by both their key
 * and the row's values. This data structure is thread-safe.
 */
public class SQLTableR extends SQLTable<SQLRow> {

    public SQLTableR(String key) {
        super(key);
    }

    public void addRow(int rowIdx, ResultSet rs) throws SQLException {
        int[] types = colTypes;
        if (types == null) {
            throw new IllegalArgumentException("column metadata (names, types) not defined");
        }
        Object[] values = new Object[types.length];
        values[0] = rowIdx;
        for (int i = 1; i < types.length; i++) {
            values[i] = rs.getObject(i);
        }
        SQLRow row = new SQLRow(rowIdx, values);
        writeLock.lock();
        try {
            model.add(row);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Object getValueAt(int rowIdx, int colIdx) {
        SQLRow row = getRow(rowIdx);
        return row != null ? row.getValueAt(colIdx) : null;
    }

    public SQLRow getRow(int rowIdx) {
        readLock.lock();
        try {
            return model.get(rowIdx);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int size() {
        readLock.lock();
        try {
            return model.size();
        } finally {
            readLock.unlock();
        }
    }
}
