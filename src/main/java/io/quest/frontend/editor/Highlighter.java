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

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

class Highlighter extends DocumentFilter {

    static final String EVENT_TYPE = "style change";

    static Highlighter of(JTextPane textPane) {
        Highlighter highlighter = new Highlighter(textPane.getStyledDocument()); // produces EVENT_TYPE
        AbstractDocument doc = (AbstractDocument) textPane.getDocument();
        doc.putProperty(DefaultEditorKit.EndOfLineStringProperty, "\n");
        doc.setDocumentFilter(highlighter);
        return highlighter;
    }

    private final StyledDocument styledDocument;
    private final StringBuilder errorBuilder;
    private final int errorHeaderLen;
    private final WeakHashMap<String, Pattern> findPatternCache;

    Highlighter(StyledDocument styledDocument) {
        this.styledDocument = Objects.requireNonNull(styledDocument);
        findPatternCache = new WeakHashMap<>(5, 0.2f); // one at the time
        errorBuilder = new StringBuilder();
        errorBuilder.append("\n").append(ERROR_HEADER).append("\n");
        errorHeaderLen = errorBuilder.length();
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String text, AttributeSet attributeSet) {
        try {
            super.insertString(fb, offset, replaceAllTabs(text), attributeSet);
            handleTextChanged();
        } catch (BadLocationException irrelevant) {
            // do nothing
        }
    }

    @Override
    public void remove(FilterBypass fb, int offset, int length) {
        try {
            super.remove(fb, offset, length);
            handleTextChanged();
        } catch (BadLocationException irrelevant) {
            // do nothing
        }
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrSet) {
        try {
            super.replace(fb, offset, length, replaceAllTabs(text), attrSet);
            handleTextChanged();
        } catch (BadLocationException irrelevant) {
            // do nothing
        }
    }

