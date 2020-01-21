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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;


public class JsonStore<StoreType extends StoreItem> implements Store<StoreType> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(JsonStore.class);
    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    protected static final Type STORE_TYPE = new TypeToken<ArrayList<StoreItem>>() {
        // just want the type
    }.getType();
    private static final String STORE_PATH_KEY = "store.path";
    private static final String DEFAULT_CRATEDBSQL_STORE_PATH = "./store";
    private static final Charset UTF_8 = Charset.forName("UTF-8");


    protected final File storeRoot;
    protected final String storeFileName;
    protected final Class<? extends StoreItem> clazz;
    protected final Map<String, StoreType> elementsByKey;
    protected final List<StoreType> elements;


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
    }

    @Override
    public int size() {
        return elements.size();
    }

    @Override
    public void clear() {
        elements.clear();
        elementsByKey.clear();
        store();
    }

    @Override
    public void addAll(boolean clear, StoreType... entries) {
        if (clear) {
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
    public CompletableFuture<Void> load(Consumer<List<StoreType>> valuesConsumer) {
        return CompletableFuture.runAsync(() -> {
            load();
            valuesConsumer.accept(values());
        });
    }

    public void load() {
        File file = getStoreFile(false);
        if (false == file.exists()) {
            LOGGER.info("Store file [{}] does not exist yet", file.getAbsolutePath());
            return;
        }
        List<StoreItem> contents;
        try (BufferedReader in = new BufferedReader(new FileReader(file, UTF_8))) {
            contents = GSON.fromJson(in, STORE_TYPE);
            LOGGER.info("Loaded store file [{}]", file.getAbsolutePath());
        } catch (Exception e) {
            LOGGER.error("Could not load properties file [{}]: {}",
                    file.getAbsolutePath(), e.getMessage());
            return;
        }
        try {
            Constructor<StoreType> constructor = (Constructor<StoreType>) clazz
                    .getConstructor(StoreItem.CONSTRUCTOR_SIGNATURE);
            elements.clear();
            for (StoreItem e : contents) {
                elements.add(constructor.newInstance(e));
            }
            elementsByKey.clear();
            for (StoreType e : elements) {
                elementsByKey.put(e.getKey(), e);
            }
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

        @Override
    public CompletableFuture<Void> store() {
        return CompletableFuture.runAsync(() -> {
            File file = getStoreFile(true);
            try (FileWriter out = new FileWriter(file, UTF_8, true)) {
                GSON.toJson(elements, STORE_TYPE, out);
                LOGGER.info("Stored file [{}]",
                        getStoreFile(false).getAbsolutePath());
            } catch (IOException e) {
                LOGGER.error("Could not store into file [{}]: {}",
                        file.getAbsolutePath(),
                        e.getMessage());
            }
        });
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

    }
}
