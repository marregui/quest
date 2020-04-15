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

import io.crate.cli.backend.SQLConnection;
import io.crate.cli.backend.SQLExecutionRequest;
import io.crate.cli.common.*;
import io.crate.cli.store.JsonStore;
import io.crate.cli.store.Store;
import io.crate.cli.store.StoreItem;

import java.awt.*;
import javax.swing.*;
import javax.swing.text.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Closeable;
import java.util.Locale;
import java.util.function.Supplier;


public class CommandBoardManager extends JPanel implements EventSpeaker<CommandBoardManager.EventType>, Closeable {

    public enum EventType {
        COMMAND_AVAILABLE,
        COMMAND_CANCEL,
        CONNECTION_STATUS_CLICKED
    }

    public static final Color FONT_COLOR = Color.WHITE;
    public static final Color KEYWORD_FONT_COLOR = GUIToolkit.CRATE_COLOR;
    private static final Color BACKGROUND_COLOR = Color.BLACK;
    private static final Color CARET_COLOR = Color.GREEN;
    private static final Font HEADER_FONT = new Font(GUIToolkit.MAIN_FONT_NAME, Font.BOLD, 16);
    private static final Font BODY_FONT = new Font(GUIToolkit.MAIN_FONT_NAME, Font.BOLD, 18);
    private static final Cursor HAND_CURSOR = new Cursor(Cursor.HAND_CURSOR);
    private static final String BORDER_TITLE = "Connection";


    public static class Data extends StoreItem {

        public enum AttributeName implements HasKey {
            board_content;

            @Override
            public String getKey() {
                return name();
            }
        }

        private transient SQLConnection sqlConnection;

        @SuppressWarnings("unused")
        public Data(StoreItem other) {
            super(other);
        }

        public Data() {
            super("CommandBoardData");
        }

        @Override
        public final String getKey() {
            return String.format(Locale.ENGLISH, "%s", name);
        }

        public void setSqlConnection(SQLConnection conn) {
            sqlConnection = conn;
        }

        public SQLConnection getSqlConnection() {
            return sqlConnection;
        }

        public String getBoardContent() {
            return getAttribute(Data.AttributeName.board_content);
        }

        public void setBoardContent(String text) {
            setAttribute(Data.AttributeName.board_content, text);
        }
    }

    private final JTextPane textPane;
    private final JButton runButton;
    private final JButton runLineButton;
    private final JButton cancelButton;
    private final JLabel selectedConnectionTitle;
    private final Data data;
    private final Store<Data> store;
    private final EventListener<CommandBoardManager, SQLExecutionRequest> eventListener;