    String highlightError(Throwable error) {
        errorBuilder.setLength(errorHeaderLen);
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            error.printStackTrace(pw);
            errorBuilder.append(sw);
            return errorBuilder.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    int handleTextChanged() {
        return handleTextChanged(null, null);
    }

    int handleTextChanged(String findRegex, String replaceWith) {
        int len = styledDocument.getLength();
        if (len > 0) {
            String txt;
            try {
                txt = styledDocument.getText(0, len);
            } catch (BadLocationException impossible) {
                return 0;
            }
            if (ERROR_HEADER_PATTERN.matcher(txt).find()) {
                styledDocument.setCharacterAttributes(0, len, HIGHLIGHT_ERROR, true);
            } else {
                styledDocument.setCharacterAttributes(0, len, HIGHLIGHT_NORMAL, true);
                applyStyle(KEYWORDS_PATTERN.matcher(txt), HIGHLIGHT_KEYWORD, false);
                applyStyle(TYPES_PATTERN.matcher(txt), HIGHLIGHT_TYPE, false);
                return applyFindReplace(findRegex, replaceWith, txt);
            }
        }
        return 0;
    }

    private int applyFindReplace(String findRegex, String replaceWith, String txt) {
        if (findRegex != null && !findRegex.isBlank()) {
            Pattern find = findPatternCache.get(findRegex);
            if (find == null) {
                try {
                    find = Pattern.compile(findRegex, PATTERN_FLAGS);
                    findPatternCache.put(findRegex, find);
                } catch (PatternSyntaxException err) {
                    JOptionPane.showMessageDialog(
                            null,
                            String.format("Not a valid regex: %s", findRegex)
                    );
                    return 0;
                }
            }
            return replaceWith == null ?
                    applyStyle(find.matcher(txt), HIGHLIGHT_FIND_MATCH, true)
                    :
                    replaceAllWith(find.matcher(txt), replaceWith);
        }
        return 0;
    }

    private int applyStyle(Matcher matcher, AttributeSet style, boolean replace) {
        int matchCount = 0;
        while (matcher.find()) {
            styledDocument.setCharacterAttributes(
                    matcher.start(),
                    matcher.end() - matcher.start(),
                    style,
                    replace);
            matchCount++;
        }
        return matchCount;
    }

    private static String replaceAllTabs(String text) {
        return text.replaceAll("\t", "    ");
    }

    private int replaceAllWith(Matcher matcher, String replaceWith) {
        int matchCount = 0;
        while (matcher.find()) {
            matchCount++;
        }
        matcher.reset();
        matcher.replaceAll(replaceWith);
        return matchCount;
    }

    // src/test/python/keywords.py
    // https://docs.oracle.com/javase/tutorial/essential/regex/bounds.html
    static final int PATTERN_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    static final Pattern TYPES_PATTERN = Pattern.compile(
            "\\bbinary\\b|\\bint\\b|\\bcapacity\\b|\\bshort\\b|\\btimestamp\\b|"
                    + "\\bboolean\\b|\\bbyte\\b|\\bindex\\b|\\bnocache\\b|\\bcache\\b|\\bnull\\b|"
                    + "\\blong\\b|\\bdate\\b|\\bstring\\b|\\bsymbol\\b|\\bdouble\\b|\\bfloat\\b|"
                    + "\\blong256\\b|\\bchar\\b|\\bgeohash\\b",
            PATTERN_FLAGS);
    static final Pattern KEYWORDS_PATTERN = Pattern.compile(
            "\\bquestdb\\b|\\badd\\b|\\ball\\b|\\balter\\b|\\band\\b|\\bas\\b|"
                    + "\\basc\\b|\\basof\\b|\\bbackup\\b|\\bbetween\\b|\\bby\\b|\\bcase\\b|"
                    + "\\bcast\\b|\\bcolumn\\b|\\bcolumns\\b|\\bcopy\\b|\\bcreate\\b|\\bcross\\b|"
                    + "\\bdatabase\\b|\\bdefault\\b|\\bdelete\\b|\\bdesc\\b|\\bdistinct\\b|"
                    + "\\bdrop\\b|\\belse\\b|\\bend\\b|\\bexcept\\b|\\bexists\\b|\\bfill\\b|"
                    + "\\bforeign\\b|\\bfrom\\b|\\bgrant\\b|\\bgroup\\b|\\bheader\\b|\\bif\\b|"
                    + "\\bin\\b|\\binner\\b|\\binsert\\b|\\bintersect\\b|\\binto\\b|\\bisolation\\b|"
                    + "\\bjoin\\b|\\bkey\\b|\\blatest\\b|\\bleft\\b|\\blevel\\b|\\blimit\\b|"
                    + "\\block\\b|\\blt\\b|\\bnan\\b|\\bnatural\\b|\\bnone\\b|\\bnot\\b|"
                    + "\\bon\\b|\\bonly\\b|\\bor\\b|\\border\\b|\\bouter\\b|\\bover\\b|"
                    + "\\bpartition\\b|\\bprimary\\b|\\breferences\\b|\\brename\\b|\\brepair\\b|"
                    + "\\bright\\b|\\bsample\\b|\\bselect\\b|\\bshow\\b|\\bsplice\\b|\\bsystem\\b|"
                    + "\\btable\\b|\\btables\\b|\\bthen\\b|\\bto\\b|\\btransaction\\b|\\btruncate\\b|"
                    + "\\btype\\b|\\bunion\\b|\\bunlock\\b|\\bupdate\\b|\\bvalues\\b|\\bwhen\\b|"
                    + "\\bwhere\\b|\\bwith\\b|\\bwriter\\b",
            PATTERN_FLAGS);
    static final String ERROR_HEADER = "==========  ERROR  ==========\n";
    static final Pattern ERROR_HEADER_PATTERN = Pattern.compile(ERROR_HEADER);
    AttributeSet HIGHLIGHT_NORMAL = styleForegroundColor(55, 190, 55);
    AttributeSet HIGHLIGHT_KEYWORD = styleForegroundColor(200, 50, 100);
    AttributeSet HIGHLIGHT_TYPE = styleForegroundColor(240, 10, 140);
    AttributeSet HIGHLIGHT_FIND_MATCH = styleForegroundColor(50, 200, 185);
    AttributeSet HIGHLIGHT_ERROR = styleForegroundColor(255, 55, 5);

    private static AttributeSet styleForegroundColor(int r, int g, int b) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        return sc.addAttribute(sc.getEmptySet(), StyleConstants.Foreground, new Color(r, g, b));
    }
}
