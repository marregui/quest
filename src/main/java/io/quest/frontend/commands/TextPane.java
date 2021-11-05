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

package io.quest.frontend.commands;

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
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.PatternSyntaxException;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.undo.UndoManager;

import io.quest.common.GTk;


public class TextPane extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String EMPTY_STR = "";
    private static final String END_LINE = "\n";
    static final String ERROR_HEADER = "==========  E R R O R  ==========" + END_LINE;
    private static final Font FONT = new Font("Monospaced", Font.BOLD, 14);
    private static final Color CARET_COLOR = Color.CYAN;
    private static final Color BACKGROUND_COLOR = Color.BLACK;

    protected final JTextPane textPane;
    private final Highlighter keywordsHighlighter;
    private final InputMap inputMap;
    private final ActionMap actionMap;
    private final AtomicReference<UndoManager> undoManager; // set by CommandBoard


    public TextPane() {
        textPane = new JTextPane();
        textPane.setEditable(true);
        textPane.setMargin(new Insets(5, 5, 5, 5));
        textPane.setFont(FONT);
        textPane.setBackground(BACKGROUND_COLOR);
        textPane.setCaretColor(CARET_COLOR);
        textPane.setCaretPosition(0);
        keywordsHighlighter = new Highlighter(textPane.getStyledDocument()); // produces "style change" events
        inputMap = textPane.getInputMap(JComponent.WHEN_FOCUSED);
        actionMap = textPane.getActionMap();
        undoManager = new AtomicReference<>();
        setupKeyboardActions();
        AbstractDocument doc = (AbstractDocument) textPane.getDocument();
        doc.putProperty(DefaultEditorKit.EndOfLineStringProperty, END_LINE);
        doc.setDocumentFilter(keywordsHighlighter);
        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
    }

    public void displayMessage(String message) {
        textPane.setText(message);
        repaint();
    }

    public void displayError(Throwable error) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(ERROR_HEADER).append("\n");
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            error.printStackTrace(pw);
            sb.append(sw);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        textPane.setText(sb.toString());
        repaint();
    }

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

    protected String getContent() {
        return getContent(0, -1);
    }

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

    protected int highlightContent(String findRegex) {
        if (findRegex != null && !findRegex.isBlank() && !findRegex.isEmpty()) {
            return keywordsHighlighter.handleTextChanged(findRegex);
        }
        return 0;
    }

    protected int replaceContent(String findRegex, String replaceWith) {
        if (findRegex != null && !findRegex.isBlank() && replaceWith != null) {
            try {
                textPane.setText(getContent().replaceAll(findRegex, replaceWith));
                return keywordsHighlighter.handleTextChanged();
            } catch (PatternSyntaxException err) {
                JOptionPane.showMessageDialog(null, String.format(
                        "Not a valid filter: %s", err.getMessage()));
            }
        }
        return 0;
    }

    protected void setUndoManager(UndoManager undoManager) {
        AbstractDocument doc = (AbstractDocument) textPane.getDocument();
        UndoManager current = this.undoManager.get();
        if (current != null) {
            doc.removeUndoableEditListener(current);
        }
        undoManager.discardAllEdits();
        this.undoManager.set(undoManager);
        doc.addUndoableEditListener(undoManager);
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
            UndoManager undoManager = this.undoManager.get();
            if (undoManager != null && undoManager.canUndo()) {
                try {
                    undoManager.undo();
                } catch (Throwable ignore) {
                    // do nothing
                }
                keywordsHighlighter.handleTextChanged();
            }
        });

        // cmd-y, redo last undo edit
        addCmdKeyAction(KeyEvent.VK_Y, e -> {
            UndoManager undoManager = this.undoManager.get();
            if (undoManager != null && undoManager.canRedo()) {
                try {
                    undoManager.redo();
                } catch (Throwable ignore) {
                    // do nothing
                }
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
        addCmdKeyAction(KeyEvent.VK_DOWN,
                e -> textPane.setCaretPosition(textPane.getStyledDocument().getLength()));

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
