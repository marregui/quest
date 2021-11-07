package io.quest.backend;

import io.quest.common.WithKey;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class SQLRow implements WithKey<Long> {
    private final SQLTable parent;
    private final long rowKey;
    private final Object[] values;
    private final AtomicReference<String> toString;

    public SQLRow(SQLTable parent, long key, Object[] values) {
        this.parent = parent;
        this.rowKey = key;
        this.values = values;
        this.toString = new AtomicReference<>();
    }

    @Override
    public Long getKey() {
        return rowKey;
    }

    public Object getValueAt(int colIdx) {
        return values[colIdx];
    }

    public int size() {
        return parent.getColCount();
    }

    public void clear() {
        Arrays.fill(values, null);
        toString.set(null);
    }

    @Override
    public int hashCode() {
        int result = 17 + Long.hashCode(rowKey);
        result = 17 * result + Arrays.deepHashCode(values);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (false == o instanceof SQLRow) {
            return false;
        }
        SQLRow that = (SQLRow) o;
        if (rowKey != that.rowKey) {
            return false;
        }
        return Arrays.deepEquals(values, that.values);
    }

    @Override
    public String toString() {
        String str = toString.get();
        if (str == null) {
            synchronized (toString) {
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
        }
        return str;
    }
}
