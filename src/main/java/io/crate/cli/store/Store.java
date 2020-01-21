package io.crate.cli.store;

import java.io.Closeable;
import java.io.File;
import java.util.*;


public interface Store<StoreType extends StoreItem> extends Closeable, Iterable<StoreType> {

    StoreType [] defaultStoreEntries();

    void load();

    void store();

    File getPath();

    void addAll(boolean clear, StoreType... value);

    void remove(StoreType value);

    Set<String> keys();

    StoreItem lookup(String key);

    List<StoreType> values();

    int size();

    @Override
    void close();

    @Override
    default Iterator<StoreType> iterator() {
        return new Iterator<>() {
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
