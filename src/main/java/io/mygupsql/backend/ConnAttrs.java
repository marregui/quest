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
 * Copyright 2020, Miguel Arregui a.k.a. marregui
 */

package io.mygupsql.backend;

import java.util.Properties;

import io.mygupsql.WithKey;


/**
 * Persistent connection attributes, host, port, username and password.
 */
public class ConnAttrs extends StoreEntry {

    private static final String JDBC_DRIVER_URL_FORMAT = "jdbc:postgresql://%s:%s/";

    /**
     * Attribute names.
     */
    public enum AttrName implements WithKey {

        /**
         * host.
         */
        host("localhost"),
        /**
         * port.
         */
        port("5432"),
        /**
         * user name.
         */
        username("crate"),
        /**
         * password.
         */
        password("");

        private final String defaultValue;

        AttrName(String defaultValue) {
            this.defaultValue = defaultValue;
        }

        @Override
        public String getKey() {
            return name();
        }

        /**
         * @return default value
         */
        public String getDefaultValue() {
            return defaultValue;
        }
    }

    /**
     * Shallow copy constructor, used by the store, attributes are a reference to
     * the attributes of 'other'.
     * 
     * @param other original store item
     */
    public ConnAttrs(StoreEntry other) {
        super(other);
    }

    /**
     * Constructor .
     * 
     * @param name name of the connection
     */
    public ConnAttrs(String name) {
        super(name);
        setAttr(AttrName.host, AttrName.host.getDefaultValue());
        setAttr(AttrName.host, AttrName.host.getDefaultValue());
        setAttr(AttrName.port, AttrName.port.getDefaultValue());
        setAttr(AttrName.username, AttrName.username.getDefaultValue());
        setAttr(AttrName.password, AttrName.password.getDefaultValue());
    }

    @Override
    public final String getKey() {
        return String.format("%s %s@%s:%s", getName(), getAttr(AttrName.username), getAttr(AttrName.host),
            getAttr(AttrName.port));
    }

    /**
     * @return connection uri
     */
    public String getUri() {
        return String.format(JDBC_DRIVER_URL_FORMAT, getHost(), getPort());
    }

    /**
     * Connection properties, includes:
     * <ul>
     * <li>user: user name</li>
     * <li>password: password</li>
     * <li>ssl: false</li>
     * <li>recvBufferSize: 1 MB</li>
     * <li>defaultRowFetchSize: 20_000</li>
     * <li>loginTimeout: 5 seconds</li>
     * <li>socketTimeout: 60 seconds);
     * <li>tcpKeepAlive", true);
     * </ul>
     * 
     * @return connection properties
     */
    public Properties loginProperties() {
        Properties props = new Properties();
        // https://jdbc.postgresql.org/documentation/head/connect.html
        props.put("user", getUsername());
        props.put("password", getPassword());
        props.put("ssl", false);
        props.put("recvBufferSize", 1024 * 1024);
        props.put("defaultRowFetchSize", SQLExecutor.MAX_BATCH_SIZE);
        props.put("loginTimeout", 20); // seconds
        props.put("socketTimeout", SQLExecutor.QUERY_EXECUTION_TIMEOUT_SECS);
        props.put("tcpKeepAlive", true);
        return props;
    }

    /**
     * Host name getter.
     * 
     * @return host name
     */
    public String getHost() {
        return getAttr(AttrName.host);
    }

    /**
     * Host name setter, defaults to "localhost" when host is null or empty.
     * 
     * @param host host name
     */
    public void setHost(String host) {
        setAttr(AttrName.host, host, AttrName.host.getDefaultValue());
    }

    /**
     * Port getter.
     * 
     * @return port
     */
    public String getPort() {
        return getAttr(AttrName.port);
    }

    /**
     * Port setter, defaults to "5432" when port is null or empty.
     * 
     * @param port port
     */
    public void setPort(String port) {
        setAttr(AttrName.port, port, AttrName.port.getDefaultValue());
    }

    /**
     * User name getter.
     * 
     * @return user name
     */
    public String getUsername() {
        return getAttr(AttrName.username);
    }

    /**
     * User name setter, defaults to "crate" when host is null or empty.
     * 
     * @param username user name
     */
    public void setUsername(String username) {
        setAttr(AttrName.username, username, AttrName.username.getDefaultValue());
    }

    /**
     * Password getter.
     * 
     * @return password
     */
    public String getPassword() {
        return getAttr(AttrName.password);
    }

    /**
     * Password setter, defaults to "" when host is null.
     * 
     * @param password password
     */
    public void setPassword(String password) {
        setAttr(AttrName.password, password, AttrName.password.getDefaultValue());
    }
}
