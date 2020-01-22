package io.crate.cli.backend;

import io.crate.cli.common.GUIToolkit;
import io.crate.cli.common.HasKey;
import io.crate.cli.store.StoreItem;

import java.util.*;


public class ConnectionItem extends StoreItem {


    public enum AttributeName implements HasKey {

        host("localhost"),
        port("5432"),
        username("crate"),
        password("");


        private final String defaultValue;

        AttributeName(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public String getKey() {
            return name();
        }

        public String getDefaultValue() {
            return defaultValue;
        }
    }


    public ConnectionItem(StoreItem other) {
        super(other);
    }

    public ConnectionItem(String name) {
        super(name);
        attributes.put(getStoreItemAttributeKey(AttributeName.host), AttributeName.host.getDefaultValue());
        attributes.put(getStoreItemAttributeKey(AttributeName.port), AttributeName.port.getDefaultValue());
        attributes.put(getStoreItemAttributeKey(AttributeName.username), AttributeName.username.getDefaultValue());
        attributes.put(getStoreItemAttributeKey(AttributeName.password), AttributeName.password.getDefaultValue());
    }

    @Override
    public final String getKey() {
        return String.format(
                Locale.ENGLISH,
                "%s %s@%s:%s",
                name,
                attributes.get(getStoreItemAttributeKey(AttributeName.username)),
                attributes.get(getStoreItemAttributeKey(AttributeName.host)),
                attributes.get(getStoreItemAttributeKey(AttributeName.port)));
    }

    public String getUrl() {
        return String.format(
                Locale.ENGLISH,
                GUIToolkit.JDBC_DRIVER_URL_FORMAT,
                getHost(),
                getPort());
    }

    public Properties loginProperties() {
        Properties props = new Properties();
        // https://jdbc.postgresql.org/documentation/head/connect.html
        props.put("user", getUsername());
        props.put("password", getPassword());
        props.put("loggerLevel", "DEBUG"); // also:OFF
        props.put("ssl", false);
        //props.put("sslkey", "");
        //props.put("sslpassword", "");
        props.put("recvBufferSize", 1 * 1024 * 1024); // SO_RCVBUF
        props.put("defaultRowFetchSize", SQLExecutor.MAX_BATCH_SIZE);
        props.put("loginTimeout", 10); // seconds, fail fast-ish
        props.put("socketTimeout", 60); // seconds, no query allowed longer reads than 60 seconds
        props.put("tcpKeepAlive", true);
        return props;
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
}
