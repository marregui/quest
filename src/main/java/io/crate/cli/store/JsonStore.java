/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */
package io.crate.cli.store;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.*;


public abstract class JsonStore<StoreType extends StoreItem> implements Store<StoreType> {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonStore.class);
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type STORE_TYPE = new TypeToken<ArrayList<StoreItem>>() {
        // just want the type
    }.getType();
    private static final String STORE_PATH_KEY = "store.path";
    private static final String DEFAULT_CRATEDBSQL_STORE_PATH = "./store";
    private static final Charset UTF_8 = Charset.forName("UTF-8");


    private final File storeRoot;
    private final String storeFileName;
    private final Class<? extends StoreItem> clazz;
    private final Map<String, StoreType> elementsByKey;
    private final List<StoreType> elements;
    private final ExecutorService asyncStorer;


    public JsonStore(String storeFileName, Class<? extends StoreItem> clazz) {
        this(System.getProperty(
                STORE_PATH_KEY,
                DEFAULT_CRATEDBSQL_STORE_PATH),
                storeFileName,
                clazz);
    }

    public JsonStore(String storeRootPath,
                     String storeFileName,
                     Class<? extends StoreItem> clazz) {
        this.clazz = clazz;
        storeRoot = new File(storeRootPath);
        this.storeFileName = storeFileName;
        elementsByKey = new TreeMap<>();
        elements = new ArrayList<>();
        asyncStorer = Executors.newSingleThreadExecutor(task -> {
            Thread storerThread = new Thread(task);
            storerThread.setDaemon(false);
            storerThread.setName(String.format(
                    Locale.ENGLISH,
                    "%s-storer-%s",
                    JsonStore.class.getSimpleName(),
                    storeFileName));
            return storerThread;
        });
    }

    @Override
    public abstract StoreType [] defaultStoreEntries();

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public void addAll(boolean clearFirst, StoreType... entries) {
        if (clearFirst) {
            elements.clear();
            elementsByKey.clear();
        }
        for (StoreType e : entries) {
            if (null != e) {
                elements.add(e);
                elementsByKey.put(e.getKey(), e);
            }
        }
        store();
    }

    @Override
    public File getPath() {
        return storeRoot;
    }

    @Override
    public void load() {
        File file = getStoreFile(false);
        if (false == file.exists()) {
            for (StoreType e : defaultStoreEntries()) {
                if (null != e) {
                    elements.add(e);
                    elementsByKey.put(e.getKey(), e);
                }
            }
            storeSync(() -> {
                LOGGER.info("Created default store [{}]", file.getAbsolutePath());
            });
            return;
        }

        List<StoreItem> contents;
        try (BufferedReader in = new BufferedReader(new FileReader(file, UTF_8))) {
            contents = GSON.fromJson(in, STORE_TYPE);
            LOGGER.info("Loaded store [{}]", file.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("Could not load store [{}]: {}",
                    file.getAbsolutePath(), e.getMessage());
            return;
        }
        if (null != contents) {
            try {
                Constructor<StoreType> eFactory = (Constructor<StoreType>) clazz.getConstructor(
                        StoreItem.CONSTRUCTOR_SIGNATURE);
                elements.clear();
                for (StoreItem e : contents) {
                    elements.add(eFactory.newInstance(e));
                }
                elementsByKey.clear();
                for (StoreType e : elements) {
                    elementsByKey.put(e.getKey(), e);
                }
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void store() {
        store(null);
    }

    private void store(Runnable whenDoneTask) {
        asyncStorer.submit(() -> {
            storeSync(whenDoneTask);
        });
    }

    private void storeSync(Runnable whenDoneTask) {
        try {
            File file = getStoreFile(true);
            try (FileWriter out = new FileWriter(file, UTF_8, true)) {
                GSON.toJson(elements, STORE_TYPE, out);
                out.flush();
                LOGGER.info("Stored file [{}]",
                        getStoreFile(false).getAbsolutePath());
            } catch (IOException e) {
                LOGGER.error("Could not store into file [{}]: {}",
                        file.getAbsolutePath(),
                        e.getMessage());
            }
        } finally {
            if (null != whenDoneTask) {
                whenDoneTask.run();
            }
        }
    }

    private File getStoreFile(boolean deleteIfExists) {
        if (false == storeRoot.exists()) {
            LOGGER.info("Creating Store [{}]", storeRoot.getAbsolutePath());
            storeRoot.mkdirs();
        }
        File file = new File(storeRoot, storeFileName);
        if (file.exists() && deleteIfExists) {
            file.delete();
            file = new File(storeRoot, storeFileName);
        }
        return file;
    }

    @Override
    public void remove(StoreType element) {
        if (null == element) {
            return;
        }
        if (null != elementsByKey.remove(element.getKey())) {
            elements.remove(element);
            store();
        }
    }

    @Override
    public Set<String> keys() {
        return Collections.unmodifiableSet(elementsByKey.keySet());
    }

    @Override
    public List<StoreType> values() {
        return Collections.unmodifiableList(elements);
    }

    @Override
    public StoreType lookup(String key) {
        return null != key ? elementsByKey.get(key) : null;
    }

    @Override
    public void close() {
        storeSync(null);
        elements.clear();
        elementsByKey.clear();
        asyncStorer.shutdown();
        try {
            asyncStorer.awaitTermination(500L, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
