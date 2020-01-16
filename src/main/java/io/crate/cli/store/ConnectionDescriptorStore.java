package io.crate.cli.store;

import io.crate.cli.backend.ConnectionDescriptor;
import io.crate.cli.backend.ConnectionDescriptor.AttributeName;

import java.util.*;
import java.util.function.Function;


public class ConnectionDescriptorStore<T extends ConnectionDescriptor> extends BaseStore<T> {

    private static final String CONNECTIONS_STORE_FILENAME = "connections.properties";


    public ConnectionDescriptorStore(Function<String, T> newElementSupplier) {
        super(CONNECTIONS_STORE_FILENAME, newElementSupplier);
    }

    @Override
    protected Map<String, T> loadFromProperties(Properties props) {
        if (null == props) {
            return new TreeMap<>();
        }
        Map<String, T> elementsByUniqueName = new LinkedHashMap<>();
        String defaultHost = props.getProperty(AttributeName.host.name(), AttributeName.host.getDefaultValue());
        String defaultPort = props.getProperty(AttributeName.port.name(), AttributeName.port.getDefaultValue());
        String defaultUsername = props.getProperty(AttributeName.username.name(), AttributeName.username.getDefaultValue());
        String defaultPassword = props.getProperty(AttributeName.password.name(), AttributeName.password.getDefaultValue());
        LOGGER.info("Connection defaults: [{}:{}, {}:{}, {}:{}]",
                AttributeName.host, defaultHost,
                AttributeName.port, defaultPort,
                AttributeName.username, defaultUsername);
        for (Map.Entry<Object, Object> prop : props.entrySet()) {
            StoreItemDescriptor.splitStoreItemAttributeKey((String) prop.getKey(), (uniqueName, attributeName) -> {
                String value = (String) prop.getValue();
                T cd = elementsByUniqueName.computeIfAbsent(uniqueName, newElementSupplier::apply);
                switch (AttributeName.valueOf(attributeName)) {
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

    @Override
    protected String [] producePropertiesFileContents(ConnectionDescriptor cd) {
        return new String[]{
                toPropertiesFileFormat(cd.getStoreItemAttributeKey(AttributeName.host), cd.getHost()),
                toPropertiesFileFormat(cd.getStoreItemAttributeKey(AttributeName.port), cd.getPort()),
                toPropertiesFileFormat(cd.getStoreItemAttributeKey(AttributeName.username), cd.getUsername()),
                toPropertiesFileFormat(cd.getStoreItemAttributeKey(AttributeName.password), cd.getPassword())
        };
    }
}
