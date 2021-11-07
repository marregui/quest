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

import io.quest.frontend.GTk;

import javax.swing.*;
import javax.swing.text.*;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

class Highlighter extends DocumentFilter {

    public final static AttributeSet HIGHLIGHT_NORMAL = GTk.styleForegroundColor(255, 245, 222);
    public final static AttributeSet HIGHLIGHT_ERROR = GTk.styleForegroundColor(225, 125, 5);
    public final static AttributeSet HIGHLIGHT_KEYWORD = GTk.styleForegroundColor(200, 50, 100);
    public final static AttributeSet HIGHLIGHT_TYPE = GTk.styleForegroundColor(240, 10, 140);
    public final static AttributeSet HIGHLIGHT_MATCH = GTk.styleForegroundColor(50, 200, 185);

    private final StyledDocument styledDocument;
    private final WeakHashMap<String, Pattern> findPatternCache;

    Highlighter(StyledDocument styledDocument) {
        this.styledDocument = Objects.requireNonNull(styledDocument);
        findPatternCache = new WeakHashMap<>(5, 0.2f); // one at the time
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String text, AttributeSet attributeSet) {
        try {
            text = text.replaceAll("\t", "    ");
            super.insertString(fb, offset, text, attributeSet);
        } catch (BadLocationException irrelevant) {
            // do nothing
        }
        handleTextChanged();
    }

    @Override
    public void remove(FilterBypass fb, int offset, int length) {
        try {
            super.remove(fb, offset, length);
        } catch (BadLocationException irrelevant) {
            // do nothing
        }
        handleTextChanged();
    }

    @Override
    public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrSet) {
        try {
            text = text.replaceAll("\t", "    ");
            super.replace(fb, offset, length, text, attrSet);
        } catch (BadLocationException irrelevant) {
            // do nothing
        }
        handleTextChanged();
    }

    int handleTextChanged() {
        return handleTextChanged(null, null);
    }

    int handleTextChanged(String findRegex) {
        return handleTextChanged(findRegex, null);
    }

    int handleTextChanged(String findRegex, String replaceWith) {
        int len = styledDocument.getLength();
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
            if (findRegex != null && !findRegex.isBlank()) {
                Pattern findPattern = findPatternCache.get(findRegex);
                if (findPattern == null) {
                    try {
                        findPattern = Pattern.compile(findRegex, PATTERN_FLAGS);
                        findPatternCache.put(findRegex, findPattern);
                    } catch (PatternSyntaxException err) {
                        JOptionPane.showMessageDialog(null, String.format(
                                "Not a valid filter: %s", findRegex));
                        return 0;
                    }
                }
                if (replaceWith != null) {
                    return replaceAll(findPattern.matcher(txt), replaceWith);
                }
                return replaceWith != null ?
                        replaceAll(findPattern.matcher(txt), replaceWith)
                        :
                        applyStyle(findPattern.matcher(txt), HIGHLIGHT_MATCH, true);
            }
        }
        return 0;
    }

    private int replaceAll(Matcher matcher, String replaceWith) {
        int matchCount = 0;
        while (matcher.find()) {
            matchCount++;
        }
        matcher.reset();
        matcher.replaceAll(replaceWith);
        return matchCount;
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

    private static final Pattern ERROR_HEADER_PATTERN = Pattern.compile(TextPanel.ERROR_HEADER);
    private static final int PATTERN_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    // src/test/python/keywords.py
    // https://docs.oracle.com/javase/tutorial/essential/regex/bounds.html
    private static final Pattern TYPES_PATTERN = Pattern.compile(
            "\\bbinary\\b|\\bint\\b|\\bcapacity\\b|\\bshort\\b|\\btimestamp\\b|"
                    + "\\bboolean\\b|\\bbyte\\b|\\bindex\\b|\\bnocache\\b|\\bcache\\b|\\bnull\\b|"
                    + "\\blong\\b|\\bdate\\b|\\bstring\\b|\\bsymbol\\b|\\bdouble\\b|\\bfloat\\b|"
                    + "\\blong256\\b|\\bchar\\b|\\bgeohash\\b",
            PATTERN_FLAGS);
    private static final Pattern KEYWORDS_PATTERN = Pattern.compile(
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
}
