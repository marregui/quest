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

package io.quest.frontend.results;

import java.awt.Color;
import java.sql.Types;

import io.quest.frontend.GTk;


/**
 * Resolves {@link java.sql.Types} to their text representation, and rendering color.
 */
final class SQLType {

    static String resolveName(int sqlType) {
        String type;
        switch (sqlType) {
            case Types.OTHER:
                type = "OBJECT";
                break;
            case Types.BOOLEAN:
                type = "BOOLEAN";
                break;
            case Types.TINYINT:
                type = "TINYINT";
                break;
            case Types.SMALLINT:
                type = "SMALLINT";
                break;
            case Types.INTEGER:
                type = "INTEGER";
                break;
            case Types.BIGINT:
                type = "BIGINT";
                break;
            case Types.REAL:
                type = "REAL";
                break;
            case Types.DOUBLE:
                type = "DOUBLE";
                break;
            case Types.DATE:
                type = "DATE";
                break;
            case Types.TIMESTAMP:
                type = "TIMESTAMP";
                break;
            case Types.TIMESTAMP_WITH_TIMEZONE:
                type = "TIMESTAMPTZ";
                break;
            case Types.TIME:
                type = "TIME";
                break;
            case Types.TIME_WITH_TIMEZONE:
                type = "TIMETZ";
                break;
            case Types.ARRAY:
                type = "ARRAY";
                break;
            case Types.BLOB:
                type = "BLOB";
                break;
            case Types.BINARY:
                type = "BINARY";
                break;
            case Types.VARBINARY:
                type = "VARBINARY";
                break;
            case Types.CHAR:
                type = "CHAR";
                break;
            case Types.CLOB:
                type = "CLOB";
                break;
            case Types.VARCHAR:
                type = "VARCHAR";
                break;
            case Types.BIT:
                type = "BIT";
                break;
            case Types.STRUCT:
                type = "STRUCT";
                break;
            case Types.JAVA_OBJECT:
                type = "JAVA_OBJECT";
                break;
            case Types.ROWID:
                type = "";
                break;
            default:
                type = String.valueOf(sqlType);
        }
        return type;
    }

    static int resolveColWidth(String name, int sqlType) {
        final int width;
        switch (sqlType) {
            case Types.BIT:
            case Types.BOOLEAN:
            case Types.CHAR:
            case Types.ROWID:
            case Types.SMALLINT:
                width = 100;
                break;
            case Types.INTEGER:
                width = 120;
                break;
            case Types.DATE:
            case Types.TIME:
            case Types.BIGINT:
                width = 200;
                break;
            case Types.TIMESTAMP:
            case Types.DOUBLE:
            case Types.REAL:
                width = 250;
                break;
            case Types.BINARY:
                width = 400;
                break;
            case Types.VARCHAR:
                width = 620;
                break;
            default:
                width = 150;
        }
        int nameWidth = 15 * (name.length() + resolveName(sqlType).length());
        return Math.max(width, nameWidth);
    }

    static Color resolveColor(int sqlType) {
        Color color = Color.MAGENTA;
        switch (sqlType) {
            case Types.OTHER:
                color = Color.ORANGE;
                break;
            case Types.BOOLEAN:
                color = BLUE_GREENISH_COLOR;
                break;
            case Types.TINYINT:
            case Types.SMALLINT:
            case Types.INTEGER:
            case Types.BIGINT:
                color = OLIVE_COLOR;
                break;
            case Types.REAL:
            case Types.DOUBLE:
                color = Color.GREEN;
                break;
            case Types.TIMESTAMP:
            case Types.TIMESTAMP_WITH_TIMEZONE:
                color = CYAN_DULL_COLOR;
                break;
            case Types.VARCHAR:
                color = GTk.APP_THEME_COLOR;
                break;
        }
        return color;
    }

    private static final Color BLUE_GREENISH_COLOR = new Color(0, 112, 112); // blue-greenish
    private static final Color OLIVE_COLOR = new Color(140, 140, 0); // olive
    private static final Color CYAN_DULL_COLOR = new Color(0, 168, 188); // cyan dull

    private SQLType() {
        throw new IllegalStateException("not meant to me instantiated");
    }
}
