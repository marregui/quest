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

import ssl
import psycopg2
import typing


def format_as_java_regex_pattern(keywords: typing.List[str]) -> str:
    pattern = '"'
    indent = 0
    for kw in keywords:
        pattern = f'{pattern}|\\\\b{kw}\\\\b'
        indent += len(kw) + 9  # 2x quote + 2x \\b + 1x pipe
        if indent > 65:
            indent = 0
            pattern += '"\n + "'
    pattern += '"'
    return pattern


CONN_ATTRS = {
    'user': 'admin',
    'password': 'quest',
    'host': '127.0.0.1',
    'port': '8812',
    'database': 'qdb'
}

if __name__ == '__main__':
    with psycopg2.connect(**CONN_ATTRS) as conn:
        with conn.cursor() as stmt_cursor:
            stmt_cursor.execute(f'pg_catalog.pg_get_keywords();')
            print(format_as_java_regex_pattern([
                'boolean', 'byte', 'short', 'char',
                'int', 'long', 'date', 'timestamp',
                'float', 'double', 'string', 'symbol',
                'long256', 'geohash', 'binary', 'null'
            ]))
            print("=" * 100)
            keywords = ['questdb']
            for row in stmt_cursor.fetchall():
                keywords.append(row[0].lower())
            print(format_as_java_regex_pattern(keywords))
