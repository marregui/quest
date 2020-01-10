package io.crate.cli.connections;

import io.crate.shade.org.postgresql.jdbc.PgResultSetMetaData;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;


public class PostgresProtocolWireTests {


    public static void main(String[] args) throws Exception {
        SQLConnection conn = new SQLConnection("test");
        conn.setHost("localhost");
        conn.setPort("5432");
        conn.setUsername("crate");
        conn.setPassword("");
        try (Connection connection = conn.open()) {
            connection.setAutoCommit(true);
            try(Statement stmt = connection.createStatement()) {
                boolean checkResults = stmt.execute("show tables");
                if (checkResults) {
                    ResultSet rs = stmt.getResultSet();
                    while(rs.next()) {
                        PgResultSetMetaData metaData = (PgResultSetMetaData) rs.getMetaData();
                        int columnCount = metaData.getColumnCount();
                        for (int i = 1; i <= columnCount; i++) {
                            System.out.printf(">> %d: %s -> %s\n", i, metaData.getColumnName(i), rs.getObject(i));
                        }
                    }
                }
            }
        }
    }
}