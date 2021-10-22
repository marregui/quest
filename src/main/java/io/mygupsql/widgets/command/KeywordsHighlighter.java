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

package io.mygupsql.widgets.command;

import io.mygupsql.GTk;

import javax.swing.text.*;
import java.awt.*;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class KeywordsHighlighter extends DocumentFilter {

    private final static Object FOREGROUND = StyleConstants.Foreground;
    private final static StyleContext STYLE = StyleContext.getDefaultStyleContext();
    private final static AttributeSet NORMAL = STYLE.addAttribute(STYLE.getEmptySet(), FOREGROUND, Color.WHITE);
    private final static AttributeSet ERROR = STYLE.addAttribute(STYLE.getEmptySet(), FOREGROUND, Color.ORANGE);
    private final static AttributeSet KEYWORD = STYLE.addAttribute(STYLE.getEmptySet(), FOREGROUND, GTk.APP_THEME_COLOR);


    private final StyledDocument styledDocument;

    KeywordsHighlighter(StyledDocument styledDocument) {
        this.styledDocument = Objects.requireNonNull(styledDocument);
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
        int len = styledDocument.getLength();
        String txt;
        try {
            txt = styledDocument.getText(0, len);

        } catch (BadLocationException impossible) {
            return;
        }
        if (ERROR_HEADER_PATTERN.matcher(txt).find()) {
            styledDocument.setCharacterAttributes(0, len, ERROR, true);
        } else {
            styledDocument.setCharacterAttributes(0, len, NORMAL, true);
            Matcher matcher = KEYWORDS_PATTERN.matcher(txt);
            while (matcher.find()) {
                int start = matcher.start();
                len = matcher.end() - start;
                styledDocument.setCharacterAttributes(start, len, KEYWORD, false);
            }
        }
    }

    private static final Pattern ERROR_HEADER_PATTERN = Pattern.compile(TextPane.ERROR_HEADER);

    // Execute <b>src/test/python/keywords.py</b>, then copy the results to the
    // first parameter of Pattern.compile.
    private static final Pattern KEYWORDS_PATTERN = Pattern.compile(
            "\\bcrate\\b|\\badd\\b|\\balias\\b|\\ball\\b|\\ballocate\\b|\\balter\\b"
                    + "|\\balways\\b|\\banalyze\\b|\\banalyzer\\b|\\band\\b|\\bany\\b"
                    + "|\\barray\\b|\\bartifacts\\b|\\bas\\b|\\basc\\b|\\bat\\b"
                    + "|\\bauthorization\\b|\\bbegin\\b|\\bbernoulli\\b|\\bbetween\\b"
                    + "|\\bblob\\b|\\bboolean\\b|\\bboth\\b|\\bby\\b|\\bbyte\\b"
                    + "|\\bcalled\\b|\\bcancel\\b|\\bcase\\b|\\bcast\\b|\\bcatalogs\\b"
                    + "|\\bcharacter\\b|\\bcharacteristics\\b|\\bcheck\\b|\\bclose\\b"
                    + "|\\bcluster\\b|\\bclustered\\b|\\bcolumn\\b|\\bcolumns\\b|\\bcommit\\b"
                    + "|\\bcommitted\\b|\\bconflict\\b|\\bconstraint\\b|\\bcopy\\b"
                    + "|\\bcreate\\b|\\bcross\\b|\\bcurrent\\b|\\bdangling\\b|\\bday\\b"
                    + "|\\bdeallocate\\b|\\bdecommission\\b|\\bdefault\\b|\\bdeferrable\\b"
                    + "|\\bdelete\\b|\\bdeny\\b|\\bdesc\\b|\\bdescribe\\b|\\bdirectory\\b"
                    + "|\\bdiscard\\b|\\bdistinct\\b|\\bdistributed\\b|\\bdo\\b|\\bdouble\\b"
                    + "|\\bdrop\\b|\\bduplicate\\b|\\bdynamic\\b|\\belse\\b|\\bend\\b"
                    + "|\\bescape\\b|\\bexcept\\b|\\bexists\\b|\\bexplain\\b|\\bextends\\b"
                    + "|\\bextract\\b|\\bfailed\\b|\\bfalse\\b|\\bfilter\\b|\\bfirst\\b"
                    + "|\\bfloat\\b|\\bfollowing\\b|\\bfor\\b|\\bformat\\b|\\bfrom\\b"
                    + "|\\bfull\\b|\\bfulltext\\b|\\bfunction\\b|\\bfunctions\\b|\\bgc\\b"
                    + "|\\bgenerated\\b|\\bglobal\\b|\\bgrant\\b|\\bgraphviz\\b|\\bgroup\\b"
                    + "|\\bhaving\\b|\\bhour\\b|\\bif\\b|\\bignored\\b|\\bilike\\b"
                    + "|\\bin\\b|\\bindex\\b|\\binner\\b|\\binput\\b|\\binsert\\b"
                    + "|\\bint\\b|\\binteger\\b|\\bintersect\\b|\\binterval\\b|\\binto\\b"
                    + "|\\bip\\b|\\bis\\b|\\bisolation\\b|\\bjoin\\b|\\bkey\\b|\\bkill\\b"
                    + "|\\blanguage\\b|\\blast\\b|\\bleading\\b|\\bleft\\b|\\blevel\\b"
                    + "|\\blicense\\b|\\blike\\b|\\blimit\\b|\\blocal\\b|\\blogical\\b"
                    + "|\\blong\\b|\\bmatch\\b|\\bmaterialized\\b|\\bmetadata\\b|\\bminute\\b"
                    + "|\\bmonth\\b|\\bmove\\b|\\bnatural\\b|\\bnot\\b|\\bnothing\\b"
                    + "|\\bnull\\b|\\bnulls\\b|\\bobject\\b|\\boff\\b|\\boffset\\b"
                    + "|\\bon\\b|\\bonly\\b|\\bopen\\b|\\boptimize\\b|\\bor\\b|\\border\\b"
                    + "|\\bouter\\b|\\bover\\b|\\bpartition\\b|\\bpartitioned\\b|\\bpartitions\\b"
                    + "|\\bpersistent\\b|\\bplain\\b|\\bplans\\b|\\bpreceding\\b|\\bprecision\\b"
                    + "|\\bprepare\\b|\\bprivileges\\b|\\bpromote\\b|\\brange\\b|\\bread\\b"
                    + "|\\brecursive\\b|\\brefresh\\b|\\brename\\b|\\brepeatable\\b"
                    + "|\\breplace\\b|\\breplica\\b|\\brepository\\b|\\breroute\\b"
                    + "|\\breset\\b|\\brestore\\b|\\bretry\\b|\\breturn\\b|\\breturning\\b"
                    + "|\\breturns\\b|\\brevoke\\b|\\bright\\b|\\brow\\b|\\brows\\b"
                    + "|\\bschema\\b|\\bschemas\\b|\\bsecond\\b|\\bselect\\b|\\bsequences\\b"
                    + "|\\bserializable\\b|\\bsession\\b|\\bset\\b|\\bshard\\b|\\bshards\\b"
                    + "|\\bshort\\b|\\bshow\\b|\\bsnapshot\\b|\\bsome\\b|\\bstart\\b"
                    + "|\\bstorage\\b|\\bstratify\\b|\\bstrict\\b|\\bstring\\b|\\bsubstring\\b"
                    + "|\\bsummary\\b|\\bswap\\b|\\bsystem\\b|\\btable\\b|\\btables\\b"
                    + "|\\btablesample\\b|\\btemp\\b|\\btemporary\\b|\\btext\\b|\\bthen\\b"
                    + "|\\btime\\b|\\btimestamp\\b|\\bto\\b|\\btokenizer\\b|\\btrailing\\b"
                    + "|\\btransaction\\b|\\btransient\\b|\\btrim\\b|\\btrue\\b|\\btype\\b"
                    + "|\\bunbounded\\b|\\buncommitted\\b|\\bunion\\b|\\bupdate\\b"
                    + "|\\buser\\b|\\busing\\b|\\bvalues\\b|\\bvarying\\b|\\bview\\b"
                    + "|\\bwhen\\b|\\bwhere\\b|\\bwindow\\b|\\bwith\\b|\\bwithout\\b"
                    + "|\\bwork\\b|\\bwrite\\b|\\byear\\b|\\bzone\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
}
