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

package io.quest.frontend.editor.meta;

import io.quest.frontend.editor.Editor;
import io.questdb.cairo.ColumnType;
import io.questdb.cairo.PartitionBy;
import io.questdb.cairo.TableReaderMetadata;
import io.questdb.std.datetime.DateFormat;
import io.questdb.std.datetime.microtime.TimestampFormatCompiler;
import io.questdb.std.str.StringSink;

import java.util.concurrent.TimeUnit;

class Display extends Editor {
    private static final DateFormat TS_FORMATTER = new TimestampFormatCompiler().compile(
            "yyyy-MM-ddTHH:mm:ss.SSSz"
    );

    private static final DateFormat DATE_FORMATTER = new TimestampFormatCompiler().compile(
            "yyyy-MM-dd"
    );

    static {
        // preload, which compiles the pattern and is costly, penalising startup time
        TS_FORMATTER.format(0, null, "Z", new StringSink());
    }

    private final StringSink sink = new StringSink();

    public Display() {
        super(true, MetaHighlighter::of);
        setFontSize(13);
    }

    public void clear() {
        sink.clear();
    }

    public void render() {
        displayMessage(sink.toString());
    }

    public void addLn() {
        sink.put(System.lineSeparator());
    }

    public void addLn(String name) {
        sink.put(name).put(System.lineSeparator());
    }

    public void addLn(String name, int value) {
        sink.put(name).put(value).put(System.lineSeparator());
    }

    public void addLn(String name, long value) {
        sink.put(name).put(value).put(System.lineSeparator());
    }

    public void addLn(String name, boolean value) {
        sink.put(name).put(value).put(System.lineSeparator());
    }

    public void addLn(String name, String value) {
        sink.put(name).put(value).put(System.lineSeparator());
    }

    public void addIndexedSymbolLn(int index, CharSequence value, boolean indented) {
        if (indented) {
            sink.put(" - ");
        }
        sink.put(index).put(": ").put(value).put(System.lineSeparator());
    }

    public void addMicrosLn(String name, long value) {
        sink.put(name).put(value).put(" micros (")
                .put(TimeUnit.MICROSECONDS.toSeconds(value))
                .put(" sec, or ")
                .put(TimeUnit.MICROSECONDS.toMinutes(value))
                .put(" min)")
                .put(System.lineSeparator());
    }

    public void addTimestampLn(String name, long timestamp) {
        addFormattedTimestampLn(name, timestamp, TS_FORMATTER);
    }

    public void addDateLn(String name, long timestamp) {
        addFormattedTimestampLn(name, timestamp, DATE_FORMATTER);
    }

    private void addFormattedTimestampLn(String name, long timestamp, DateFormat formatter) {
        if (Long.MAX_VALUE == timestamp) {
            sink.put(name).put("MAX_VALUE").put(System.lineSeparator());
        } else if (Long.MIN_VALUE == timestamp) {
            sink.put(name).put("MIN_VALUE").put(System.lineSeparator());
        } else {
            sink.put(name).put(timestamp).put(" (");
            formatter.format(timestamp, null, "Z", sink);
            sink.put(')').put(System.lineSeparator());
        }
    }

    public void addPartitionLn(
            int partitionIndex,
            long partitionTimestamp,
            long partitionNameTxn,
            long partitionSize,
            long partitionColumnVersion,
            long partitionSymbolValueCount,
            long partitionMask,
            boolean partitionIsRO,
            long partitionAvailable0,
            long partitionAvailable1,
            long partitionAvailable2
    ) {
        sink.put(" - partition ").put(partitionIndex)
                .put(" [").put(partitionTimestamp).put(" (");
        DATE_FORMATTER.format(partitionTimestamp, null, "Z", sink);
        sink.put(')')
                .put("] size: ").put(partitionSize)
                .put(", txn: ").put(partitionNameTxn)
                .put(", column version: ").put(partitionColumnVersion)
                .put(System.lineSeparator())
                .put(", symbol value count: ").put(partitionSymbolValueCount)
                .put(", isRO: ").put(partitionIsRO)
                .put(", mask: ").put(partitionMask)
                .put(", av0: ").put(partitionAvailable0)
                .put(", av1: ").put(partitionAvailable1)
                .put(", av2: ").put(partitionAvailable2)
                .put(System.lineSeparator());
    }

    public void addPartitionLn(
            int partitionIndex,
            long partitionTimestamp,
            long partitionNameTxn,
            long partitionSize,
            long partitionColumnVersion,
            long partitionSymbolValueCount
    ) {
        sink.put(" - partition ").put(partitionIndex)
                .put(" [").put(partitionTimestamp).put(" (");
        DATE_FORMATTER.format(partitionTimestamp, null, "Z", sink);
        sink.put(')')
                .put("] size: ").put(partitionSize)
                .put(", txn: ").put(partitionNameTxn)
                .put(", column version: ").put(partitionColumnVersion)
                .put(", symbol value count: ").put(partitionSymbolValueCount)
                .put(System.lineSeparator());
    }


    public void addColumnLn(
            int columnIndex,
            CharSequence columnName,
            int columnType,
            boolean columnIsIndexed,
            int columnIndexBlockCapacity,
            boolean indented
    ) {
        if (indented) {
            sink.put(" - ");
        }
        sink.put("column ").put(columnIndex)
                .put(" -> ").put(columnName)
                .put(" ").put(ColumnType.nameOf(columnType));
        if (columnIsIndexed) {
            sink.put(", indexed: ").put(columnIsIndexed)
                    .put(", indexBlockCapacity: ").put(columnIndexBlockCapacity);
        }
        sink.put(System.lineSeparator());
    }

    public void addCreateTableLn(TableReaderMetadata metadata) {
        sink.put("CREATE TABLE IF NOT EXISTS change_me (").put(System.lineSeparator());
        for (int i = 0, n = metadata.getColumnCount(); i < n; i++) {
            String colName = metadata.getColumnName(i);
            int colType = metadata.getColumnType(i);
            sink.put("    ").put(colName).put(' ').put(ColumnType.nameOf(colType));
            if (colType == ColumnType.SYMBOL && metadata.isColumnIndexed(i)) {
                sink.put(" INDEX CAPACITY ").put(metadata.getIndexValueBlockCapacity(i));
            }
            if (i < n - 1) {
                sink.put(',');
            }
            sink.put(System.lineSeparator());
        }
        sink.put(')');
        int timestampIdx = metadata.getTimestampIndex();
        if (timestampIdx != -1) {
            sink.put(" TIMESTAMP(").put(metadata.getColumnName(timestampIdx)).put(')');
            int partitionBy = metadata.getPartitionBy();
            if (partitionBy != PartitionBy.NONE) {
                sink.put(" PARTITION BY ").put(PartitionBy.toString(partitionBy));
            }
        }
        sink.put(';').put(System.lineSeparator());
    }
}
