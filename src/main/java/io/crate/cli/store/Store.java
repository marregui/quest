package io.crate.cli.store;

import java.io.File;
import java.util.*;


public interface Store<StoreType extends StoreItem> extends Iterable<StoreType> {

    void load();

    void store();

    File getPath();

    void clear();

    void addAll(boolean clear, StoreType... value);

    void remove(StoreType value);

    Set<String> keys();

    List<StoreType> values();

    StoreItem lookup(String key);

    int size();

    @Override
    default Iterator<StoreType> iterator() {
        return new Iterator<StoreType>() {
            private final List<StoreType> values = values();
            private int offset = 0;

            @Override
            public boolean hasNext() {
                return offset < values.size();
            }

            @Override
            public StoreType next() {
                if (offset >= values.size()) {
                    throw new IndexOutOfBoundsException();
                }
                return values.get(offset++);
            }
        };
    }
}
