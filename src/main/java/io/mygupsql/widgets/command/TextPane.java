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

package io.mygupsql.widgets.command;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.text.*;
import javax.swing.undo.UndoManager;

import io.mygupsql.GTk;


public class TextPane extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String EMPTY_STR = "";
    private static final String END_LINE = "\n";
    static final String ERROR_HEADER = "==========  E R R O R  ==========" + END_LINE;
    private static final Font FONT = new Font(GTk.MAIN_FONT_NAME, Font.BOLD, 14);
    private static final Color CARET_COLOR = Color.GREEN;
    private static final Color BACKGROUND_COLOR = Color.BLACK;

    protected final JTextPane textPane;
    private final UndoManager undoManager;
    private final KeywordsHighlighter keywordsHighlighter;
    private final InputMap inputMap;
    private final ActionMap actionMap;


    public TextPane() {
        textPane = new JTextPane();
        textPane.setEditable(true);
        textPane.setMargin(new Insets(5, 5, 5, 5));
        textPane.setFont(FONT);
        textPane.setBackground(BACKGROUND_COLOR);
        textPane.setCaretColor(CARET_COLOR);
        textPane.setCaretPosition(0);
        keywordsHighlighter = new KeywordsHighlighter(textPane.getStyledDocument()); // produces "style change" events
        undoManager = new UndoManager() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                if (!"style change".equals(e.getEdit().getPresentationName())) {
                    super.undoableEditHappened(e);
                }
            }
        };
        inputMap = textPane.getInputMap(JComponent.WHEN_FOCUSED);
        actionMap = textPane.getActionMap();
        setupKeyboardActions();
        AbstractDocument doc = (AbstractDocument) textPane.getDocument();
        doc.putProperty(DefaultEditorKit.EndOfLineStringProperty, END_LINE);
        doc.setDocumentFilter(keywordsHighlighter);
        doc.addUndoableEditListener(undoManager);
        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Displays the message.
     *
     * @param message message to be displayed
     */
    public void displayMessage(String message) {
        textPane.setText(message);
        repaint();
    }

    /**
     * Displays the error's stack trace.
     *
     * @param error carries the stack trace to be displayed
     */
    public void displayError(Throwable error) {
        textPane.setText(createErrorMessage(error));
        repaint();
    }

    /**
     * @return the content of the current line under the caret
     */
    protected String getCurrentLine() {
        try {
            int caretPos = textPane.getCaretPosition();
            int start = Utilities.getRowStart(textPane, caretPos);
            int end = Utilities.getRowEnd(textPane, caretPos);
            return getContent(start, end - start);
        } catch (BadLocationException ignore) {
            // do nothing
        }
        return EMPTY_STR;
    }

    /**
     * @return full content
     */
    protected String getContent() {
        return getContent(0, -1);
    }

    /**
     * @param start start offset
     * @param len   num chars to get, -1 to get all from start
     * @return content start.. to start + len, or ""
     */
    protected String getContent(int start, int len) {
        Document doc = textPane.getStyledDocument();
        int length = len < 0 ? doc.getLength() - start : len;
        if (length > 0) {
            try {
                String txt = doc.getText(start, length);
                if (txt != null && !txt.isBlank()) {
                    return txt.trim();
                }
            } catch (BadLocationException ignore) {
                // do nothing
            }
        }
        return EMPTY_STR;
    }

    private static String createErrorMessage(Throwable error) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(ERROR_HEADER).append("\n");
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            error.printStackTrace(pw);
            sb.append(sw);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    private void addCmdKeyAction(int keyEvent, ActionListener action) {
        Action cmd = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.actionPerformed(e);
            }
        };
        inputMap.put(KeyStroke.getKeyStroke(keyEvent, GTk.CMD_DOWN_MASK), cmd);
        actionMap.put(cmd, cmd);
    }

    private void addCmdShiftKeysAction(int keyEvent, ActionListener action) {
        Action cmd = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.actionPerformed(e);
            }
        };
        inputMap.put(KeyStroke.getKeyStroke(keyEvent, GTk.CMD_DOWN_MASK | KeyEvent.SHIFT_DOWN_MASK), cmd);
        actionMap.put(cmd, cmd);
    }

    private void setupKeyboardActions() {
        // cmd-z, undo edit
        addCmdKeyAction(KeyEvent.VK_Z, e -> {
            if (undoManager.canUndo()) {
                undoManager.undo();
            }
        });

        // cmd-y, redo last undo edit
        addCmdKeyAction(KeyEvent.VK_Y, e -> {
            if (undoManager.canRedo()) {
                undoManager.redo();
                keywordsHighlighter.handleTextChanged();
            }
        });

        // cmd-a, select the full content
        addCmdKeyAction(KeyEvent.VK_A, e -> textPane.selectAll());

        // cmd-c, copy to clipboard, selection or current line
        addCmdKeyAction(KeyEvent.VK_C, e -> {
            String selected = textPane.getSelectedText();
            if (selected == null) {
                selected = getCurrentLine();
            }
            if (!selected.equals(EMPTY_STR)) {
                GTk.systemClipboard().setContents(new StringSelection(selected), null);
            }
        });

        // cmd-d, duplicate line under caret, and append it under
        addCmdKeyAction(KeyEvent.VK_D, e -> {
            try {
                int caretPos = textPane.getCaretPosition();
                int start = Utilities.getRowStart(textPane, caretPos);
                int end = Utilities.getRowEnd(textPane, caretPos);
                String line = getContent(start, end - start);
                String insert = line.equals(EMPTY_STR) ? END_LINE : END_LINE + line;
                textPane.getStyledDocument().insertString(end, insert, null);
                textPane.setCaretPosition(caretPos + insert.length());
            } catch (BadLocationException ignore) {
                // do nothing
            }
        });

        // cmd-v, paste content of clipboard into selection or caret position
        addCmdKeyAction(KeyEvent.VK_V, e -> {
            try {
                String data = (String) GTk.systemClipboard().getData(DataFlavor.stringFlavor);
                if (data != null && !data.isEmpty()) {
                    int start = textPane.getSelectionStart();
                    int end = textPane.getSelectionEnd();
                    Document doc = textPane.getStyledDocument();
                    if (end > start) {
                        doc.remove(start, end - start);
                    }
                    doc.insertString(textPane.getCaretPosition(), data, null);
                }
            } catch (Exception fail) {
                // do nothing
            }
        });

        // cmd-x, remove selection or whole line under caret and copy to clipboard
        addCmdKeyAction(KeyEvent.VK_X, e -> {
            try {
                int start = textPane.getSelectionStart();
                int end = textPane.getSelectionEnd();
                int caretPos = textPane.getCaretPosition();
                if (end <= start) {
                    start = Utilities.getRowStart(textPane, caretPos);
                    end = Utilities.getRowEnd(textPane, caretPos);
                }
                Document doc = textPane.getStyledDocument();
                int len = end - start;
                if (len > 0) {
                    GTk.systemClipboard().setContents(new StringSelection(doc.getText(start, len)), null);
                    doc.remove(start, len);
                }
                end = doc.getLength();
                caretPos = textPane.getCaretPosition();
                if (caretPos == end) {
                    if (start > 0) {
                        doc.remove(start - 1, 1);
                    }
                } else {
                    doc.remove(start, 1);
                }
            } catch (Exception fail) {
                // do nothing
            }
        });

        // cmd-left, jump to the beginning of the line
        addCmdKeyAction(KeyEvent.VK_LEFT, e -> {
            try {
                int caretPos = textPane.getCaretPosition();
                textPane.setCaretPosition(Utilities.getRowStart(textPane, caretPos));
            } catch (BadLocationException ignore) {
                // do nothing
            }
        });

        // cmd-right, jump to the end of the line
        addCmdKeyAction(KeyEvent.VK_RIGHT, e -> {
            try {
                int caretPos = textPane.getCaretPosition();
                textPane.setCaretPosition(Utilities.getRowEnd(textPane, caretPos));
            } catch (BadLocationException ignore) {
                // do nothing
            }
        });

        // cmd-up, jump to the beginning of the document
        addCmdKeyAction(KeyEvent.VK_UP, e -> textPane.setCaretPosition(0));

        // cmd-down, jump to the end of the document
        addCmdKeyAction(KeyEvent.VK_DOWN, e -> textPane.setCaretPosition(textPane.getStyledDocument().getLength()));

        // cmd-shift-left, select from caret to the beginning of the line
        addCmdShiftKeysAction(KeyEvent.VK_LEFT, e -> {
            try {
                int caretPos = textPane.getCaretPosition();
                int start = Utilities.getRowStart(textPane, caretPos);
                textPane.setSelectionStart(start);
                textPane.setSelectionEnd(caretPos);
            } catch (BadLocationException ignore) {
                // do nothing
            }
        });

        // cmd-shift-right, select from caret to the end of the line
        addCmdShiftKeysAction(KeyEvent.VK_RIGHT, e -> {
            try {
                int caretPos = textPane.getCaretPosition();
                int end = Utilities.getRowEnd(textPane, caretPos);
                textPane.setSelectionStart(caretPos);
                textPane.setSelectionEnd(end);
            } catch (BadLocationException ignore) {
                // do nothing
            }
        });

        // cmd-shift-up, select from caret to the beginning of the document
        addCmdShiftKeysAction(KeyEvent.VK_UP, e -> {
            int caretPos = textPane.getCaretPosition();
            textPane.setSelectionStart(0);
            textPane.setSelectionEnd(caretPos);
        });

        // cmd-shift-down, select from caret to the end of the document
        addCmdShiftKeysAction(KeyEvent.VK_DOWN, e -> {
            int caretPos = textPane.getCaretPosition();
            int end = textPane.getStyledDocument().getLength();
            textPane.setSelectionStart(caretPos);
            textPane.setSelectionEnd(end);
        });
    }
}
