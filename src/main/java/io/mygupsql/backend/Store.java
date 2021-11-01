/* **
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) 2019 - 2022, Miguel Arregui a.k.a. marregui
 */

package io.mygupsql.backend;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;


/**
 * A store is a persistent list of entries, each a subclass of {@link StoreEntry},
 * backed by a JSON formatted file. Each entry has a unique key and a set of
 * identified attributes (key, value pairs).
 *
 * @param <T> a subclass of StoreItem
 */
public class Store<T extends StoreEntry> implements Closeable, Iterable<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Store.class);
    private static final String STORE_PATH_KEY = "store.path";
    private static final String DEFAULT_STORE_PATH = ".mygupsql";
    private static final Class<?>[] ITEM_CONSTRUCTOR_SIGNATURE = {
            StoreEntry.class
    };
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final StoreEntry[] EMPTY_STORE = new StoreEntry[0];
    private static final Type STORE_TYPE = new TypeToken<ArrayList<StoreEntry>>() {
        /* just want the type */
    }.getType();

    /**
     * Takes the value from system property "store.path" if set, otherwise the
     * store's default root path is ".mygupsql", relative to the home folder of
     * the running process.
     *
     * @return the default store path
     */
    public static File getDefaultRootPath() {
        return new File(System.getProperty(STORE_PATH_KEY, DEFAULT_STORE_PATH));
    }

    private final File rootPath;
    private final String fileName;
    private final Class<? extends StoreEntry> entryClass;
    private final List<T> entries;
    private final ExecutorService asyncPersister;

    /**
     * Constructor.
     * <p>
     * The store's path is folder "rootPath", which hosts a file named "fileName".
     * The contents of the file are a JSON
     *
     * @param rootPath   persistence folder used by the store
     * @param fileName   name of the JSON formatted file within rootPath that contains
     *                   the store's entries
     * @param entryClass type/class of the entries within the file, must be a subclass
     *                   of {@link StoreEntry} and have
     */
    public Store(File rootPath, String fileName, Class<? extends StoreEntry> entryClass) {
        this.rootPath = rootPath;
        this.fileName = fileName;
        this.entryClass = entryClass;
        entries = new ArrayList<>();
        asyncPersister = Executors.newSingleThreadExecutor(runnable -> {
            Thread t = new Thread(runnable);
            t.setName("Store-" + fileName);
            return t;
        });
    }

    /**
     * Constructor.
     * <p>
     * The store's path is whatever folder is returned by
     * {@link Store#getDefaultRootPath()}.
     *
     * @param fileName name of the JSON formatted file within rootPath that contains
     *                 the store's entries
     */
    public Store(String fileName, Class<? extends StoreEntry> clazz) {
        this(getDefaultRootPath(), fileName, clazz);
    }

    /**
     * This method can be overridden to provide default store entries, which are added
     * to the store by method {@link Store#loadEntriesFromFile()} when the backing file
     * does not exist.
     *
     * @return the store's default entries
     */
    @SuppressWarnings("unchecked")
    public T[] defaultStoreEntries() {
        return (T[]) EMPTY_STORE;
    }

    /**
     * @return the store's root path, where all persisted files are kept
     */
    public File getRootPath() {
        return rootPath;
    }

    /**
     * Adds the entry to the store, giving you the chance to clear the store first.
     * Store contents are asynchronously persisted by the background thread.
     *
     * @param entry           to be added
     * @param clearStoreFirst if true, the contents of the store are cleared
     */
    public synchronized void addEntry(T entry, boolean clearStoreFirst) {
        if (clearStoreFirst) {
            entries.clear();
        }
        if (entry != null) {
            entries.add(entry);
        }
        asyncSaveToFile();
    }

    /**
     * Returns the entry at the specified index.
     *
     * @param idx         index of the entry
     * @param constructor supplies the entry at idx
     */
    public synchronized T getEntry(int idx, Supplier<T> constructor) {
        if (constructor != null && entries.size() == idx) {
            entries.add(idx, constructor.get());
        }
        return entries.get(idx);
    }

    /**
     * Removes the entry from the store. Store contents are asynchronously persisted
     * by a background thread.
     *
     * @param entry to be removed
     */
    public synchronized void removeEntry(T entry) {
        if (entry != null) {
            entries.remove(entry);
            asyncSaveToFile();
        }
    }

    /**
     * Removes the entry from the store. Store contents are asynchronously persisted
     * by a background thread.
     *
     * @param idx to be removed
     */
    public synchronized void removeEntry(int idx) {
        entries.remove(idx);
        asyncSaveToFile();
    }

    /**
     * @return a read only list containing the store entries
     */
    public synchronized List<T> entries() {
        return Collections.unmodifiableList(entries);
    }

    /**
     * @return an array containing the store entries' name
     */
    public synchronized String[] entryNames() {
        return entries.stream().map(StoreEntry::getName).toArray(String[]::new);
    }

    /**
     * @return the number of entries in the store
     */
    public synchronized int size() {
        return entries.size();
    }

    /**
     * Returns thread safe, read only iterator over the current entries.
     *
     * @return an iterator
     */
    @Override
    public Iterator<T> iterator() {
        return new Iterator<>() {

            private final List<T> values = entries();
            private int idx = 0;

            @Override
            public boolean hasNext() {
                return idx < values.size();
            }

            @Override
            public T next() {
                if (idx >= values.size()) {
                    throw new NoSuchElementException();
                }
                return values.get(idx++);
            }
        };
    }

    /**
     * Loads the store's entries from the backing JSON formatted file.
     * <p>
     * If the file does not exist, it is created on the spot with the default store
     * entries. Otherwise the backing file is read and parsed.
     */
    public void loadEntriesFromFile() {
        File file = getFile(false);
        if (!file.exists()) {
            for (T entry : defaultStoreEntries()) {
                if (entry != null) {
                    entries.add(entry);
                }
            }
            saveEntriesToFile(() -> LOGGER.info("Created default store [{}]", file.getAbsolutePath()));
            return;
        }

        List<StoreEntry> content = loadEntriesFromFile(file);
        if (content != null) {
            try {
                // This constructor is T's decorator constructor to StoreEntry(StoreEntry
                // other).
                // We do not need to instantiate yet another attribute's map when we can recycle
                // the instance provided by GSON.
                @SuppressWarnings("unchecked")
                Constructor<T> entryFactory = (Constructor<T>) entryClass.getConstructor(ITEM_CONSTRUCTOR_SIGNATURE);
                entries.clear();
                for (StoreEntry i : content) {
                    entries.add(entryFactory.newInstance(i));
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Persists the store to its backing JSON formatted file using asynchronously.
     * This is a non blocking call.
     */
    public void asyncSaveToFile() {
        asyncPersister.submit(() -> saveEntriesToFile(null));
    }

    public void saveToFile(File file) {
        try (FileWriter out = new FileWriter(file, StandardCharsets.UTF_8, true)) {
            GSON.toJson(entries, STORE_TYPE, out);
            LOGGER.info("Saved [{}]", file.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Could not store into file [{}]: {}", file.getAbsolutePath(), e.getMessage());
        }
    }

    /**
     * Shuts down the persistence thread, saves the contents of the store, as they
     * are, to the backing file and then releases the store's resources.
     */
    @Override
    public void close() {
        asyncPersister.shutdown();
        try {
            asyncPersister.awaitTermination(400L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        saveEntriesToFile(null);
        entries.clear();
    }

    private static List<StoreEntry> loadEntriesFromFile(File file) {
        List<StoreEntry> entries = null;
        try (BufferedReader in = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            entries = GSON.fromJson(in, STORE_TYPE);
            LOGGER.info("Loaded [{}]", file.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("Could not load store [{}]: {}", file.getAbsolutePath(), e.getMessage());
        }
        return entries;
    }

    private void saveEntriesToFile(Runnable whenDoneTask) {
        try {
            saveToFile(getFile(true));
        } finally {
            if (whenDoneTask != null) {
                whenDoneTask.run();
            }
        }
    }

    private File getFile(boolean deleteIfExists) {
        if (!rootPath.exists()) {
            boolean created = rootPath.mkdirs();
            LOGGER.info("Creating Store [{}]: {}", rootPath.getAbsolutePath(), created ? "Ok" : "Fail");
        }
        File file = new File(rootPath, fileName);
        if (deleteIfExists && file.exists()) {
            if (!file.delete()) {
                throw new RuntimeException("Could not delete: " + file.getAbsolutePath());
            }
        }
        return file;
    }
}
