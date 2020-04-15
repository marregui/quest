##
# Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
# license agreements.  See the NOTICE file distributed with this work for
# additional information regarding copyright ownership.  Crate licenses
# this file to you under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.  You may
# obtain a copy of the License at
#
#  http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
# WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
# License for the specific language governing permissions and limitations
# under the License.
#
# However, if you have executed another commercial license agreement
# with Crate these terms will supersede the license and you may use the
# software solely pursuant to the terms of the relevant commercial agreement.
##

w = ["'SELECT'", "'FROM'", "'TO'", "'AS'", "'AT'", "'ALL'",
     "'ANY'", "'SOME'", "'DEALLOCATE'", "'DIRECTORY'", "'DISTINCT'", "'WHERE'",
     "'GROUP'", "'BY'", "'ORDER'", "'HAVING'", "'LIMIT'", "'OFFSET'", "'OR'",
     "'AND'", "'IN'", "'NOT'", "'EXISTS'", "'BETWEEN'", "'LIKE'", "'ILIKE'",
     "'IS'", "'NULL'", "'TRUE'", "'FALSE'", "'NULLS'", "'FIRST'", "'LAST'",
     "'ESCAPE'", "'ASC'", "'DESC'", "'SUBSTRING'", "'TRIM'", "'LEADING'",
     "'TRAILING'", "'BOTH'", "'FOR'", "'TIME'", "'ZONE'", "'YEAR'", "'MONTH'",
     "'DAY'", "'HOUR'", "'MINUTE'", "'SECOND'", "'CURRENT_DATE'", "'CURRENT_TIME'",
     "'CURRENT_TIMESTAMP'", "'CURRENT_SCHEMA'", "'CURRENT_USER'", "'SESSION_USER'",
     "'EXTRACT'", "'CASE'", "'WHEN'", "'THEN'", "'ELSE'", "'END'", "'IF'",
     "'INTERVAL'", "'JOIN'", "'CROSS'", "'OUTER'", "'INNER'", "'LEFT'", "'RIGHT'",
     "'FULL'", "'NATURAL'", "'USING'", "'ON'", "'OVER'", "'WINDOW'", "'PARTITION'",
     "'PROMOTE'", "'RANGE'", "'ROWS'", "'UNBOUNDED'", "'PRECEDING'", "'FOLLOWING'",
     "'CURRENT'", "'ROW'", "'WITH'", "'WITHOUT'", "'RECURSIVE'", "'CREATE'",
     "'BLOB'", "'TABLE'", "'SWAP'", "'GC'", "'DANGLING'", "'ARTIFACTS'", "'DECOMMISSION'",
     "'CLUSTER'", "'REPOSITORY'", "'SNAPSHOT'", "'ALTER'", "'KILL'", "'ONLY'",
     "'ADD'", "'COLUMN'", "'OPEN'", "'CLOSE'", "'RENAME'", "'REROUTE'", "'MOVE'",
     "'SHARD'", "'ALLOCATE'", "'REPLICA'", "'CANCEL'", "'RETRY'", "'FAILED'",
     "'BOOLEAN'", "'BYTE'", "'SHORT'", "'INTEGER'", "'INT'", "'LONG'", "'FLOAT'",
     "'DOUBLE'", "'PRECISION'", "'TIMESTAMP'", "'IP'", "'OBJECT'", "'STRING'",
     "'GEO_POINT'", "'GEO_SHAPE'", "'GLOBAL'", "'SESSION'", "'LOCAL'", "'LICENSE'",
     "'BEGIN'", "'COMMIT'", "'WORK'", "'TRANSACTION'", "'TRANSACTION_ISOLATION'",
     "'CHARACTERISTICS'", "'ISOLATION'", "'LEVEL'", "'SERIALIZABLE'", "'REPEATABLE'",
     "'COMMITTED'", "'UNCOMMITTED'", "'READ'", "'WRITE'", "'DEFERRABLE'",
     "'RETURNS'", "'CALLED'", "'REPLACE'", "'FUNCTION'", "'LANGUAGE'", "'INPUT'",
     "'ANALYZE'", "'CONSTRAINT'", "'CHECK'", "'DESCRIBE'", "'EXPLAIN'", "'FORMAT'",
     "'TYPE'", "'TEXT'", "'GRAPHVIZ'", "'LOGICAL'", "'DISTRIBUTED'", "'CAST'",
     "'TRY_CAST'", "'SHOW'", "'TABLES'", "'SCHEMAS'", "'CATALOGS'", "'COLUMNS'",
     "'PARTITIONS'", "'FUNCTIONS'", "'MATERIALIZED'", "'VIEW'", "'OPTIMIZE'",
     "'REFRESH'", "'RESTORE'", "'DROP'", "'ALIAS'", "'UNION'", "'EXCEPT'",
     "'INTERSECT'", "'SYSTEM'", "'BERNOULLI'", "'TABLESAMPLE'", "'STRATIFY'",
     "'INSERT'", "'INTO'", "'VALUES'", "'DELETE'", "'UPDATE'", "'KEY'", "'DUPLICATE'",
     "'CONFLICT'", "'DO'", "'NOTHING'", "'SET'", "'RESET'", "'DEFAULT'", "'COPY'",
     "'CLUSTERED'", "'SHARDS'", "'PRIMARY KEY'", "'OFF'", "'FULLTEXT'", "'FILTER'",
     "'PLAIN'", "'INDEX'", "'STORAGE'", "'RETURNING'", "'DYNAMIC'", "'STRICT'",
     "'IGNORED'", "'ARRAY'", "'ANALYZER'", "'EXTENDS'", "'TOKENIZER'", "'TOKEN_FILTERS'",
     "'CHAR_FILTERS'", "'PARTITIONED'", "'PREPARE'", "'TRANSIENT'", "'PERSISTENT'",
     "'MATCH'", "'GENERATED'", "'ALWAYS'", "'USER'", "'GRANT'", "'DENY'",
     "'REVOKE'", "'PRIVILEGES'", "'SCHEMA'", "'RETURN'", "'SUMMARY'",
     "SELECT", "FROM", "TO", "AS", "AT", "ALL", "ANY", "SOME", "DEALLOCATE",
     "DIRECTORY", "DISTINCT", "WHERE", "GROUP", "BY", "ORDER", "HAVING", "LIMIT",
     "OFFSET", "OR", "AND", "IN", "NOT", "EXISTS", "BETWEEN", "LIKE", "ILIKE",
     "IS", "NULL", "TRUE", "FALSE", "NULLS", "FIRST", "LAST", "ESCAPE", "ASC",
     "DESC", "SUBSTRING", "TRIM", "LEADING", "TRAILING", "BOTH", "FOR", "TIME",
     "ZONE", "YEAR", "MONTH", "DAY", "HOUR", "MINUTE", "SECOND", "CURRENT_DATE",
     "CURRENT_TIME", "CURRENT_TIMESTAMP", "CURRENT_SCHEMA", "CURRENT_USER",
     "SESSION_USER", "EXTRACT", "CASE", "WHEN", "THEN", "ELSE", "END", "IF",
     "INTERVAL", "JOIN", "CROSS", "OUTER", "INNER", "LEFT", "RIGHT", "FULL",
     "NATURAL", "USING", "ON", "OVER", "WINDOW", "PARTITION", "PROMOTE", "RANGE",
     "ROWS", "UNBOUNDED", "PRECEDING", "FOLLOWING", "CURRENT", "ROW", "WITH",
     "WITHOUT", "RECURSIVE", "CREATE", "BLOB", "TABLE", "SWAP", "GC", "DANGLING",
     "ARTIFACTS", "DECOMMISSION", "CLUSTER", "REPOSITORY", "SNAPSHOT", "ALTER",
     "KILL", "ONLY", "ADD", "COLUMN", "OPEN", "CLOSE", "RENAME", "REROUTE",
     "MOVE", "SHARD", "ALLOCATE", "REPLICA", "CANCEL", "RETRY", "FAILED",
     "BOOLEAN", "BYTE", "SHORT", "INTEGER", "INT", "LONG", "FLOAT", "DOUBLE",
     "PRECISION", "TIMESTAMP", "IP", "OBJECT", "STRING_TYPE", "GEO_POINT",
     "GEO_SHAPE", "GLOBAL", "SESSION", "LOCAL", "LICENSE", "BEGIN", "COMMIT",
     "WORK", "TRANSACTION", "TRANSACTION_ISOLATION", "CHARACTERISTICS", "ISOLATION",
     "LEVEL", "SERIALIZABLE", "REPEATABLE", "COMMITTED", "UNCOMMITTED", "READ",
     "WRITE", "DEFERRABLE", "RETURNS", "CALLED", "REPLACE", "FUNCTION", "LANGUAGE",
     "INPUT", "ANALYZE", "CONSTRAINT", "CHECK", "DESCRIBE", "EXPLAIN", "FORMAT",
     "TYPE", "TEXT", "GRAPHVIZ", "LOGICAL", "DISTRIBUTED", "CAST", "TRY_CAST",
     "SHOW", "TABLES", "SCHEMAS", "CATALOGS", "COLUMNS", "PARTITIONS", "FUNCTIONS",
     "MATERIALIZED", "VIEW", "OPTIMIZE", "REFRESH", "RESTORE", "DROP", "ALIAS",
     "UNION", "EXCEPT", "INTERSECT", "SYSTEM", "BERNOULLI", "TABLESAMPLE",
     "STRATIFY", "INSERT", "INTO", "VALUES", "DELETE", "UPDATE", "KEY", "DUPLICATE",
     "CONFLICT", "DO", "NOTHING", "SET", "RESET", "DEFAULT", "COPY", "CLUSTERED",
     "SHARDS", "PRIMARY_KEY", "OFF", "FULLTEXT", "FILTER", "PLAIN", "INDEX",
     "STORAGE", "RETURNING", "DYNAMIC", "STRICT", "IGNORED", "ARRAY", "ANALYZER",
     "EXTENDS", "TOKENIZER", "TOKEN_FILTERS", "CHAR_FILTERS", "PARTITIONED",
     "PREPARE", "TRANSIENT", "PERSISTENT", "MATCH", "GENERATED", "ALWAYS",
     "USER", "GRANT", "DENY", "REVOKE", "PRIVILEGES", "SCHEMA", "RETURN",
     "SUMMARY", "EQ", "NEQ", "LT", "LTE", "GT", "GTE", "LLT", "REGEX_MATCH",
     "REGEX_NO_MATCH", "REGEX_MATCH_CI", "REGEX_NO_MATCH_CI", "PLUS", "MINUS",
     "ASTERISK", "SLASH", "PERCENT", "CONCAT", "CAST_OPERATOR", "SEMICOLON",
     "STRING", "ESCAPED_STRING", "INTEGER_VALUE", "DECIMAL_VALUE", "IDENTIFIER",
     "DIGIT_IDENTIFIER", "QUOTED_IDENTIFIER", "BACKQUOTED_IDENTIFIER", "COLON_IDENT",
     "COMMENT", "WS", "UNRECOGNIZED"]

