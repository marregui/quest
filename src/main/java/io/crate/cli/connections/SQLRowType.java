package io.crate.cli.connections;

import io.crate.cli.gui.common.HasKey;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class SQLRowType extends LinkedHashMap<String, Object> implements HasKey {

    private final String key;

    public SQLRowType(String key, Map<String, Object> attributes) {
        super(attributes);
        this.key = key;
    }

    @Override
    public String getKey() {
        return key;
    }
}