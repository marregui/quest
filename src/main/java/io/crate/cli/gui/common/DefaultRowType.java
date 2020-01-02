package io.crate.cli.gui.common;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class DefaultRowType extends LinkedHashMap<String, Object> implements HasKey {

    private final String key;

    public DefaultRowType(String key, Map<String, Object> attributes) {
        super(attributes);
        this.key = key;
    }

    @Override
    public String getKey() {
        return key;
    }
}