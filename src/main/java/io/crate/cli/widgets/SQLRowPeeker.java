/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */
package io.crate.cli.widgets;

import io.crate.cli.backend.SQLTable;
import io.crate.cli.common.GUIToolkit;
import io.crate.cli.common.SqlType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;


class SQLRowPeeker extends JPanel implements MouseListener, MouseMotionListener {

    private static final char NEWLINE = '\n';
    private static final int CHAR_WIDTH = 12;
    private static final int CHAR_HEIGHT = GUIToolkit.TABLE_HEADER_HEIGHT;
    private static final int MAX_POPUP_WIDTH = 600;
    private static final int MAX_POPUP_HEIGHT = 500;


    private final Component owner;

    private final AtomicBoolean isShowing;
    private final JScrollPane toolTipScrollPane;
    private final JTextPane toolTip;
    private final Map<String, String> toStringCache;
    private Popup toolTipPopup;
    private SQLTable.SQLTableRow currentRow;

    SQLRowPeeker(Component owner) {
        this.owner = owner;
        isShowing = new AtomicBoolean();
        toolTip = new JTextPane();
        toolTip.setEditable(false);
        toolTip.setBorder(BorderFactory.createEtchedBorder());
        toolTip.setBackground(Color.BLACK);
        toolTip.setForeground(GUIToolkit.CRATE_COLOR);
        toolTip.setFont(GUIToolkit.TABLE_CELL_FONT);
        toolTipScrollPane = new JScrollPane(toolTip);
        toolTipScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        toolTipScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        toStringCache = new WeakHashMap<>();
        JButton closeButton = new JButton() {
            @Override
            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.BLACK);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setFont(GUIToolkit.TABLE_HEADER_FONT);
                g2.setColor(Color.ORANGE);
                int y = (int)(getHeight() * 0.7);
                g2.drawString("Copied to clipboard", 10, y);
                g2.drawString("[X]", getWidth() - 40, y);
            }
        };
        closeButton.setBorder(BorderFactory.createEtchedBorder());
        closeButton.setOpaque(true);
        closeButton.addActionListener(this::onCloseEvent);
        closeButton.setPreferredSize(new Dimension(0, 30));
        setOpaque(true);
        setLayout(new BorderLayout());
        add(closeButton, BorderLayout.NORTH);
        add(toolTipScrollPane, BorderLayout.CENTER);

    }

    public void clear() {
        toStringCache.clear();
    }

    public void onCloseEvent() {
        onCloseEvent(null);
    }

    private void onCloseEvent(ActionEvent event) {
        if (isShowing.compareAndSet(true, false)) {
            toStringCache.clear();
            toolTipPopup.hide();
        }
    }

    private static String renderRow(SQLTable.SQLTableRow row) {
        StringBuilder sb = new StringBuilder();
        sb.append(NEWLINE)
                .append("Key ")
                .append(row.getKey())
                .append(": ")
                .append(NEWLINE)
                .append(NEWLINE);
        int colIdx = 0;
        SQLTable table = row.getParent();
        String[] colNames = table.getColumnNames();
        Object [] values = row.getValues();
        int[] colTypes = table.getColumnTypes();
        for (int i=0; i < colNames.length; i++) {
            sb.append("   - ")
                    .append(colNames[i])
                    .append(" [")
                    .append(SqlType.resolveName(colTypes[colIdx++]))
                    .append("]: ")
                    .append(values[i])
                    .append(NEWLINE);
        }
        sb.append(NEWLINE);
        return sb.toString();
    }

    private static int maxLength(String str, char delimiter) {
        int max = 0;
        int runningMax = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == delimiter) {
                max = Math.max(max, runningMax);
                runningMax = 0;
                continue;
            }
            runningMax += 1;
        }
        return Math.max(max, runningMax);
    }

    private static SQLTable.SQLTableRow getRow(MouseEvent e) {
        SQLTable.SQLTableRow row = null;
        JTable table = (JTable) e.getSource();
        int rowIdx = table.rowAtPoint(e.getPoint());
        int colIdx = table.columnAtPoint(e.getPoint());
        if (rowIdx >= 0 && rowIdx < table.getRowCount() && colIdx >= 0 && colIdx < table.getColumnCount()) {
            row = (SQLTable.SQLTableRow) table.getValueAt(rowIdx, -1);
        }
        return row;
    }

    private void setText(SQLTable.SQLTableRow row) {
        currentRow = row;
        String text = toStringCache.computeIfAbsent(row.getKey(), key -> renderRow(row));
        GUIToolkit.copyToClipboard(text);
        toolTip.setText(text);
        toolTip.setCaretPosition(0);
        int width = CHAR_WIDTH * maxLength(text, NEWLINE);
        int height = CHAR_HEIGHT * Math.max(row.getParent().getColumnTypes().length, 4);
        Dimension size = new Dimension(
                Math.min(width, MAX_POPUP_WIDTH),
                Math.min(height, MAX_POPUP_HEIGHT));
        toolTip.setPreferredSize(size);
        toolTipScrollPane.setPreferredSize(size);
    }

    private void showToolTipPopup(MouseEvent e) {
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension size = toolTip.getPreferredSize();
        int x = e.getXOnScreen();
        int y = e.getYOnScreen() - size.height / 7;
        if (x + size.width > screen.width) {
            x = screen.width - size.width - 20;
        }
        if (y + size.height > screen.height) {
            y = screen.height - size.height - 100;
        }
        toolTipPopup = PopupFactory.getSharedInstance().getPopup(owner,this, x, y);
        toolTipPopup.show();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        SQLTable.SQLTableRow row = getRow(e);
        if (null != row) {
            setText(row);
            if (isShowing.compareAndSet(false, true)) {
                showToolTipPopup(e);
            }
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        if (isShowing.get()) {
            SQLTable.SQLTableRow row = getRow(e);
            if (null != row && false == currentRow.getKey().equals(row.getKey())) {
                setText(row);
                toolTipPopup.hide();
                showToolTipPopup(e);
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // ignore
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // ignore
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // ignore
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // ignore
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        // ignore
    }
}
