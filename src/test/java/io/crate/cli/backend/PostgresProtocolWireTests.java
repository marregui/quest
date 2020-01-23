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

package io.crate.cli.backend;

import java.sql.Connection;
import java.sql.ResultSet;
//import io.crate.shade.org.postgresql.jdbc.PgResultSetMetaData;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.Locale;


public class PostgresProtocolWireTests {

    public static void main(String[] args) throws Exception {
        SQLConnection conn = new SQLConnection("test");
        conn.setHost("localhost");
        conn.setPort("5432");
        conn.setUsername("crate");
        conn.setPassword("");
        try (Connection connection = conn.open()) {
            connection.setAutoCommit(true);
            if (connection.isClosed()) {
                System.out.println("Connection is not valid");
                return;
            }
            try(Statement stmt = connection.createStatement()) {
                boolean checkResults = stmt.execute("show tables");
                if (checkResults) {
                    ResultSet rs = stmt.getResultSet();
                    while(rs.next()) {
                        ResultSetMetaData metaData = rs.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        for (int i = 1; i <= columnCount; i++) {
                            System.out.printf(
                                    Locale.ENGLISH,
                                    ">> col %d: %s: %s\n",
                                    i,
                                    metaData.getColumnName(i),
                                    rs.getObject(i));
                        }
                    }
                }
            }
        }
    }
}