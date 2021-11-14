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

public class SQLTableC extends SQLTable<SQLCol> {

    public SQLTableC(String key) {
        super(key);
    }

    @Override
    public void setColMetadata(ResultSet rs) throws SQLException {
        super.setColMetadata(rs);
        writeLock.lock();
        try {
            for (int colIdx = 0; colIdx < colTypes.length; colIdx++) {
                model.add(SQLCol.checkIn(colIdx));
            }
        } finally {
            writeLock.unlock();
        }
    }

    public void addRow(int rowIdx, ResultSet rs) throws SQLException {
        int[] types = colTypes;
        if (types == null) {
            throw new IllegalArgumentException("column metadata (names, types) not defined");
        }
        writeLock.lock();
        try {
            model.get(0).setValueAt(rowIdx, rowIdx);
            for (int colIdx = 1; colIdx < colTypes.length; colIdx++) {
                model.get(colIdx).setValueAt(rowIdx, rs.getObject(colIdx));
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Object getValueAt(int rowIdx, int colIdx) {
        readLock.lock();
        try {
            return model.get(colIdx).getValueAt(rowIdx);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public int size() {
        readLock.lock();
        try {
            return model.get(0).size();
        } finally {
            readLock.unlock();
        }
    }
}
