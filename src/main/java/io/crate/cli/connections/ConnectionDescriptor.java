package io.crate.cli.connections;

import io.crate.cli.gui.common.HasKey;

import java.util.*;


public class ConnectionDescriptor implements HasKey {

    private static final String JDBC_DRIVER_URL_FORMAT = "jdbc:crate://%s:%s/";


    private String name;
    private Map<String, String> attributes;


    public ConnectionDescriptor(String name) {
        if (null == name || name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }
        this.name = name;
        attributes = new TreeMap<>();
        attributes.put(AttributeName.key(name, AttributeName.host), AttributeName.host.getDefaultValue());
        attributes.put(AttributeName.key(name, AttributeName.port), AttributeName.port.getDefaultValue());
        attributes.put(AttributeName.key(name, AttributeName.username), AttributeName.username.getDefaultValue());
        attributes.put(AttributeName.key(name, AttributeName.password), AttributeName.password.getDefaultValue());
    }

    public String getAttributeKey(AttributeName attr) {
        return AttributeName.key(name, attr);
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Override
    public final String getKey() {
        return String.format(
                Locale.ENGLISH,
                "%s %s@%s:%s",
                name,
                attributes.get(getAttributeKey(AttributeName.username)),
                attributes.get(getAttributeKey(AttributeName.host)),
                attributes.get(getAttributeKey(AttributeName.port)));
    }

    public String getName() {
        return name;
    }

    public String setName(String newName) {
        if (null == newName || newName.isEmpty()) {
            throw new IllegalArgumentException("newName cannot be null or empty");
        }
        if (false == name.equals(newName)) {
            attributes = getRenamedAttributes(attributes, newName);
            name = newName;
        }
        return name;
    }

    private Map<String, String> getRenamedAttributes(Map<String, String> attributes, String newName) {
        Map<String, String> renamed = new HashMap<>();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            AttributeName.splitKey(entry.getKey(), (uniqueName, attributeName) -> {
                if (uniqueName.equals(name)) {
                    String newKey = name.equals(newName) ?
                            entry.getKey()
                            :
                            AttributeName.key(newName, attributeName);
                    renamed.put(newKey, entry.getValue());
                }
            });

        }
        return renamed;
    }

    public String getUrl() {
        return String.format(Locale.ENGLISH, JDBC_DRIVER_URL_FORMAT, getHost(), getPort());
    }

    public Properties loginProperties() {
        Properties props = new Properties();
        props.put("user", getUsername());
        props.put("password", getPassword());
        return props;
    }

    public String getAttribute(String attributeName) {
        return getAttribute(AttributeName.valueOf(attributeName));
    }

    private String getAttribute(AttributeName attributeName) {
        return attributes.get(getAttributeKey(attributeName));
    }

    public String setAttribute(String attributeName, String value) {
        return setAttribute(AttributeName.valueOf(attributeName), value);
    }

    public String setAttribute(AttributeName attributeName, String value) {
        return setAttribute(attributeName, value, attributeName.getDefaultValue());
    }

    public String setAttribute(AttributeName attributeName, String value, String defaultValue) {
        return attributes.put(
                getAttributeKey(attributeName),
                null == value || value.isEmpty() ? defaultValue : value);
    }

    public String getHost() {
        return getAttribute(AttributeName.host);
    }

    public void setHost(String host) {
        setHost(host, AttributeName.host.getDefaultValue());
    }

    public void setHost(String host, String defaultHost) {
        setAttribute(AttributeName.host, host, defaultHost);
    }

    public String getPort() {
        return getAttribute(AttributeName.port);
    }

    public void setPort(String port) {
        setPort(port, AttributeName.port.getDefaultValue());
    }

    public void setPort(String port, String defaultPort) {
        setAttribute(AttributeName.port, port, defaultPort);
    }

    public String getUsername() {
        return getAttribute(AttributeName.username);
    }

    public void setUsername(String username) {
        setUsername(username, AttributeName.username.getDefaultValue());
    }

    public void setUsername(String username, String defaultUsername) {
        setAttribute(AttributeName.username, username, defaultUsername);
    }

    public String getPassword() {
        return getAttribute(AttributeName.password);
    }

    public void setPassword(String password) {
        setPassword(password, AttributeName.password.getDefaultValue());
    }

    public void setPassword(String password, String defaultPassword) {
        setAttribute(AttributeName.password, password, defaultPassword);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ConnectionDescriptor that = (ConnectionDescriptor) o;
        return name.equals(that.name) && attributes.equals(that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, attributes);
    }

    @Override
    public String toString() {
        return getKey();
    }
}
