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

package io.quest.frontend.editor;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.PatternSyntaxException;
import javax.swing.*;
import javax.swing.text.*;
import javax.swing.undo.UndoManager;

import io.quest.frontend.GTk;


public class QuestPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private static final String MARGIN_TOKEN = ":99999:";
    private static final Font FONT = new Font("Monospaced", Font.BOLD, 14);
    private static final Font LINENO_FONT = new Font(GTk.MAIN_FONT_NAME, Font.ITALIC, 14);
    static final Color LINENO_COLOR = Color.LIGHT_GRAY.darker().darker();
    private static final Color CARET_COLOR = Color.CYAN;
    private static final Color BACKGROUND_COLOR = Color.BLACK;

    protected final JTextPane textPane;
    private final Highlighter highlighter;
    private final AtomicReference<UndoManager> undoManager; // set by CommandBoard
    private final StringBuilder sb;

    public QuestPanel() {
        this(false);
    }

    public QuestPanel(boolean isErrorPanel) {
        sb = new StringBuilder();
        undoManager = new AtomicReference<>();
        textPane = new JTextPane() {
            public boolean getScrollableTracksViewportWidth() {
                return getUI().getPreferredSize(this).width <= getParent().getSize().width;
            }
        };
        final FontMetrics metrics = textPane.getFontMetrics(FONT);
        final int topMargin = metrics.getHeight() / 2;
        final int bottomMargin = metrics.getHeight() / 2;
        final int leftMargin = metrics.stringWidth(MARGIN_TOKEN);
        final int rightMargin = metrics.stringWidth(":");
        final Insets margin = new Insets(topMargin, leftMargin, bottomMargin, rightMargin);
        final JScrollPane scrollPane = new JScrollPane(textPane);
        textPane.setMargin(margin);
        textPane.setFont(FONT);
        textPane.setBackground(BACKGROUND_COLOR);
        textPane.setCaretColor(CARET_COLOR);
        textPane.setEditorKit(new StyledEditorKit() {
            @Override
            public ViewFactory getViewFactory() {
                final ViewFactory defaultViewFactory = super.getViewFactory();
                return elem -> {
                    if (elem.getName().equals(AbstractDocument.ParagraphElementName)) {
                        return new ParagraphView(elem, topMargin / 2);
                    }
                    return defaultViewFactory.create(elem);
                };
            }
        });
        textPane.setCaretPosition(0);
        textPane.setEditable(!isErrorPanel);
        setupKeyboardActions(isErrorPanel);
        highlighter = Highlighter.of(textPane); // produces "style change" events
        scrollPane.getViewport().setBackground(BACKGROUND_COLOR);
        scrollPane.getVerticalScrollBar().setUnitIncrement(5);
        scrollPane.getVerticalScrollBar().setBlockIncrement(15);
        scrollPane.getHorizontalScrollBar().setUnitIncrement(5);
        scrollPane.getHorizontalScrollBar().setBlockIncrement(15);
        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);

    }

    private static class ParagraphView extends javax.swing.text.ParagraphView {
        private final int topMargin;

        public ParagraphView(Element element, int topMargin) {
            super(element);
            this.topMargin = topMargin;
        }

        @Override
        public void paintChild(Graphics g, Rectangle alloc, int index) {
            super.paintChild(g, alloc, index);
            g.setFont(LINENO_FONT);
            g.setColor(LINENO_COLOR);
            int line = getLineNumber() + 1;
            String lineStr = String.valueOf(line);
            FontMetrics metrics = g.getFontMetrics();
            int strWidth = metrics.stringWidth(lineStr);
            int marginWidth = metrics.stringWidth(MARGIN_TOKEN);
            int x = marginWidth - strWidth;
            int y = topMargin + line * metrics.getHeight();
            g.drawString(String.valueOf(line), x, y);
        }

        private int getLineNumber() {
            Element root = getDocument().getDefaultRootElement();
            for (int i = 0; i < root.getElementCount(); i++) {
                if (root.getElement(i) == getElement()) {
                    return i;
                }
            }
            return 0;
        }
    }

    public void displayMessage(String message) {
        textPane.setText(message);
        repaint();
    }

    public void displayError(Throwable error) {
        textPane.setText(highlighter.highlightError(error));
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
        return "";
    }

    protected String getContent() {
        return getContent(0, -1);
    }

    protected String getContent(int start, int len) {
        Document doc = textPane.getDocument();
        int length = len < 0 ? doc.getLength() - start : len;
        if (length > 0) {
            try {
                String txt = doc.getText(start, length);
                if (txt != null) {
                    return txt;
                }
            } catch (BadLocationException ignore) {
                // do nothing
            }
        }
        return "";
    }

    protected int highlightContent(String findRegex) {
        return findRegex != null ? highlighter.handleTextChanged(findRegex, null) : 0; // number of matches
    }

    protected int replaceContent(String findRegex, String replaceWith) {
        if (findRegex != null) {
            try {
                textPane.setText(getContent().replaceAll(findRegex, replaceWith));
                return highlighter.handleTextChanged();
            } catch (PatternSyntaxException err) {
                JOptionPane.showMessageDialog(
                        null,
                        String.format(
                                "Not a valid filter: %s",
                                err.getMessage()));
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

    private void cmdZUndo(ActionEvent event) {
        // cmd-z, undo edit
        UndoManager undoManager = this.undoManager.get();
        if (undoManager != null && undoManager.canUndo()) {
            try {
                undoManager.undo();
                highlighter.handleTextChanged();
            } catch (Throwable ignore) {
                // do nothing
            }
        }
    }

    private void cmdYRedo(ActionEvent event) {
        // cmd-y, redo last undo edit
        UndoManager undoManager = this.undoManager.get();
        if (undoManager != null && undoManager.canRedo()) {
            try {
                undoManager.redo();
                highlighter.handleTextChanged();
            } catch (Throwable ignore) {
                // do nothing
            }
        }
    }

    private void cmdDDupLine(ActionEvent event) {
        // cmd-d, duplicate line under caret, and append it under
        try {
            int caretPos = textPane.getCaretPosition();
            int start = Utilities.getRowStart(textPane, caretPos);
            int end = Utilities.getRowEnd(textPane, caretPos);
            Document doc = textPane.getDocument();
            String line = doc.getText(start, end - start);
            String insert = line.isEmpty() ? "\n" : "\n" + line;
            doc.insertString(end, insert, null);
            textPane.setCaretPosition(caretPos + insert.length());
            highlighter.handleTextChanged();
        } catch (BadLocationException ignore) {
            // do nothing
        }
    }

    private void cmdCCopyToClipboard(ActionEvent event) {
        // cmd-c, copy to clipboard, selection or current line
        String selected = textPane.getSelectedText();
        if (selected == null) {
            selected = getCurrentLine();
        }
        if (!selected.isEmpty()) {
            GTk.setClipboardContent(selected);
        }
    }

    private void cmdVPasteFromClipboard(ActionEvent event) {
        // cmd-v, paste content of clipboard into selection or caret position
        try {
            String text = GTk.getClipboardContent();
            if (text != null) {
                int start = textPane.getSelectionStart();
                int end = textPane.getSelectionEnd();
                Document doc = textPane.getDocument();
                if (end > start) {
                    doc.remove(start, end - start);
                }
                doc.insertString(textPane.getCaretPosition(), text, null);
                highlighter.handleTextChanged();
            }
        } catch (Exception fail) {
            // do nothing
        }
    }

    private void cmdXCutToClipboard(ActionEvent event) {
        // cmd-x, remove selection or whole line under caret and copy to clipboard
        try {
            int start = Utilities.getRowStart(textPane, textPane.getSelectionStart());
            int end = Utilities.getRowEnd(textPane, textPane.getSelectionEnd());
            Document doc = textPane.getDocument();
            int len = end - start;
            if (len > 0) {
                GTk.setClipboardContent(doc.getText(start, len));
            }
            if (start > 0) {
                doc.remove(start - 1, len + 1);
            } else {
                doc.remove(start, len + 1);
            }
        } catch (Exception fail) {
            // do nothing
        }
    }

    private void cmdUp(ActionEvent event) {
        // cmd-up, jump to the beginning of the document
        textPane.setCaretPosition(0);
    }

    private void cmdDown(ActionEvent event) {
        // cmd-down, jump to the end of the document
        textPane.setCaretPosition(textPane.getDocument().getLength());
    }

    private void cmdLeft(ActionEvent event) {
        // cmd-left, jump to the beginning of the line
        try {
            int caretPos = textPane.getCaretPosition();
            textPane.setCaretPosition(Utilities.getRowStart(textPane, caretPos));
        } catch (BadLocationException ignore) {
            // do nothing
        }
    }

    private void cmdRight(ActionEvent event) {
        // cmd-right, jump to the end of the line
        try {
            int caretPos = textPane.getCaretPosition();
            textPane.setCaretPosition(Utilities.getRowEnd(textPane, caretPos));
        } catch (BadLocationException ignore) {
            // do nothing
        }
    }

    private void cmdShiftUp(ActionEvent event) {
        // cmd-shift-up, jump to the beginning of the page
        int end = textPane.getSelectionEnd();
        textPane.setCaretPosition(0);
        if (textPane.getSelectionStart() != end) {
            textPane.select(0, end);
        }
    }

    private void cmdShiftDown(ActionEvent event) {
        // cmd-shift-down, jump to the end of the page
        int start = textPane.getSelectionStart();
        int end = textPane.getDocument().getLength();
        textPane.setCaretPosition(end);
        if (start != textPane.getSelectionEnd()) {
            textPane.select(start, end);
        }
    }

    private void cmdShiftLeft(ActionEvent event) {
        // cmd-shift-left, jump to the beginning of the line
        try {
            int caretPos = textPane.getCaretPosition();
            int start = Utilities.getRowStart(textPane, caretPos);
            int end = textPane.getSelectionEnd();
            textPane.setCaretPosition(start);
            if (textPane.getSelectionStart() != end) {
                textPane.select(start, end);
            }
        } catch (BadLocationException ignore) {
            // do nothing
        }
    }

    private void cmdShiftRight(ActionEvent event) {
        // cmd-shift-right, jump to the end of the line
        try {
            int caretPos = textPane.getCaretPosition();
            int start = textPane.getSelectionStart();
            int end = Utilities.getRowEnd(textPane, caretPos);
            textPane.setCaretPosition(end);
            if (start != textPane.getSelectionEnd()) {
                textPane.select(start, end);
            }
        } catch (BadLocationException ignore) {
            // do nothing
        }
    }

    private void altUp(ActionEvent event) {
        // alt-up, select current word under caret
        try {
            int caretPos = textPane.getCaretPosition();
            int wordStart = Utilities.getWordStart(textPane, caretPos);
            int wordEnd = Utilities.getWordEnd(textPane, caretPos);
            textPane.select(wordStart, wordEnd);
        } catch (BadLocationException ignore) {
            // do nothing
        }
    }

    private void altLeft(ActionEvent event) {
        // alt-left, jump to the beginning of the word
        try {
            int caretPos = textPane.getCaretPosition();
            int wordStart = Utilities.getWordStart(textPane, caretPos);
            if (caretPos == wordStart && caretPos > 0) {
                wordStart = Utilities.getWordStart(textPane, caretPos - 1);
            }
            textPane.setCaretPosition(wordStart);
        } catch (BadLocationException ignore) {
            // do nothing
        }
    }

    private void altRight(ActionEvent event) {
        // alt-right, jump to the end of the word
        try {
            int caretPos = textPane.getCaretPosition();
            int wordEnd = Utilities.getWordEnd(textPane, caretPos);
            if (caretPos == wordEnd && caretPos < textPane.getDocument().getLength()) {
                wordEnd = Utilities.getWordEnd(textPane, caretPos + 1);
            }
            textPane.setCaretPosition(wordEnd);
        } catch (BadLocationException ignore) {
            // do nothing
        }
    }

    private void altShiftLeft(ActionEvent event) {
        // alt-shift-left, select from previous word start to current selection end
        try {
            int caretPos = textPane.getCaretPosition();
            if (caretPos > 0) {
                int start = Utilities.getWordStart(textPane, caretPos - 1);
                int end = textPane.getSelectionEnd();
                textPane.setCaretPosition(end);
                textPane.moveCaretPosition(start);
            }
        } catch (BadLocationException ignore) {
            // do nothing
        }
    }

    private void altShiftRight(ActionEvent event) {
        // alt-shift-right, select from current selection start to next word end
        try {
            int caretPos = textPane.getCaretPosition();
            if (caretPos < textPane.getDocument().getLength()) {
                int start = textPane.getSelectionStart();
                int end = Utilities.getWordEnd(textPane, caretPos + 1);
                textPane.setCaretPosition(start);
                textPane.moveCaretPosition(end);
            }
        } catch (BadLocationException ignore) {
            // do nothing
        }
    }

    private void cmdSlashToggleComment(ActionEvent event) {
        // cmd-fwd-slash, toggle comment on line or selection
        try {
            int start = Utilities.getRowStart(textPane, textPane.getSelectionStart());
            int end = Utilities.getRowEnd(textPane, textPane.getSelectionEnd());
            Document doc = textPane.getDocument();
            int len = end - start;
            String lines = doc.getText(start, len);
            int linesLen = lines.length();
            sb.setLength(0);
            int lineStart = 0;
            for (int i = 0; i < linesLen; i++) {
                char c = lines.charAt(i);
                if (c == '\n') {
                    if (i - lineStart >= 2 &&
                            lines.charAt(lineStart) == '-' &&
                            lines.charAt(lineStart + 1) == '-') {
                        sb.append(lines, lineStart + 2, i + 1);
                    } else {
                        sb.append("--").append(lines, lineStart, i + 1);
                    }
                    lineStart = i + 1;
                }
            }
            if (linesLen - lineStart >= 2 &&
                    lines.charAt(lineStart) == '-' &&
                    lines.charAt(lineStart + 1) == '-') {
                sb.append(lines, lineStart + 2, linesLen);
            } else {
                sb.append("--").append(lines, lineStart, linesLen);
            }
            doc.remove(start, len);
            doc.insertString(start, sb.toString(), null);
            highlighter.handleTextChanged();
        } catch (Exception fail) {
            // do nothing
        }
    }

    private void cmdQuoteToggleQuote(ActionEvent event) {
        // cmd-quote, toggle quote
        try {
            Document doc = textPane.getDocument();
            int docLen = doc.getLength();
            int start = textPane.getSelectionStart();
            int end = textPane.getSelectionEnd();
            int len = end - start;
            if (len == 0 && (start == 0 || end == docLen)) {
                doc.insertString(textPane.getCaretPosition(), "''", null);
            } else if (start > 0 && len <= docLen) {
                String targetText = doc.getText(start, len);
                String window = doc.getText(start - 1, len + 2);
                char first = window.charAt(0);
                char last = window.charAt(len + 1);
                String finalText;
                if ((first == '\'' || first == '"') && first == last) {
                    finalText = targetText;
                    start--;
                    len += 2;
                } else {
                    finalText = "'" + targetText + "'";
                }
                doc.remove(start, len);
                doc.insertString(textPane.getCaretPosition(), finalText, null);
            }
        } catch (Exception fail) {
            // do nothing
        }
    }

    private void cmdASelectAll(ActionEvent event) {
        // cmd-a, select all
        textPane.selectAll();
    }

    private void setupKeyboardActions(boolean isErrorPanel) {
        // cmd/cmd+shift
        GTk.addCmdKeyAction(KeyEvent.VK_UP, textPane, this::cmdUp);
        GTk.addCmdKeyAction(KeyEvent.VK_DOWN, textPane, this::cmdDown);
        GTk.addCmdKeyAction(KeyEvent.VK_LEFT, textPane, this::cmdLeft);
        GTk.addCmdKeyAction(KeyEvent.VK_RIGHT, textPane, this::cmdRight);
        GTk.addCmdShiftKeyAction(KeyEvent.VK_UP, textPane, this::cmdShiftUp);
        GTk.addCmdShiftKeyAction(KeyEvent.VK_DOWN, textPane, this::cmdShiftDown);
        GTk.addCmdShiftKeyAction(KeyEvent.VK_LEFT, textPane, this::cmdShiftLeft);
        GTk.addCmdShiftKeyAction(KeyEvent.VK_RIGHT, textPane, this::cmdShiftRight);
        GTk.addCmdKeyAction(KeyEvent.VK_A, textPane, this::cmdASelectAll);
        GTk.addCmdKeyAction(KeyEvent.VK_C, textPane, this::cmdCCopyToClipboard);
        if (!isErrorPanel) {
            GTk.addCmdKeyAction(KeyEvent.VK_X, textPane, this::cmdXCutToClipboard);
            GTk.addCmdKeyAction(KeyEvent.VK_V, textPane, this::cmdVPasteFromClipboard);
            GTk.addCmdKeyAction(KeyEvent.VK_D, textPane, this::cmdDDupLine);
            GTk.addCmdKeyAction(KeyEvent.VK_Z, textPane, this::cmdZUndo);
            GTk.addCmdKeyAction(KeyEvent.VK_Y, textPane, this::cmdYRedo);
            GTk.addCmdKeyAction(KeyEvent.VK_SLASH, textPane, this::cmdSlashToggleComment);
            GTk.addCmdKeyAction(KeyEvent.VK_QUOTE, textPane, this::cmdQuoteToggleQuote);
        }
        // alt/alt+shift
        GTk.addAltKeyAction(KeyEvent.VK_UP, textPane, this::altUp);
        GTk.addAltKeyAction(KeyEvent.VK_LEFT, textPane, this::altLeft);
        GTk.addAltKeyAction(KeyEvent.VK_RIGHT, textPane, this::altRight);
        GTk.addAltShiftKeyAction(KeyEvent.VK_LEFT, textPane, this::altShiftLeft);
        GTk.addAltShiftKeyAction(KeyEvent.VK_RIGHT, textPane, this::altShiftRight);
    }
}