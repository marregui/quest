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

import io.quest.frontend.GTk;

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

public class Highlighter extends DocumentFilter {
    public static final String EVENT_TYPE = "style change";

    public static Highlighter of(JTextPane textPane) {
        Highlighter highlighter = new Highlighter(textPane.getStyledDocument()); // produces EVENT_TYPE
        AbstractDocument doc = (AbstractDocument) textPane.getDocument();
        doc.putProperty(DefaultEditorKit.EndOfLineStringProperty, "\n");
        doc.setDocumentFilter(highlighter);
        return highlighter;
    }

    protected final StyledDocument styledDocument;
    private final StringBuilder errorBuilder;
    private final int errorHeaderLen;
    private final WeakHashMap<String, Pattern> findPatternCache;

    protected Highlighter(StyledDocument styledDocument) {
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

    public String highlightError(Throwable error) {
        errorBuilder.setLength(errorHeaderLen);
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            error.printStackTrace(pw);
            errorBuilder.append(sw);
            return errorBuilder.toString();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String highlightError(String error) {
        errorBuilder.setLength(errorHeaderLen);
        errorBuilder.append(error);
        return errorBuilder.toString();
    }

    public int handleTextChanged() {
        return handleTextChanged(null, null);
    }

    public int handleTextChanged(String findRegex, String replaceWith) {
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
                handleTextChanged(txt);
                return applyFindReplace(findRegex, replaceWith, txt);
            }
        }
        return 0;
    }

    protected int handleTextChanged(String txt) {
        return applyStyle(KEYWORDS_PATTERN.matcher(txt), HIGHLIGHT_KEYWORD, false);
    }

    protected int applyStyle(Matcher matcher, AttributeSet style, boolean replace) {
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


    // | Boundary Construct | Description             |
    // | ================== | ======================= |
    // |       ^            | The beginning of a line |
    // |       $            | The end of a line       |
    // |       \\b          | A word boundary         |
    // |       \\B          | A non-word boundary     |
    // https://docs.oracle.com/javase/tutorial/essential/regex/bounds.html

    protected static final int PATTERN_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final String NON_KEYWORDS = "|;|,|\\.|\\(|\\)";
    private static final Pattern KEYWORDS_PATTERN = Pattern.compile(
            // src/main/python/keywords.py
            "\\bindex\\b|\\bday\\b|\\bdouble\\b|\\bas\\b|\\blt\\b|\\block\\b|"
                    + "\\bexcept\\b|\\bisolation\\b|\\bgrant\\b|\\bdrop\\b|\\bintersect\\b|"
                    + "\\bcopy\\b|\\bnot\\b|\\bover\\b|\\bwith\\b|\\bdate\\b|\\brepair\\b|"
                    + "\\bright\\b|\\bnocache\\b|\\bbackup\\b|\\bouter\\b|\\bif\\b|\\bshow\\b|"
                    + "\\bby\\b|\\bfrom\\b|\\btransaction\\b|\\blevel\\b|\\bselect\\b|\\bbyte\\b|"
                    + "\\bmonth\\b|\\bjoin\\b|\\basc\\b|\\binto\\b|\\bnan\\b|\\bcolumns\\b|"
                    + "\\breferences\\b|\\btable\\b|\\bhour\\b|\\bcast\\b|\\bsample\\b|\\bdistinct\\b|"
                    + "\\btruncate\\b|\\basof\\b|\\bpartition\\b|\\ball\\b|\\bdefault\\b|"
                    + "\\bon\\b|\\bdelete\\b|\\bforeign\\b|\\belse\\b|\\bin\\b|\\bcreate\\b|"
                    + "\\bheader\\b|\\balter\\b|\\bto\\b|\\bleft\\b|\\bvalues\\b|\\bquestdb\\b|"
                    + "\\bcache\\b|\\bbetween\\b|\\bnatural\\b|\\bdesc\\b|\\bonly\\b|\\bbinary\\b|"
                    + "\\bnone\\b|\\bcross\\b|\\bunion\\b|\\bupdate\\b|\\bcolumn\\b|\\brename\\b|"
                    + "\\bthen\\b|\\bwhere\\b|\\badd\\b|\\blong\\b|\\bgeohash\\b|\\bboolean\\b|"
                    + "\\bint\\b|\\btables\\b|\\bwhen\\b|\\bgroup\\b|\\bnull\\b|\\bshort\\b|"
                    + "\\bkey\\b|\\binner\\b|\\btype\\b|\\bexists\\b|\\bfloat\\b|\\bunlock\\b|"
                    + "\\bchar\\b|\\bsymbol\\b|\\border\\b|\\bwriter\\b|\\bstring\\b|\\bcase\\b|"
                    + "\\blatest\\b|\\bsplice\\b|\\bdatabase\\b|\\bor\\b|\\bcapacity\\b|\\band\\b|"
                    + "\\bend\\b|\\bfill\\b|\\bprimary\\b|\\binsert\\b|\\bsystem\\b|\\blong256\\b|"
                    + "\\blimit\\b|\\btimestamp\\b" + NON_KEYWORDS,
            PATTERN_FLAGS);
    private static final String ERROR_HEADER = "==========  ERROR  ==========\n";
    private static final Pattern ERROR_HEADER_PATTERN = Pattern.compile(ERROR_HEADER);
    protected AttributeSet HIGHLIGHT_NORMAL = styleForegroundColor(95, 235, 150); // terminal green
    protected AttributeSet HIGHLIGHT_KEYWORD = styleForegroundColor(
            GTk.APP_THEME_COLOR.getRed(),
            GTk.APP_THEME_COLOR.getGreen(),
            GTk.APP_THEME_COLOR.getBlue()); // red
    private AttributeSet HIGHLIGHT_FIND_MATCH = styleForegroundColor(
            Color.YELLOW.getRed(),
            Color.YELLOW.getGreen(),
            Color.YELLOW.getBlue());
    private AttributeSet HIGHLIGHT_ERROR = styleForegroundColor(255, 55, 5); // bright red

    protected static AttributeSet styleForegroundColor(int r, int g, int b) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        return sc.addAttribute(sc.getEmptySet(), StyleConstants.Foreground, new Color(r, g, b));
    }
}