# crate_symbolic_keywords = set(("copy&paste keywords _SYMBOLIC_NAMES crate SqlBaseLexer.java"))

def show(title, col, max_chars_in_line=60):
    sorted_terms = list(col)
    sorted_terms.sort()
    print("{}:".format(title))
    line_offset = 1
    for t in sorted_terms:
        print("\"{}\", ".format(t), end='')
        line_offset += 4 + len(t)
        if line_offset > max_chars_in_line:
            line_offset = 1
            print("")
    print("\n{}".format("=" * 100))


def show_pattern(terms, max_chars_in_line=60):
    sorted_terms = list(terms)
    sorted_terms.sort()
    pattern = "\"\\\\bcrate\\\\b"
    line_len = 1
    for t in sorted_terms:
        pattern = "{}|\\\\b{}\\\\b".format(pattern, t)
        line_len += len(t) + 5
        if line_len > max_chars_in_line:
            line_len = 1
            pattern += "\"\n + \""
    pattern += "\""
    print(pattern)


if __name__ == '__main__':
    # show("crate_admin_keywords", crate_admin_keywords)
    # show("crate_symbolic_keywords", crate_symbolic_keywords)
    # show("new_terms", new_terms)
    # show("final_terms", final_terms)
    # show_pattern(final_terms)
    terms = set()
    for t in w:
        if t and t[0] == "'" and t[-1] == "'":
            terms.add(t[1:-1].lower())
        else:
            terms.add(t.lower())
    show_pattern(terms)
