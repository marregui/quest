package io.crate.cli.backend;

import io.crate.cli.persistence.HasKey;

import java.util.LinkedHashMap;
import java.util.Map;


public class SQLRowType implements HasKey {

    private final String key;
    private final String [] columnNames;
    private final Map<String, Object> values;


    public SQLRowType(String key, String [] columnNames, Object [] values) {
        if (columnNames.length != values.length) {
            throw new IllegalArgumentException("columnNames.length != values.length");
        }
        this.key = key;
        this.columnNames = columnNames;
        this.values = new LinkedHashMap<>(columnNames.length);
        for (int i=0; i < columnNames.length; i++) {
            this.values.put(columnNames[i], values[i]);
        }
    }

    @Override
    public String getKey() {
        return key;
    }

    public String [] getColumnNames() {
        return columnNames;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public Object get(String colName) {
        return values.get(colName);
    }

    public Object set(String colName, Object value) {
        throw new UnsupportedOperationException();
    }
}