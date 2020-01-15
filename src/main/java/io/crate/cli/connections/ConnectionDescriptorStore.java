package io.crate.cli.connections;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.*;
import java.util.function.Function;

public class ConnectionDescriptorStore<T extends ConnectionDescriptor> implements Store<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionDescriptorStore.class);
    private static final String CRATEDBSQL_STORE_PATH_KEY = "cratedbsql.store.path";
    private static final String DEFAULT_CRATEDBSQL_STORE_PATH = "./store";
    private static final String CONNECTIONS_STORE_FILENAME = "connections.properties";


    private final File storeRoot;
    private final Function<String, T> newElementSupplier;
    private final Map<String, T> elementsByKey;
    private final List<T> elements;


    public ConnectionDescriptorStore(Function<String, T> newElementSupplier) {
        this(System.getProperty(CRATEDBSQL_STORE_PATH_KEY, DEFAULT_CRATEDBSQL_STORE_PATH), newElementSupplier);
    }

    public ConnectionDescriptorStore(String storeRootPath, Function<String, T> newElementSupplier) {
        this.newElementSupplier = newElementSupplier;
        storeRoot = new File(storeRootPath);
        elementsByKey = new TreeMap<>();
        elements = new ArrayList<>();
    }

    @Override
    public void load() {
        ensureStoreExists();
        Map<String, T> elementsByUniqueName = loadFromProperties(loadProperties());
        elements.clear();
        elements.addAll(elementsByUniqueName.values());
        elementsByKey.clear();
        elementsByUniqueName.values().forEach(e -> elementsByKey.put(e.getKey(), e));
    }

    private File getConnectionsFile(boolean deleteIfExists) {
        File connectionsFile = new File(storeRoot, CONNECTIONS_STORE_FILENAME);
        if (connectionsFile.exists() && deleteIfExists) {
            connectionsFile.delete();
            connectionsFile = new File(storeRoot, CONNECTIONS_STORE_FILENAME);
        }
        return connectionsFile;
    }

    private Properties loadProperties() {
        File connectionsFile = getConnectionsFile(false);
        if (false == connectionsFile.exists()) {
            LOGGER.info("Connections file [{}] does not exist yet",
                    connectionsFile.getAbsolutePath());
            return null;
        }
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream(connectionsFile)) {
            props.load(in);
            LOGGER.info("Loaded connections file [{}]",
                    connectionsFile.getAbsolutePath());
            return props;
        } catch (IOException e) {
            LOGGER.error("Could not load connections file [{}]: {}",
                    connectionsFile.getAbsolutePath(), e.getMessage());
            return null;
        }
    }

    private static String getAttribute(Properties props, AttributeName attributeName) {
        return props.getProperty(attributeName.name(), attributeName.getDefaultValue());
    }

    private Map<String, T> loadFromProperties(Properties props) {
        if (null == props) {
            return new TreeMap<>();
        }
        Map<String, T> elementsByUniqueName = new LinkedHashMap<>();
        String defaultHost = getAttribute(props, AttributeName.host);
        String defaultPort = getAttribute(props, AttributeName.port);
        String defaultUsername = getAttribute(props, AttributeName.username);
        String defaultPassword = getAttribute(props, AttributeName.password);
        LOGGER.info("Connection defaults: [{}:{}, {}:{}, {}:{}]",
                AttributeName.host, defaultHost,
                AttributeName.port, defaultPort,
                AttributeName.username, defaultUsername);
        for (Map.Entry<Object, Object> prop : props.entrySet()) {
            AttributeName.splitKey((String) prop.getKey(), (uniqueName, attributeName) -> {
                String value = (String) prop.getValue();
                T cd = elementsByUniqueName.computeIfAbsent(uniqueName, newElementSupplier::apply);
                switch (attributeName) {
                    case host:
                        cd.setHost(value, defaultHost);
                        break;

                    case port:
                        cd.setPort(value, defaultPort);
                        break;

                    case username:
                        cd.setUsername(value, defaultUsername);
                        break;

                    case password:
                        cd.setPassword(value, defaultPassword);
                        break;

                    default:
                        LOGGER.error("Ignoring custom connection attribute [{}.{}={}]",
                                uniqueName, attributeName, value);
                }
            });
        }
        return elementsByUniqueName;
    }

    private static String toPropertiesFileFormat(String key, String value) {
        return String.format(Locale.ENGLISH, "%s=%s\n", key, value);
    }

    private String [] mergedConnectionDescriptorAttributes() {
        Map<String, String> mergedAttributes = new TreeMap<>(AttributeName.KEY_COMPARATOR);
        for (T element : elementsByKey.values()) {
            mergedAttributes.putAll(element.getAttributes());
        }
        String [] attributes = new String[mergedAttributes.size() + 1];
        attributes[0] = String.format(Locale.ENGLISH, "# Last stored: %s\n", new Date());
        int offset = 1;
        for (Map.Entry<String, String> attr : mergedAttributes.entrySet()) {
            attributes[offset++] = toPropertiesFileFormat(attr.getKey(), attr.getValue());
        }
        return attributes;
    }

    @Override
    public File getPath() {
        return storeRoot;
    }

    @Override
    public void store() {
        ensureStoreExists();
        File connectionsFile = getConnectionsFile(true);
        if (append(connectionsFile, mergedConnectionDescriptorAttributes())) {
            LOGGER.info("Stored connections file [{}]",
                    connectionsFile.getAbsolutePath());
        }
    }

    private boolean append(File file, String ...lines) {
        try (FileWriter out = new FileWriter(file, true)) {
            for (String line : lines) {
                out.write(line);
            }
        } catch (IOException e) {
            LOGGER.error("Could not store connections into file [{}]: {}",
                    file.getAbsolutePath(),
                    e.getMessage());
            return false;
        }
        return true;
    }

    @Override
    public void add(T cd) {
        String key = cd.getKey();
        T oldValue = elementsByKey.put(key, cd);
        if (null != oldValue) {
            elements.remove(oldValue);
        }
        elements.add(cd);
        append(getConnectionsFile(false), new String[]{
                toPropertiesFileFormat(cd.getAttributeKey(AttributeName.host), cd.getHost()),
                toPropertiesFileFormat(cd.getAttributeKey(AttributeName.port), cd.getPort()),
                toPropertiesFileFormat(cd.getAttributeKey(AttributeName.username), cd.getUsername()),
                toPropertiesFileFormat(cd.getAttributeKey(AttributeName.password), cd.getPassword())
        });
    }

    @Override
    public void remove(T cd) {
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
    public List<T> values() {
        return Collections.unmodifiableList(elements);
    }

    @Override
    public T lookup(String key) {
        return null != key ? elementsByKey.get(key) : null;
    }

    private void ensureStoreExists(){
        if (false == storeRoot.exists()) {
            LOGGER.info("Creating Store [{}]", storeRoot.getAbsolutePath());
            storeRoot.mkdirs();
        }
    }
}
