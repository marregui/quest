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