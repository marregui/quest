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

package io.quest.frontend.meta;

import io.questdb.cairo.ColumnType;
import io.questdb.cairo.PartitionBy;
import io.questdb.cairo.TableReaderMetadata;
import io.questdb.std.datetime.DateFormat;
import io.questdb.std.datetime.microtime.TimestampFormatCompiler;
import io.questdb.std.str.StringSink;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.TimeUnit;

class Display extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final Font FONT = new Font("Monospaced", Font.BOLD, 14);
    private static final Color FONT_COLOR = new Color(200, 50, 100);
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

    private final JTextPane textPane = new JTextPane();
    private final StringSink sink = new StringSink();

    public Display() {
        super(new BorderLayout());
        FontMetrics metrics = textPane.getFontMetrics(FONT);
        int vMargin = metrics.getHeight();
        int hMargin = metrics.stringWidth("####");
        Insets margin = new Insets(vMargin, hMargin, vMargin, hMargin);
        textPane.setMargin(margin);
        textPane.setFont(FONT);
        textPane.setForeground(FONT_COLOR);
        textPane.setBackground(Color.BLACK);
        textPane.setEditable(false);
        add(new JScrollPane(textPane), BorderLayout.CENTER);
    }

    public void clear() {
        sink.clear();
    }

    @Override
    public String toString() {
        return sink.toString();
    }

    public void render() {
        render(toString());
    }

    public void render(String message) {
        textPane.setText(message);
        repaint();
    }

    public void addLn() {
        sink.put(System.lineSeparator());
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

    public void addTimeLn(String name, long value) {
        sink.put(name).put(value).put(" micros (")
                .put(TimeUnit.MICROSECONDS.toSeconds(value))
                .put(" sec, or ")
                .put(TimeUnit.MICROSECONDS.toMinutes(value))
                .put(" min)")
                .put(System.lineSeparator());
    }

    public void addTimestampLn(String name, long timestamp) {
        sink.put(name).put(timestamp).put(" (");
        TS_FORMATTER.format(timestamp, null, "Z", sink);
        sink.put(')').put(System.lineSeparator());
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
                .put(" / ts: ").put(partitionTimestamp).put(" (");
        DATE_FORMATTER.format(partitionTimestamp, null, "Z", sink);
        sink.put(')')
                .put(", size: ").put(partitionSize)
                .put(", txn: ").put(partitionNameTxn)
                .put(", column version: ").put(partitionColumnVersion)
                .put(System.lineSeparator())
                .put("   symbol value count: ").put(partitionSymbolValueCount)
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
                .put(" / ts: ").put(partitionTimestamp).put(" (");
        DATE_FORMATTER.format(partitionTimestamp, null, "Z", sink);
        sink.put(')')
                .put(", size: ").put(partitionSize)
                .put(", txn: ").put(partitionNameTxn)
                .put(", column version: ").put(partitionColumnVersion)
                .put("   symbol value count: ").put(partitionSymbolValueCount)
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
                .put(" -> name: ").put(columnName)
                .put(", type: ").put(ColumnType.nameOf(columnType))
                .put(", indexed: ").put(columnIsIndexed)
                .put(", indexBlockCapacity: ").put(columnIndexBlockCapacity)
                .put(System.lineSeparator());
    }

    public void addCreateTableLn(TableReaderMetadata metadata) {
        sink.put("CREATE TABLE IF NOT EXISTS change_me (").put(System.lineSeparator());
        for (int i=0, n=metadata.getColumnCount(); i < n; i++) {
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
            if (partitionBy != PartitionBy.NONE ) {
                sink.put(" PARTITION BY ").put(PartitionBy.toString(partitionBy));
            }
        }
        sink.put(';').put(System.lineSeparator());
    }
}
