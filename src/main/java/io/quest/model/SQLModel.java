package io.quest.model;

import java.util.Arrays;
import java.util.Objects;

public abstract class SQLModel implements WithKey<Integer> {

    public enum Type {COLUMNS, ROWS}


    protected final int key;
    protected Object[] values;

    public SQLModel(int key, Object[] values) {
        this.key = key;
        this.values = Objects.requireNonNull(values);
    }

    @Override
    public Integer getKey() {
        return key;
    }

    public Object getValueAt(int idx) {
        return values[idx];
    }

    public void clear() {
        Arrays.fill(values, null);
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SQLModel)) {
            return false;
        }
        SQLModel that = (SQLModel) o;
        return key == that.key;
    }
}