    public CommandBoardManager(int height, EventListener<CommandBoardManager, SQLExecutionRequest> eventListener) {
        this.eventListener = eventListener;
        store = new JsonStore<>("command_board.json", Data.class) {
            @Override
            public Data[] defaultStoreEntries() {
                return new Data[0];
            }
        };
        store.load();
        data = !store.values().isEmpty() ? store.values().get(0) : new Data();
        textPane = new JTextPane();
        textPane.setCaretPosition(0);
        textPane.setMargin(new Insets(5, 5, 5, 5));
        textPane.setFont(BODY_FONT);
        textPane.setForeground(FONT_COLOR);
        textPane.setBackground(BACKGROUND_COLOR);
        textPane.setCaretColor(CARET_COLOR);
        AbstractDocument abstractDocument = (AbstractDocument) textPane.getDocument();
        abstractDocument.setDocumentFilter(new KeywordDocumentFilter(textPane.getStyledDocument()));
        String boardContent = data.getBoardContent();
        textPane.setText(boardContent);
        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(this::onClearButtonEvent);
        runLineButton = new JButton("L.Run");
        runLineButton.addActionListener(this::onRunCurrentLineEvent);
        runLineButton.setEnabled(false);
        runButton = new JButton("Run");
        runButton.addActionListener(this::onRunEvent);
        runButton.setEnabled(false);
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this::onCancelButtonEvent);
        cancelButton.setEnabled(false);
        JPanel actionButtonsPanel = new JPanel(new GridLayout(1, 3, 0, 0));
        actionButtonsPanel.setBorder(BorderFactory.createEtchedBorder());
        actionButtonsPanel.add(clearButton);
        actionButtonsPanel.add(runLineButton);
        actionButtonsPanel.add(runButton);
        actionButtonsPanel.add(cancelButton);
        selectedConnectionTitle = new JLabel();
        selectedConnectionTitle.setFont(HEADER_FONT);
        selectedConnectionTitle.setForeground(GUIToolkit.TABLE_HEADER_FONT_COLOR);
        selectedConnectionTitle.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                eventListener.onSourceEvent(
                        CommandBoardManager.this,
                        EventType.CONNECTION_STATUS_CLICKED,
                        new SQLExecutionRequest(data.getKey(), data.sqlConnection, data.getBoardContent()));
            }

            @Override
            public void mousePressed(MouseEvent e) {
                // no-op
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                // no-op
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setCursor(HAND_CURSOR);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
            }
        });
        JPanel bufferActionButtonsPanel = new JPanel(new BorderLayout());
        bufferActionButtonsPanel.add(selectedConnectionTitle, BorderLayout.WEST);
        bufferActionButtonsPanel.add(actionButtonsPanel, BorderLayout.EAST);
        JScrollPane scrollPane = new JScrollPane(
                textPane,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(bufferActionButtonsPanel, BorderLayout.SOUTH);
        setPreferredSize(new Dimension(0, height));
        toggleComponents();
    }

    public SQLConnection getSQLConnection() {
        return data.getSqlConnection();
    }

    public void setSQLConnection(SQLConnection conn) {
        data.setSqlConnection(conn);
        toggleComponents();
    }

    private String getFullTextContents() {
        try {
            int len = textPane.getStyledDocument().getLength();
            return textPane.getStyledDocument().getText(0, len);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    private String getCommand() {
        String command = textPane.getSelectedText();
        if (null == command) {
            command = getFullTextContents();
        }
        return command.trim();
    }

    private String getCurrentLine() {
        try {
            int caretPos = textPane.getCaretPosition();
            int start = Utilities.getRowStart(textPane, caretPos);
            int end = Utilities.getRowEnd(textPane, caretPos) - 1;
            String command = textPane.getStyledDocument().getText(start, end - start + 1);
            return command.trim();
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    private void toggleComponents() {
        SQLConnection conn = data.getSqlConnection();
        if (null != conn) {
            selectedConnectionTitle.setText(String.format(
                    Locale.ENGLISH,
                    "  %s [%s]",
                    BORDER_TITLE,
                    conn.getKey()));
        }
        boolean isConnected = null != conn && conn.isConnected();
        selectedConnectionTitle.setForeground(isConnected ? GUIToolkit.CRATE_COLOR : Color.BLACK);
        runButton.setEnabled(isConnected);
        runLineButton.setEnabled(isConnected);
        cancelButton.setEnabled(isConnected);
    }

    public void store() {
        data.setBoardContent(getFullTextContents());
        store.add(true, data);
    }

    @Override
    public void requestFocus() {
        super.requestFocus();
        textPane.requestFocus();
    }

    @Override
    public void close() {
        data.setBoardContent(getFullTextContents());
        store.store();
    }

    public void onClearButtonEvent(ActionEvent event) {
        data.setBoardContent("");
        textPane.setText("");
    }

    public void onCancelButtonEvent(ActionEvent event) {
        SQLConnection conn = data.getSqlConnection();
        if (null == conn || !conn.isConnected()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Not connected");
            return;
        }
        eventListener.onSourceEvent(
                this,
                EventType.COMMAND_CANCEL,
                new SQLExecutionRequest(data.getKey(), conn, "CANCEL"));
    }

    public void onRunCurrentLineEvent(ActionEvent event) {
        fireCommandEvent(this::getCurrentLine);
    }

    public void onRunEvent(ActionEvent event) {
        fireCommandEvent(this::getCommand);
    }

    private void fireCommandEvent(Supplier<String> commandSupplier) {
        SQLConnection conn = data.getSqlConnection();
        if (null == conn) {
            JOptionPane.showMessageDialog(
                    this,
                    "Connection not selected");
            return;
        }
        String command = commandSupplier.get();
        if (null == command || command.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Command is empty");
            return;
        }
        eventListener.onSourceEvent(
                this,
                EventType.COMMAND_AVAILABLE,
                new SQLExecutionRequest(data.getKey(), conn, command));
    }
}
