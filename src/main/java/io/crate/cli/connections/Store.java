package io.crate.cli.connections;

import io.crate.cli.gui.common.HasKey;

import java.util.*;


public interface Store<EntryType extends HasKey> extends Iterable<EntryType> {

    void load();

    void store();

    void add(EntryType value);

    void remove(EntryType value);

    Set<String> keys();

    List<EntryType> values();

    EntryType lookup(String key);

    @Override
    default Iterator<EntryType> iterator() {
        return new Iterator<EntryType>() {
            private final List<EntryType> values = values();
            private int offset = 0;

            @Override
            public boolean hasNext() {
                return offset < values.size();
            }

            @Override
            public EntryType next() {
                if (offset >= values.size()) {
                    throw new IndexOutOfBoundsException();
                }
                return values.get(offset++);
            }
        };
    }
}
