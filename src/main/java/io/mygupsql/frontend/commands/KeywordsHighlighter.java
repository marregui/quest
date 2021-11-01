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

package io.mygupsql.frontend.commands;

import io.mygupsql.GTk;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.util.Objects;
import java.util.WeakHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

class KeywordsHighlighter extends DocumentFilter {

    private final static Object FOREGROUND = StyleConstants.Foreground;
    private final static StyleContext STYLE = StyleContext.getDefaultStyleContext();
    private final static AttributeSet NORMAL = STYLE.addAttribute(STYLE.getEmptySet(), FOREGROUND, Color.WHITE);
    private final static AttributeSet ERROR = STYLE.addAttribute(STYLE.getEmptySet(), FOREGROUND, Color.ORANGE);
    private final static AttributeSet KEYWORD = STYLE.addAttribute(STYLE.getEmptySet(), FOREGROUND, GTk.APP_THEME_COLOR);
    private final static AttributeSet HIGHLIGHT = STYLE.addAttribute(STYLE.getEmptySet(), FOREGROUND, Color.CYAN);


    private final StyledDocument styledDocument;
    private final WeakHashMap<String, Pattern> findPatternCache;

    KeywordsHighlighter(StyledDocument styledDocument) {
        this.styledDocument = Objects.requireNonNull(styledDocument);
        findPatternCache = new WeakHashMap<>(5, 0.2f); // one at the time
    }

    @Override
    public void insertString(FilterBypass fb, int offset, String text, AttributeSet attributeSet) {
        try {
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
            super.replace(fb, offset, length, text, attrSet);
        } catch (BadLocationException irrelevant) {
            // do nothing
        }
        handleTextChanged();
    }

    void handleTextChanged() {
        handleTextChanged(null);
    }

    int handleTextChanged(String findRegex) {
        int len = styledDocument.getLength();
        String txt;
        try {
            txt = styledDocument.getText(0, len);
        } catch (BadLocationException impossible) {
            return 0;
        }
        if (ERROR_HEADER_PATTERN.matcher(txt).find()) {
            styledDocument.setCharacterAttributes(0, len, ERROR, true);
        } else {
            styledDocument.setCharacterAttributes(0, len, NORMAL, true);
            applyStyle(KEYWORDS_PATTERN.matcher(txt), KEYWORD, false);
            if (findRegex != null && !findRegex.isBlank()) {
                Pattern findPattern = findPatternCache.get(findRegex);
                if (findPattern == null) {
                    try {
                        findPattern = Pattern.compile(findRegex, KEYWORDS_PATTERN_FLAGS);
                        findPatternCache.put(findRegex, findPattern);
                    } catch (PatternSyntaxException err) {
                        JOptionPane.showMessageDialog(null, String.format(
                                "Not a valid filter: %s", findRegex));
                        return 0;
                    }
                }
                return applyStyle(findPattern.matcher(txt), HIGHLIGHT, true);
            }
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

    private static final Pattern ERROR_HEADER_PATTERN = Pattern.compile(TextPane.ERROR_HEADER);

    // Execute <b>src/test/python/keywords.py</b>, then copy the results to the
    // first parameter of Pattern.compile.
    private static final int KEYWORDS_PATTERN_FLAGS = Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE;
    private static final Pattern KEYWORDS_PATTERN = Pattern.compile(
            "\\bquestdb\\b|\\badd\\b|\\ball\\b|\\balter\\b|\\band\\b|\\bas\\b|\\basc\\b"
                    + "|\\basof\\b|\\bbackup\\b|\\bbetween\\b|\\bby\\b|\\bcache\\b"
                    + "|\\bcapacity\\b|\\bcase\\b|\\bcast\\b|\\bcolumn\\b|\\bcolumns\\b"
                    + "|\\bcopy\\b|\\bcreate\\b|\\bcross\\b|\\bdatabase\\b|\\bdefault\\b"
                    + "|\\bdelete\\b|\\bdesc\\b|\\bdistinct\\b|\\bdrop\\b|\\belse\\b"
                    + "|\\bend\\b|\\bexcept\\b|\\bexists\\b|\\bfill\\b|\\bforeign\\b"
                    + "|\\bfrom\\b|\\bgrant\\b|\\bgroup\\b|\\bheader\\b|\\bif\\b"
                    + "|\\bin\\b|\\bindex\\b|\\binner\\b|\\binsert\\b|\\bintersect\\b"
                    + "|\\binto\\b|\\bisolation\\b|\\bjoin\\b|\\bkey\\b|\\blatest\\b"
                    + "|\\bleft\\b|\\blevel\\b|\\blimit\\b|\\block\\b|\\blt\\b|\\bnan\\b"
                    + "|\\bnatural\\b|\\bnocache\\b|\\bnone\\b|\\bnot\\b|\\bnull\\b"
                    + "|\\bon\\b|\\bonly\\b|\\bor\\b|\\border\\b|\\bouter\\b|\\bover\\b"
                    + "|\\bpartition\\b|\\bprimary\\b|\\breferences\\b|\\brename\\b"
                    + "|\\brepair\\b|\\bright\\b|\\bsample\\b|\\bselect\\b|\\bshow\\b"
                    + "|\\bsplice\\b|\\bsystem\\b|\\btable\\b|\\btables\\b|\\bthen\\b"
                    + "|\\bto\\b|\\btransaction\\b|\\btruncate\\b|\\btype\\b|\\bunion\\b"
                    + "|\\bunlock\\b|\\bupdate\\b|\\bvalues\\b|\\bwhen\\b|\\bwhere\\b"
                    + "|\\bwith\\b|\\bwriter\\b",
            KEYWORDS_PATTERN_FLAGS);
}