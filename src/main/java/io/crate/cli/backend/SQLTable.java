package io.crate.cli.backend;

import io.crate.cli.common.HasKey;
//import io.crate.shade.org.postgresql.jdbc.PgResultSetMetaData;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class SQLTable implements HasKey {

    public static SQLTable emptyTable() {
        return new SQLTable(null);
    }

    public static SQLTable emptyTable(String key) {
        return new SQLTable(key);
    }


    public static class SQLTableRow implements HasKey {

        private final String key;
        private final Map<String, Object> values;


        public SQLTableRow(String key, Map<String, Object> values) {
            this.key = key;
            this.values = values;
        }

        @Override
        public String getKey() {
            return key;
        }

        public Object get(String colName) {
            return values.get(colName);
        }

        public void clear() {
            values.clear();
        }
    }


    private String key;
    private boolean hasMetadata;
    private String[] columnNames;
    private int[] columnTypes;
    private final List<SQLTableRow> values;


    public SQLTable(String key) {
        this.key = key;
        this.values = new ArrayList<>();
    }

    public void extractColumnMetadata(ResultSet rs) throws SQLException {
        ResultSetMetaData metaData = rs.getMetaData();
        int columnCount = metaData.getColumnCount();
        columnNames = new String[columnCount];
        columnTypes = new int[columnCount];
        for (int i = 0; i < columnCount; i++) {
            columnNames[i] = metaData.getColumnName(i + 1);
            columnTypes[i] = metaData.getColumnType(i + 1);
        }
        hasMetadata = true;
    }

    public void addRow(String key, ResultSet rs) throws SQLException {
        if (false == hasMetadata) {
            throw new SQLException("column metadata has not been set");
        }
        Map<String, Object> values = new LinkedHashMap<>(columnNames.length);
        for (int i = 0; i < columnNames.length; i++) {
            values.put(columnNames[i], rs.getObject(i + 1));
        }
        this.values.add(new SQLTableRow(key, values));
    }

    public void setSingleRow(String key,
                             String[] columnNames,
                             int[] columnTypes,
                             Object[] values) {
        if (null == columnNames || null == columnTypes || null == values
                || columnNames.length != columnTypes.length
                || columnNames.length != values.length) {
            throw new IllegalArgumentException("illegal values");
        }
        this.columnNames = columnNames;
        this.columnTypes = columnTypes;
        this.values.clear();
        Map<String, Object> finalValues = new LinkedHashMap<>(columnNames.length);
        for (int i = 0; i < columnNames.length; i++) {
            finalValues.put(columnNames[i], values[i]);
        }
        this.values.add(new SQLTableRow(key, finalValues));
    }

    public int getSize() {
        return values.size();
    }

    public void clear() {
        values.forEach(SQLTableRow::clear);
        values.clear();
        key = null;
        hasMetadata = false;
        columnNames = null;
        columnTypes = null;
    }

    public List<SQLTableRow> getRows() {
        return values;
    }

    public List<SQLTableRow> getRows(int fromIdx, int toIdx) {
        return values.subList(fromIdx, toIdx);
    }

    public void merge(SQLTable table) {
        if (null == key && null != table.key) {
            key = table.key;
        }
        if (false == key.equals(table.key)) {
            throw new IllegalArgumentException("keys do not match");
        }
        if (null == columnNames || null == columnTypes) {
            columnNames = table.columnNames;
            columnTypes = table.columnTypes;
            if (columnNames.length != table.columnNames.length) {
                throw new IllegalArgumentException("number of columns does not match");
            }
        }
        values.addAll(table.values);
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