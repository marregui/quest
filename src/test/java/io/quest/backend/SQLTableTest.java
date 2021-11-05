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


public class SQLTableTest {

    private static final String[] COL_NAMES = {
        "Status", "Source", "Uptime"
    };
    private static final int[] COL_TYPES = {
        Types.VARCHAR, Types.VARCHAR, Types.INTEGER
    };
    private static final Object[] COL_VALUES = {
        "OK", "Entropy generator", 42
    };

    private ResultSet rs;

    @BeforeEach
    public void beforeEach() throws SQLException {
        rs = mock(ResultSet.class);
        ResultSetMetaData metadata = mock(ResultSetMetaData.class);
        when(metadata.getColumnCount()).thenReturn(COL_NAMES.length);
        when(metadata.getColumnName(eq(1))).thenReturn(COL_NAMES[0]);
        when(metadata.getColumnName(eq(2))).thenReturn(COL_NAMES[1]);
        when(metadata.getColumnName(eq(3))).thenReturn(COL_NAMES[2]);
        when(metadata.getColumnType(eq(1))).thenReturn(COL_TYPES[0]);
        when(metadata.getColumnType(eq(2))).thenReturn(COL_TYPES[1]);
        when(metadata.getColumnType(eq(3))).thenReturn(COL_TYPES[2]);
        when(rs.getMetaData()).thenReturn(metadata);
        when(rs.getObject(1)).thenReturn(COL_VALUES[0]);
        when(rs.getObject(2)).thenReturn(COL_VALUES[1]);
        when(rs.getObject(3)).thenReturn(COL_VALUES[2]);
    }

    @Test
    public void test_empty_table_no_key() {
        SQLTable table = new SQLTable(null);
        assertThat(table.getKey(), nullValue());
        assertThat(table.getColNames(), nullValue());
        assertThat(table.getColTypes(), nullValue());
        assertThat(table.size(), is(0));
        table.clear();
    }

    @Test
    public void test_extractColumnMetadata() throws SQLException {
        SQLTable table = new SQLTable(null);
        table.setColMetadata(rs);
        assertThat(table.getColNames(), is(COL_NAMES));
        assertThat(table.getColTypes(), is(COL_TYPES));
        assertThat(table.size(), is(0));
        table.clear();
        assertThat(table.getColNames(), nullValue());
        assertThat(table.getColTypes(), nullValue());
    }

    @Test
    public void test_addRow() throws SQLException {
        String rowKey = "0";
        SQLTable table = new SQLTable(null);
        table.setColMetadata(rs);
        table.addRow(rowKey, rs);
        assertThat(table.getColNames(), is(COL_NAMES));
        assertThat(table.getColTypes(), is(COL_TYPES));
        assertThat(table.size(), is(1));
        assertThat(table.getRow(0), Matchers.is(new SQLTable.Row(null, rowKey, COL_VALUES)));
        table.clear();
    }
}
