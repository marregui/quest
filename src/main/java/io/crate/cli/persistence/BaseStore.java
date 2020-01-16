package io.crate.cli.persistence;

import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;


public abstract class BaseStore<StoreType extends StoreItemDescriptor> implements Store<StoreType> {

    protected static final Logger LOGGER = LoggerFactory.getLogger(BaseStore.class);
    private static final String CRATEDBSQL_STORE_PATH_KEY = "cratedbsql.store.path";
    private static final String DEFAULT_CRATEDBSQL_STORE_PATH = "./store";
    private static final Gson GSON = new Gson();

    protected final File storeRoot;
    protected final String propertiesFileName;
    protected final Function<String, StoreType> newElementSupplier;
    protected final Map<String, StoreType> elementsByKey;
    protected final List<StoreType> elements;


    protected BaseStore(String propertiesFileName, Function<String, StoreType> newElementSupplier) {
        this(System.getProperty(CRATEDBSQL_STORE_PATH_KEY, DEFAULT_CRATEDBSQL_STORE_PATH), 
                propertiesFileName, 
                newElementSupplier);
    }

    protected BaseStore(String storeRootPath,
                        String propertiesFileName,
                        Function<String, StoreType> newElementSupplier) {
        this.newElementSupplier = newElementSupplier;
        storeRoot = new File(storeRootPath);
        this.propertiesFileName = propertiesFileName;
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
        for (StoreType e: entries) {
            if (null != e) {
                elements.add(e);
                elementsByKey.put(e.getKey(), e);
            }
        }
        store();
    }

    @Override
    public void load() {
        ensureStoreExists();
        Map<String, StoreType> elementsByUniqueName = loadFromProperties(loadProperties());
        elements.clear();
        elements.addAll(elementsByUniqueName.values());
        elementsByKey.clear();
        elementsByUniqueName.values().forEach(e -> elementsByKey.put(e.getKey(), e));
    }

    @Override
    public void store(StoreType cd) {
        String key = cd.getKey();
        StoreType oldValue = elementsByKey.put(key, cd);
        if (null != oldValue) {
            elements.remove(oldValue);
        }
        elements.add(cd);
        append(getPropertiesFile(false), producePropertiesFileContents(cd));
    }

    @Override
    public File getPath() {
        return storeRoot;
    }

    @Override
    public void store() {
        ensureStoreExists();
        File connectionsFile = getPropertiesFile(true);
        if (append(connectionsFile, producePropertiesFileContents(StoreItemDescriptor.KEY_COMPARATOR))) {
            LOGGER.info("Stored properties file [{}]",
                    connectionsFile.getAbsolutePath());
        }
    }

    protected abstract Map<String, StoreType> loadFromProperties(Properties props);

    protected abstract String [] producePropertiesFileContents(StoreType newElement);

    private String[] producePropertiesFileContents(Comparator<String> keyComparator) {
        Map<String, String> mergedAttributes = new TreeMap<>(keyComparator);
        for (StoreType element : elementsByKey.values()) {
            mergedAttributes.putAll(element.getAttributes());
        }
        String[] attributes = new String[mergedAttributes.size() + 1];
        attributes[0] = String.format(Locale.ENGLISH, "# Last stored: %s\n", new Date());
        int offset = 1;
        for (Map.Entry<String, String> attr : mergedAttributes.entrySet()) {
            attributes[offset++] = toPropertiesFileFormat(attr.getKey(), attr.getValue());
        }
        return attributes;
    }

    private File getPropertiesFile(boolean deleteIfExists) {
        File connectionsFile = new File(storeRoot, propertiesFileName);
        if (connectionsFile.exists() && deleteIfExists) {
            connectionsFile.delete();
            connectionsFile = new File(storeRoot, propertiesFileName);
        }
        return connectionsFile;
    }

    private Properties loadProperties() {
        File propertiesFile = getPropertiesFile(false);
        if (false == propertiesFile.exists()) {
            LOGGER.info("Properties file [{}] does not exist yet",
                    propertiesFile.getAbsolutePath());
            return null;
        }
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(propertiesFile)) {
            props.load(in);
            LOGGER.info("Loaded properties file [{}]",
                    propertiesFile.getAbsolutePath());
            return props;
        } catch (IOException e) {
            LOGGER.error("Could not load properties file [{}]: {}",
                    propertiesFile.getAbsolutePath(), e.getMessage());
            return null;
        }
    }

    protected static String toPropertiesFileFormat(String key, String value) {
        return String.format(Locale.ENGLISH, "%s=%s\n", key, value);
    }

    private boolean append(File file, String ...lines) {
        try (FileWriter out = new FileWriter(file, true)) {
            for (String line : lines) {
                out.write(line);
            }
        } catch (IOException e) {
            LOGGER.error("Could not store properties into file [{}]: {}",
                    file.getAbsolutePath(),
                    e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public void remove(StoreType cd) {
        if (null == cd) {
            return;
        }
        if (null != elementsByKey.remove(cd.getKey())) {
            elements.remove(cd);
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

    protected void ensureStoreExists(){
        if (false == storeRoot.exists()) {
            LOGGER.info("Creating Store [{}]", storeRoot.getAbsolutePath());
            storeRoot.mkdirs();
        }
    }
}
