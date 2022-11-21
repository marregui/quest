#!/usr/bin/env python3
# Licensed to Miguel Arregui ("marregui") under one or more contributor
# license agreements. See the LICENSE file distributed with this work
# for additional information regarding copyright ownership. You may
# obtain a copy at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations
# under the License.
#
# Copyright 2020, Miguel Arregui a.k.a. marregui
#

import psycopg2

CONN_ATTRS = {
    "user": "admin",
    "password": "quest",
    "host": "127.0.0.1",
    "port": "8812",
    "database": "qdb",
}

TYPE_KEYWORDS = {
    "boolean",
    "byte",
    "short",
    "char",
    "int",
    "long",
    "date",
    "timestamp",
    "float",
    "double",
    "string",
    "symbol",
    "long256",
    "geohash",
    "binary",
    "null",
    "index",
    "capacity",
    "cache",
    "nocache",
    "day",
    "hour",
    "month",
    "none",
}


def format_as_java_regex_pattern(keywords):
    indent = 0
    pattern = ""
    for kw in keywords:
        pattern += f"\\\\b{kw}\\\\b|"
        indent += len(kw) + 9  # 2x quote + 2x \\b + 1x pipe
        if indent > 75:
            pattern += '"\n + "'
            indent = 0
    return f'"{pattern[:-1]}"'


if __name__ == "__main__":
    with psycopg2.connect(**CONN_ATTRS) as conn:
        with conn.cursor() as stmt_cursor:
            stmt_cursor.execute("pg_catalog.pg_get_keywords();")
            keywords = ["questdb"]
            for row in stmt_cursor.fetchall():
                kw = row[0].lower()
                if kw not in TYPE_KEYWORDS:
                    keywords.append(kw)
            print(format_as_java_regex_pattern(TYPE_KEYWORDS.union(keywords)))
