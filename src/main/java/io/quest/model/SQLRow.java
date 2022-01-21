package io.quest.model;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class SQLRow implements WithKey<Integer> {

    private final int key;
    private Object[] values;
    private final AtomicReference<String> toString;

    public SQLRow(int key, Object[] values) {
        this.key = key;
        this.values = Objects.requireNonNull(values);
        this.toString = new AtomicReference<>();
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
        toString.set(null);
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
        if (!(o instanceof SQLRow)) {
            return false;
        }
        SQLRow that = (SQLRow) o;
        return key == that.key;
    }

    @Override
    public String toString() {
        String str = toString.get();
        if (str == null) {
            StringBuilder sb = new StringBuilder();
            if (null != values) {
                for (Object o : values) {
                    sb.append(null != o ? o : "null").append(", ");
                }
                if (values.length > 0) {
                    sb.setLength(sb.length() - 2);
                }
            }
            str = sb.toString();
            if (!toString.compareAndSet(null, str)) {
                str = toString.get();
            }
        }
        return str;
    }
}
