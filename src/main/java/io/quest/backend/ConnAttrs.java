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

package io.quest.backend;

import java.util.Properties;

import io.quest.QuestMain;
import io.quest.common.WithKey;


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
        port("8812"),
        /**
         * user name.
         */
        username("admin"),
        /**
         * password.
         */
        password("quest"),
        /**
         * isDefault.
         */
        isDefault(String.valueOf(false));

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
        setAttr(AttrName.port, AttrName.port.getDefaultValue());
        setAttr(AttrName.username, AttrName.username.getDefaultValue());
        setAttr(AttrName.password, AttrName.password.getDefaultValue());
        setAttr(AttrName.isDefault, AttrName.isDefault.getDefaultValue());
    }

    /**
     * Constructor .
     *
     * @param name of the connection
     * @param host of the connection
     * @param port of the connection
     * @param username of the connection
     * @param password of the connection
     */
    protected ConnAttrs(String name, String host, String port, String username, String password) {
        super(name);
        setAttr(AttrName.host, host);
        setAttr(AttrName.port, port);
        setAttr(AttrName.username, username);
        setAttr(AttrName.password, password);
        setAttr(AttrName.isDefault, AttrName.isDefault.getDefaultValue());
    }

    @Override
    public final String getKey() {
        return String.format("%s %s@%s:%s",
                getName(),
                getAttr(AttrName.username),
                getAttr(AttrName.host),
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
     * <li>sslmode: disable</li>
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

        // Specifies the name of the application that is using the connection.
        // This allows a database administrator to see what applications are
        // connected to the server and what resources they are using through
        // views like pgstatactivity.
        props.put("ApplicationName", QuestMain.NAME + "-" + QuestMain.VERSION);

        // Connect using SSL. The server must have been compiled with SSL
        // support. This property does not need a value associated with it.
        // The mere presence of it specifies a SSL connection. However, for
        // compatibility with future versions, the value "true" is preferred.
        // For more information see Chapter 4, Using SSL. Setting up the
        // certificates and keys for ssl connection can be tricky see The test
        // documentation for detailed examples.
        props.put("ssl", false);

        // possible values include disable, allow, prefer, require, verify-ca
        // and verify-full . require, allow and prefer all default to a non
        // validating SSL factory and do not check the validity of the certificate
        // or the host name. verify-ca validates the certificate, but does not
        // verify the hostname. verify-full will validate that the certificate
        // is correct and verify the host connected to has the same hostname as
        // the certificate. Default is prefer
        // Setting these will necessitate storing the server certificate on the
        // client machine see "Configuring the client" for details.
        props.setProperty("sslmode", "disable");

        // Sets SO_RCVBUF on the connection stream
        props.put("receiveBufferSize", 1024 * 1024);

        // Sets SO_SNDBUF on the connection stream
        props.put("sendBufferSize", 1024 * 1024);

        // Determine the number of rows fetched in ResultSet by one fetch with
        // trip to the database. Limiting the number of rows are fetch with each
        // trip to the database allow avoids unnecessary memory consumption and
        // as a consequence OutOfMemoryException.
        // The default is zero, meaning that in ResultSet will be fetch all rows
        // at once. Negative number is not available.
        props.put("defaultRowFetchSize", SQLExecutor.MAX_BATCH_SIZE);

        // Specify how long to wait for establishment of a database connection.
        // The timeout is specified in seconds.
        props.put("loginTimeout", 20); // seconds

        // The timeout value used for socket read operations. If reading from
        // the server takes longer than this value, the connection is closed.
        // This can be used as both a brute force global query timeout and a
        // method of detecting network problems. The timeout is specified in
        // seconds and a value of zero means that it is disabled.
        props.put("socketTimeout", SQLExecutor.QUERY_EXECUTION_TIMEOUT_SECS);

        // Enable or disable TCP keep-alive probe. The default is false.
        props.put("tcpKeepAlive", true);

        // Specifies the maximum size (in megabytes) of fields to be cached
        // per connection. A value of 0 disables the cache.
        props.put("databaseMetadataCacheFieldsMiB", 0);

        // Specify the type to use when binding PreparedStatement parameters
        // set via setString(). If stringtype is set to VARCHAR (the default),
        // such parameters will be sent to the server as varchar parameters.
        // If stringtype is set to unspecified, parameters will be sent to the
        // server as untyped values, and the server will attempt to infer an
        // appropriate type. This is useful if you have an existing application
        // that uses setString() to set parameters that are actually some other
        // type, such as integers, and you are unable to change the application
        // to use an appropriate method such as setInt().
        props.put("stringtype", "unspecified");

        // This will change batch inserts from
        // insert into foo (col1, col2, col3) values (1,2,3)
        // into
        // insert into foo (col1, col2, col3) values (1,2,3), (4,5,6)
        // this provides 2-3x performance improvement
        props.put("reWriteBatchedInserts ", "true");
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

    /**
     * Password getter.
     *
     * @return is default
     */
    public boolean isDefault() {
        return Boolean.parseBoolean(getAttr(AttrName.isDefault));
    }

    /**
     * Default setter, defaults to false.
     *
     * @param isDefault is default
     */
    public void setDefault(boolean isDefault) {
        setAttr(AttrName.isDefault, String.valueOf(isDefault), AttrName.isDefault.getDefaultValue());
    }
}
