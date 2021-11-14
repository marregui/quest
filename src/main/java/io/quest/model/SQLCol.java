package io.quest.model;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class SQLCol extends SQLModel {

    private static final Map<Integer, SQLCol> COLS = new WeakHashMap<>(5);
    private static final Set<Integer> BUSY_COLS = new HashSet<>(5);

    public static synchronized SQLCol checkIn(int colIdx) {
        if (BUSY_COLS.contains(colIdx)) {
            throw new RuntimeException(String.format("colIdx %d is busy", colIdx));
        }
        SQLCol col = COLS.get(colIdx);
        if (col == null) {
            COLS.put(colIdx, col = new SQLCol(colIdx));
        }
        BUSY_COLS.add(colIdx);
        return col;
    }

    public static synchronized void checkOut(int colIdx) {
        if (!BUSY_COLS.remove(colIdx)) {
            throw new RuntimeException(String.format("colIdx %d is not busy", colIdx));
        }
    }

    private volatile int lastRowIdx;

    public SQLCol(int key) {
        super(key, new Object[SQLExecutor.MAX_BATCH_SIZE]);
    }

    public void setValueAt(int rowIdx, Object value) {
        int len = values.length;
        if (rowIdx >= len) {
            int newSize = len * 2;
            Object[] copy = new Object[newSize];
            System.arraycopy(values, 0, copy, 0, len);
            values = copy;
        }
        values[rowIdx] = value;
        lastRowIdx = Math.max(lastRowIdx, rowIdx + 1);
    }

    public int size() {
        return lastRowIdx;
    }

    @Override
    public void clear() {
        lastRowIdx = 0;
        super.clear();
        checkOut(key);
    }
}
