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
import urllib.request

# The file behind GRAMMAR_DEFINITION_URI is CrateDB's antlr4 grammar definition. 
# About half way through the file we find the 'nonReserved' block, which is 
# followed by keyword definitions.
# 
# Each keyword is defined in its own '\n' terminated line with format: 
# "TOKEN:'DEFINITION';". For our purposes, a keyword is the DEFINITION string, 
# unquoted and lower cased, but only if it is all alphanumeric.
#
# extract_keywords_from_crate_sql_grammar() does the extraction of keywords from 
# the file.
#
# format_as_java_regex_pattern(keywords) takes the extracted keywords and formats 
# them into a string that can be passed to java.util.regex.Pattern.compile as a 
# parameter. 
# This pattern is used by: marregui.crate.cli.widgets.MessagePane.HighlightKeywordsFilter
 
GRAMMAR_DEFINITION_URI = "https://raw.githubusercontent.com/crate/crate/master/libs/sql-parser/src/main/antlr/SqlBase.g4"


def extract_keywords_from_crate_sql_grammar():
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE
    with urllib.request.urlopen(GRAMMAR_DEFINITION_URI, context=ctx) as response:
        g4Bytes = response.read()
    g4Str = g4Bytes.decode("UTF-8")
    start = g4Str.rfind("nonReserved")
    if start < 0: raise ValueError("nonReserved block not found")
    keywords = set()           
    definitions = g4Str[g4Str.index(";", start) + 1:]
    for line in definitions.split("\n"):
        line = line.strip()
        if line and ":" in line and line.endswith(";"):
            parts = line[:-1].split(":")
            if len(parts) == 2:
                val = parts[1].strip()
                if val and val[0] == "'" and val[-1] == "'":
                    val = val[1:-1]
                if val.isalnum():    
                    keywords.add(val.lower())
    keywords = list(keywords)
    keywords.sort()                
    return keywords                


def format_as_java_regex_pattern(keywords):
    pattern = "\"\\\\bcrate\\\\b"
    indent = 0
    for kw in keywords:
        pattern = "{}|\\\\b{}\\\\b".format(pattern, kw)
        indent += len(kw) + 9 # 2x quote + 2x \\b + 1x pipe
        if indent > 65:
            indent = 0
            pattern += "\"\n + \""
    pattern += "\""
    return pattern


if __name__ == '__main__':
    print(format_as_java_regex_pattern(extract_keywords_from_crate_sql_grammar()))
