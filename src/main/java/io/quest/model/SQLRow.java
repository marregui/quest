package io.quest.model;

import java.util.concurrent.atomic.AtomicReference;

public class SQLRow extends SQLModel {
    private final AtomicReference<String> toString;

    public SQLRow(int key, Object[] values) {
        super(key, values);
        this.toString = new AtomicReference<>();
    }

    @Override
    public void clear() {
        super.clear();
        toString.set(null);
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
