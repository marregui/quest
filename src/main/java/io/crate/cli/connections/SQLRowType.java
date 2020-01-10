package io.crate.cli.connections;

import io.crate.cli.gui.common.HasKey;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class SQLRowType extends LinkedHashMap<String, Object> implements HasKey {

    private static final String [] NO_VALUES = {};

    private final String key;

    public SQLRowType(String key, Map<String, Object> attributes) {
        super(attributes);
        this.key = key;
    }

    @Override
    public String getKey() {
        return key;
    }

    public String [] getColumnNames() {
        int size = size();
        if (0 == size) {
           return NO_VALUES;
        }
        return keySet().toArray(new String[size]);
    }
}