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

crate_admin_keywords = set([
    'abs', 'absolute', 'action', 'add', 'after', 'alias', 'all', 'allocate',
    'alter', 'always', 'analyzer', 'and', 'any', 'are', 'array', 'array_agg',
    'array_max_cardinality', 'as', 'asc', 'asensitive', 'assertion',
    'asymmetric', 'at', 'atomic', 'authorization', 'avg', 'before', 'begin',
    'begin_frame', 'begin_partition', 'between', 'bigint', 'binary', 'bit',
    'bit_length', 'blob', 'boolean', 'both', 'breadth', 'by', 'byte', 'call',
    'called', 'cardinality', 'cascade', 'cascaded', 'case', 'cast',
    'catalog', 'catalogs', 'ceil', 'ceiling', 'char', 'char_filters',
    'char_length', 'character', 'character_length', 'check', 'clob', 'close',
    'clustered', 'coalesce', 'collate', 'collation', 'collect', 'column',
    'columns', 'commit', 'condition', 'connect', 'connection', 'constraint',
    'constraints', 'constructor', 'contains', 'continue', 'convert', 'copy',
    'corr', 'corresponding', 'count', 'covar_pop', 'covar_samp', 'create',
    'cross', 'cube', 'cume_dist', 'current', 'current_catalog',
    'current_date', 'current_path', 'current_role', 'current_row',
    'current_schema', 'current_time', 'current_timestamp', 'current_user',
    'cursor', 'cycle', 'data', 'date', 'day', 'deallocate', 'dec', 'decimal',
    'declare', 'default', 'deferrable', 'deferred', 'delete', 'deny', 'dense_rank',
    'depth', 'deref', 'desc', 'describe', 'descriptor', 'deterministic',
    'diagnostics', 'directory', 'disconnect', 'distinct', 'distributed',
    'do', 'domain', 'double', 'drop', 'duplicate', 'dynamic', 'each',
    'element', 'else', 'elseif', 'end', 'end_exec', 'end_frame',
    'end_partition', 'equals', 'escape', 'every', 'except', 'exception',
    'exec', 'execute', 'exists', 'exit', 'explain', 'extends', 'external',
    'extract', 'false', 'fetch', 'filter', 'first', 'first_value', 'float',
    'for', 'foreign', 'format', 'found', 'frame_row', 'free', 'from', 'full',
    'fulltext', 'function', 'functions', 'fusion', 'general', 'generated',
    'geo_point', 'geo_shape', 'get', 'global', 'go', 'goto', 'grant',
    'group', 'grouping', 'groups', 'handler', 'having', 'hold', 'hour',
    'identity', 'if', 'ignored', 'immediate', 'in', 'index', 'indicator',
    'initially', 'inner', 'inout', 'input', 'insensitive', 'insert', 'int',
    'integer', 'intersect', 'intersection', 'interval', 'into', 'ip', 'is',
    'isolation', 'iterate', 'join', 'key', 'kill', 'language', 'large',
    'last', 'last_value', 'lateral', 'lead', 'leading', 'leave', 'left',
    'level', 'like', 'like_regex', 'limit', 'ln', 'local', 'localtime',
    'localtimestamp', 'locator', 'long', 'loop', 'lower', 'map', 'match',
    'max', 'member', 'merge', 'method', 'min', 'minute', 'mod', 'modifies',
    'module', 'month', 'multiset', 'names', 'national', 'natural', 'nchar',
    'nclob', 'new', 'next', 'no', 'none', 'normalize', 'not', 'nth_value',
    'ntile', 'null', 'nullif', 'nulls', 'numeric', 'object', 'octet_length',
    'of', 'off', 'offset', 'old', 'on', 'only', 'open', 'optimize', 'option',
    'or', 'order', 'ordinality', 'out', 'outer', 'output', 'over',
    'overlaps', 'overlay', 'pad', 'parameter', 'partial', 'partition',
    'partitioned', 'partitions', 'path', 'percent', 'percent_rank',
    'percentile_cont', 'percentile_disc', 'period', 'persistent', 'plain',
    'portion', 'position', 'position_regex', 'power', 'precedes',
    'preceding', 'precision', 'prepare', 'preserve', 'primary',
    'primary key', 'prior', 'privileges', 'procedure', 'public', 'range',
    'rank', 'read', 'reads', 'real', 'recursive', 'ref', 'references',
    'referencing', 'refresh', 'regr_avgx', 'regr_avgy', 'regr_count',
    'regr_intercept', 'regr_r2', 'regr_slope', 'regr_sxx',
    'regr_sxyregr_syy', 'relative', 'release', 'rename', 'repeat', 'repository',
    'reset', 'resignal', 'restore', 'restrict', 'result', 'return',
    'returns', 'revoke', 'right', 'role', 'rollback', 'rollup', 'routine',
    'row', 'row_number', 'rows', 'savepoint', 'schema', 'schemas', 'scope',
    'scroll', 'search', 'second', 'section', 'select', 'sensitive',
    'session', 'session_user', 'set', 'sets', 'shards', 'short', 'show',
    'signal', 'similar', 'size', 'smallint', 'snapshot', 'some', 'space',
    'specific', 'specifictype', 'sql', 'sqlcode', 'sqlerror', 'sqlexception',
    'sqlstate', 'sqlwarning', 'sqrt', 'start', 'state', 'static',
    'stddev_pop', 'stddev_samp', 'stratify', 'stratify', 'strict', 'string',
    'submultiset', 'substring', 'substring_regex', 'succeedsblob', 'sum',
    'symmetric', 'system', 'system_time', 'system_user', 'table', 'tables',
    'tablesample', 'temporary', 'text', 'then', 'time', 'timestamp',
    'timezone_hour', 'timezone_minute', 'to', 'token_filters', 'tokenizer',
    'trailing', 'transaction', 'transient', 'translate', 'translate_regex',
    'translation', 'treat', 'trigger', 'trim', 'trim_array', 'true',
    'truncate', 'try_cast', 'type', 'uescape', 'unbounded', 'under', 'undo',
    'union', 'unique', 'unknown', 'unnest', 'until', 'update', 'upper',
    'usage', 'user', 'using', 'value', 'value_of', 'values', 'var_pop',
    'var_samp', 'varbinary', 'varchar', 'varying', 'versioning', 'view',
    'when', 'whenever', 'where', 'while', 'width_bucket', 'window', 'with',
    'within', 'without', 'work', 'write', 'year', 'zone'
])

