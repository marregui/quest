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

package io.quest.model;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class SQLTableRTest {
    private static final String[] colNames = {"Status", "Source", "Uptime"};
    private static final int[] colTypes = {Types.VARCHAR, Types.VARCHAR, Types.INTEGER};
    private static final Object[] colValues = {"OK", "Entropy generator", 42};

    private static final String[] expectedColNames = new String[colNames.length + 1];
    private static final int[] expectedColTypes = new int[colTypes.length + 1];
    private static final Object[] expectedColValues = new Object[colValues.length + 1];

    static {
        expectedColNames[0] = "#";
        expectedColTypes[0] = Types.ROWID;
        expectedColValues[0] = 0L;
        System.arraycopy(colNames, 0, expectedColNames, 1, colNames.length);
        System.arraycopy(colTypes, 0, expectedColTypes, 1, colTypes.length);
        System.arraycopy(colValues, 0, expectedColValues, 1, colValues.length);
    }

    private ResultSet rs;

    @BeforeEach
    public void beforeEach() throws SQLException {
        rs = mock(ResultSet.class);
        ResultSetMetaData metadata = mock(ResultSetMetaData.class);
        when(metadata.getColumnCount()).thenReturn(colNames.length);
        when(metadata.getColumnName(eq(1))).thenReturn(colNames[0]);
        when(metadata.getColumnName(eq(2))).thenReturn(colNames[1]);
        when(metadata.getColumnName(eq(3))).thenReturn(colNames[2]);
        when(metadata.getColumnType(eq(1))).thenReturn(colTypes[0]);
        when(metadata.getColumnType(eq(2))).thenReturn(colTypes[1]);
        when(metadata.getColumnType(eq(3))).thenReturn(colTypes[2]);
        when(rs.getMetaData()).thenReturn(metadata);
        when(rs.getObject(1)).thenReturn(colValues[0]);
        when(rs.getObject(2)).thenReturn(colValues[1]);
        when(rs.getObject(3)).thenReturn(colValues[2]);
    }

    @Test
    public void test_empty_table_no_key() {
        try (SQLTable table = new SQLTable(null)) {
            assertThat(table.getKey(), nullValue());
            assertThat(table.getColNames(), nullValue());
            assertThat(table.getColTypes(), nullValue());
            assertThat(table.size(), is(0));
        }
    }

    @Test
    public void test_extractColumnMetadata() throws SQLException {
        try (SQLTable table = new SQLTable(null)) {
            table.setColMetadata(rs);
            assertThat(table.getColNames(), is(expectedColNames));
            assertThat(table.getColTypes(), is(expectedColTypes));
            assertThat(table.size(), is(0));
            table.close();
            assertThat(table.getColNames(), nullValue());
            assertThat(table.getColTypes(), nullValue());
        }
    }

    @Test
    public void test_addRow() throws SQLException {
        int rowKey = 0;
        try (SQLTable table = new SQLTable(null)) {
            table.setColMetadata(rs);
            table.addRow(rowKey, rs);
            assertThat(table.getColNames(), is(expectedColNames));
            assertThat(table.getColTypes(), is(expectedColTypes));
            assertThat(table.size(), is(1));
            assertThat(table.getRow(0), Matchers.is(new SQLRow(rowKey, expectedColValues)));
        }
    }
}
