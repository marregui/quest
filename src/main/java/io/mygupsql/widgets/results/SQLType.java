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

package io.mygupsql.widgets.results;

import java.awt.Color;
import java.sql.Types;

import io.mygupsql.GTk;


/**
 * Resolves {@link java.sql.Types} to their text representation, and rendering
 * colour.
 */
final class SQLType {

    static String resolveName(int sqlType) {
        String type;
        switch (sqlType) {
            case Types.OTHER:
                type = "object";
                break;

            case Types.BOOLEAN:
                type = "boolean";
                break;

            case Types.TINYINT:
                type = "tinyint";
                break;

            case Types.SMALLINT:
                type = "smallint";
                break;

            case Types.INTEGER:
                type = "integer";
                break;

            case Types.BIGINT:
                type = "bigint";
                break;

            case Types.REAL:
                type = "real";
                break;

            case Types.DOUBLE:
                type = "double";
                break;

            case Types.DATE:
                type = "date";
                break;

            case Types.TIMESTAMP:
                type = "timestamp";
                break;

            case Types.TIMESTAMP_WITH_TIMEZONE:
                type = "timestamptz";
                break;

            case Types.TIME:
                type = "time";
                break;

            case Types.TIME_WITH_TIMEZONE:
                type = "timetz";
                break;

            case Types.ARRAY:
                type = "array";
                break;

            case Types.BLOB:
                type = "blob";
                break;

            case Types.BINARY:
                type = "binary";
                break;

            case Types.VARBINARY:
                type = "varbinary";
                break;

            case Types.CHAR:
                type = "char";
                break;

            case Types.CLOB:
                type = "clob";
                break;

            case Types.VARCHAR:
                type = "varchar";
                break;

            case Types.BIT:
                type = "bit";
                break;

            case Types.STRUCT:
                type = "struct";
                break;

            case Types.JAVA_OBJECT:
                type = "java_object";
                break;

            default:
                type = String.valueOf(sqlType);
        }
        return type;
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