crate_symbolic_keywords = set(map(lambda t: t.lower(), (
    "SELECT",
    "FROM", "TO", "AS", "AT", "ALL", "ANY", "SOME", "DEALLOCATE", "DIRECTORY",
    "DISTINCT", "WHERE", "GROUP", "BY", "ORDER", "HAVING", "LIMIT", "OFFSET",
    "OR", "AND", "IN", "NOT", "EXISTS", "BETWEEN", "LIKE", "ILIKE", "IS",
    "NULL", "TRUE", "FALSE", "NULLS", "FIRST", "LAST", "ESCAPE", "ASC", "DESC",
    "SUBSTRING", "TRIM", "LEADING", "TRAILING", "BOTH", "FOR", "TIME", "ZONE",
    "YEAR", "MONTH", "DAY", "HOUR", "MINUTE", "SECOND", "CURRENT_DATE", "CURRENT_TIME",
    "CURRENT_TIMESTAMP", "CURRENT_SCHEMA", "CURRENT_USER", "SESSION_USER",
    "EXTRACT", "CASE", "WHEN", "THEN", "ELSE", "END", "IF", "INTERVAL", "JOIN",
    "CROSS", "OUTER", "INNER", "LEFT", "RIGHT", "FULL", "NATURAL", "USING",
    "ON", "OVER", "WINDOW", "PARTITION", "PROMOTE", "RANGE", "ROWS", "UNBOUNDED",
    "PRECEDING", "FOLLOWING", "CURRENT", "ROW", "WITH", "WITHOUT", "RECURSIVE",
    "CREATE", "BLOB", "TABLE", "SWAP", "GC", "DANGLING", "ARTIFACTS", "DECOMMISSION",
    "CLUSTER", "REPOSITORY", "SNAPSHOT", "ALTER", "KILL", "ONLY", "ADD",
    "COLUMN", "OPEN", "CLOSE", "RENAME", "REROUTE", "MOVE", "SHARD", "ALLOCATE",
    "REPLICA", "CANCEL", "RETRY", "FAILED", "BOOLEAN", "BYTE", "SHORT", "INTEGER",
    "INT", "LONG", "FLOAT", "DOUBLE", "PRECISION", "TIMESTAMP", "IP", "OBJECT",
    "STRING_TYPE", "GEO_POINT", "GEO_SHAPE", "GLOBAL", "SESSION", "LOCAL",
    "LICENSE", "BEGIN", "COMMIT", "WORK", "TRANSACTION", "TRANSACTION_ISOLATION",
    "CHARACTERISTICS", "ISOLATION", "LEVEL", "SERIALIZABLE", "REPEATABLE",
    "COMMITTED", "UNCOMMITTED", "READ", "WRITE", "DEFERRABLE", "RETURNS",
    "CALLED", "REPLACE", "FUNCTION", "LANGUAGE", "INPUT", "ANALYZE", "CONSTRAINT",
    "DESCRIBE", "EXPLAIN", "FORMAT", "TYPE", "TEXT", "GRAPHVIZ", "LOGICAL",
    "DISTRIBUTED", "CAST", "TRY_CAST", "SHOW", "TABLES", "SCHEMAS", "CATALOGS",
    "COLUMNS", "PARTITIONS", "FUNCTIONS", "MATERIALIZED", "VIEW", "OPTIMIZE",
    "REFRESH", "RESTORE", "DROP", "ALIAS", "UNION", "EXCEPT", "INTERSECT",
    "SYSTEM", "BERNOULLI", "TABLESAMPLE", "STRATIFY", "INSERT", "INTO", "VALUES",
    "DELETE", "UPDATE", "KEY", "DUPLICATE", "CONFLICT", "DO", "NOTHING",
    "SET", "RESET", "DEFAULT", "COPY", "CLUSTERED", "SHARDS", "PRIMARY_KEY",
    "OFF", "FULLTEXT", "FILTER", "PLAIN", "INDEX", "STORAGE", "DYNAMIC",
    "STRICT", "IGNORED", "ARRAY", "ANALYZER", "EXTENDS", "TOKENIZER", "TOKEN_FILTERS",
    "CHAR_FILTERS", "PARTITIONED", "PREPARE", "TRANSIENT", "PERSISTENT",
    "MATCH", "GENERATED", "ALWAYS", "USER", "GRANT", "DENY", "REVOKE", "PRIVILEGES",
    "SCHEMA", "RETURN", "SUMMARY", "EQ", "NEQ", "LT", "LTE", "GT", "GTE",
    "LLT", "REGEX_MATCH", "REGEX_NO_MATCH", "REGEX_MATCH_CI", "REGEX_NO_MATCH_CI",
    "PLUS", "MINUS", "ASTERISK", "SLASH", "PERCENT", "CONCAT", "CAST_OPERATOR",
    "SEMICOLON", "STRING", "ESCAPED_STRING", "INTEGER_VALUE", "DECIMAL_VALUE",
    "IDENTIFIER", "DIGIT_IDENTIFIER", "QUOTED_IDENTIFIER", "BACKQUOTED_IDENTIFIER",
    "COLON_IDENT", "COMMENT", "WS", "UNRECOGNIZED",
)))

# crate_admin_keywords = set(("copy&paste keywords crate-admin/app/scripts/controllers/console.js"))
# crate_symbolic_keywords = set(("copy&paste keywords _SYMBOLIC_NAMES crate SqlBaseLexer.java"))
new_terms = crate_symbolic_keywords.difference(crate_admin_keywords)
final_terms = crate_admin_keywords.union(new_terms)

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
    show_pattern(final_terms)

