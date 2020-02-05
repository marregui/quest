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
package io.crate.cli.backend;

import io.crate.cli.common.HasKey;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;


public class SQLTable implements HasKey {

    public static SQLTable emptyTable() {
        return new SQLTable(null);
    }

    public static SQLTable emptyTable(String key) {
        return new SQLTable(key);
    }

    public static class SQLTableRow implements HasKey {

        private final SQLTable parent;
        private final String key;
        private final Object[] values;
        private final AtomicReference<String> toString;


        private SQLTableRow(SQLTable parent, String key, Object[] values) {
            this.parent = parent;
            this.key = key;
            this.values = values;
            toString = new AtomicReference<>();
        }

        @Override
        public String getKey() {
            return key;
        }

        public SQLTable getParent() {
            return parent;
        }

        public Object get(String colName) {
            Integer idx = parent.columnIdx.get(colName);
            if (null == idx) {
                throw new IllegalArgumentException(String.format(
                        Locale.ENGLISH,
                        "Unknown column [%s]",
                        colName));
            }
            return values[idx.intValue()];
        }

        public void clear() {
            Arrays.fill(values, null);
        }

        public Object[] getValues() {
            return values;
        }

        @Override
        public String toString() {
            String str = toString.get();
            if (null == str) {
                StringBuilder sb = new StringBuilder();
                sb.append("key: ").append(key).append(", values: ");
                if (null != values) {
                    for (Object o: values) {
                        sb.append(null != o ? o : "null").append(", ");
                    }
                    if (values.length > 0) {
                        sb.setLength(sb.length() - 2);
                    }
                }
                str = sb.toString();
                if (false == toString.compareAndSet(null, str)) {
                    str = toString.get();
                }
            }
            return str;
        }
    }

    private String key;
    private boolean hasMetadata;
    private String[] columnNames;
    private final Map<String, Integer> columnIdx;
    private int[] columnTypes;
    private final List<SQLTableRow> rows;


    public SQLTable(String key) {
        this.key = key;
        rows = new ArrayList<>();
        columnIdx = new HashMap<>();
    }

    public void extractColumnMetadata(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        columnIdx.clear();
        columnNames = new String[columnCount];
        columnTypes = new int[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnNames[i] = metaData.getColumnName(i + 1);
            columnTypes[i] = metaData.getColumnType(i + 1);
            columnIdx.put(columnNames[i], i);
        }
        hasMetadata = true;
    }

    public void addRow(String key, ResultSet rs) throws SQLException {
        if (false == hasMetadata) {
            throw new SQLException("column metadata has not been set");
        }
        Object[] values = new Object[columnNames.length];
        for (int i = 0; i < columnNames.length; i++) {
            values[i] = rs.getObject(i + 1);
        }
        this.rows.add(new SQLTableRow(this, key, values));
    }

    public void setSingleOkRow(String key) {
        columnNames = new String[]{ "Status" };
        columnTypes = new int[]{ Types.VARCHAR };
        columnIdx.clear();
        columnIdx.put(columnNames[0], 0);
        rows.clear();
        rows.add(new SQLTableRow(this, key, new Object[]{ "OK" }));
    }

    public int getSize() {
        return rows.size();
    }

    public void clear() {
        rows.forEach(SQLTableRow::clear);
        rows.clear();
        key = null;
        hasMetadata = false;
        columnNames = null;
        columnTypes = null;
        columnIdx.clear();
    }

    public List<SQLTableRow> getRows() {
        return rows;
    }

    public List<SQLTableRow> getRows(int fromIdx, int toIdx) {
        return rows.subList(fromIdx, toIdx);
    }

    public void merge(SQLTable table) {
        if (null == key && null != table.key) {
            key = table.key;
        }
        if (null != key && false == key.equals(table.key)) {
            throw new IllegalArgumentException("keys do not match");
        }
        if (null == columnNames || null == columnTypes) {
            columnNames = table.columnNames;
            columnTypes = table.columnTypes;
            if (columnNames.length != columnTypes.length) {
                throw new IllegalArgumentException("number of columns does not match");
            }
        }
        rows.addAll(table.rows);
    }

    @Override
    public String getKey() {
        return key;
    }

    public String[] getColumnNames() {
        return columnNames;
    }

    public int[] getColumnTypes() {
        return columnTypes;
    }
}