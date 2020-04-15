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
package io.crate.cli.common;

import io.crate.cli.widgets.CommandBoardManager;

import javax.swing.text.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class KeywordDocumentFilter extends DocumentFilter {

    private final static StyleContext STYLE_CONTEXT = StyleContext.getDefaultStyleContext();
    private final static AttributeSet ERROR = STYLE_CONTEXT.addAttribute(
            STYLE_CONTEXT.getEmptySet(),
            StyleConstants.Foreground,
            GUIToolkit.ERROR_FONT_COLOR);
    private final static AttributeSet NORMAL = STYLE_CONTEXT.addAttribute(
            STYLE_CONTEXT.getEmptySet(),
            StyleConstants.Foreground,
            CommandBoardManager.FONT_COLOR);
    private final static AttributeSet KEYWORD = STYLE_CONTEXT.addAttribute(
            STYLE_CONTEXT.getEmptySet(),
            StyleConstants.Foreground,
            CommandBoardManager.KEYWORD_FONT_COLOR);


    private final StyledDocument styledDocument;


    public KeywordDocumentFilter(StyledDocument styledDocument) {
        this.styledDocument = styledDocument;
    }

    private void handleTextChanged() throws BadLocationException {
        int len = styledDocument.getLength();
        String text = styledDocument.getText(0, len);
        boolean isError = text.contains(GUIToolkit.ERROR_HEADER);
        if (isError) {
            styledDocument.setCharacterAttributes(0, len, ERROR, true);
        } else {
            styledDocument.setCharacterAttributes(0, len, NORMAL, true);
            Matcher matcher = KEYWORDS_PATTERN.matcher(text);
            while (matcher.find()) {
                styledDocument.setCharacterAttributes(
                        matcher.start(),
                        matcher.end() - matcher.start(),
                        KEYWORD,
                        false);
            }
        }
    }

    @Override
    public void insertString(FilterBypass fb,
                             int offset,
                             String text,
                             AttributeSet attributeSet) throws BadLocationException {
        super.insertString(fb, offset, text, attributeSet);
        handleTextChanged();
    }

    @Override
    public void remove(FilterBypass fb,
                       int offset,
                       int length) throws BadLocationException {
        super.remove(fb, offset, length);
        handleTextChanged();
    }

    @Override
    public void replace(FilterBypass fb,
                        int offset,
                        int length,
                        String text,
                        AttributeSet attributeSet) throws BadLocationException {
        super.replace(fb, offset, length, text, attributeSet);
        handleTextChanged();
    }

    private static final Pattern KEYWORDS_PATTERN = Pattern.compile(
            "\\bcrate\\b|\\badd\\b|\\balias\\b|\\ball\\b|\\ballocate\\b|\\balter\\b|\\balways\\b"
                    + "|\\banalyze\\b|\\banalyzer\\b|\\band\\b|\\bany\\b|\\barray\\b|\\bartifacts\\b"
                    + "|\\bas\\b|\\basc\\b|\\basterisk\\b|\\bat\\b|\\bbackquoted_identifier\\b"
                    + "|\\bbegin\\b|\\bbernoulli\\b|\\bbetween\\b|\\bblob\\b|\\bboolean\\b|\\bboth\\b"
                    + "|\\bby\\b|\\bbyte\\b|\\bcalled\\b|\\bcancel\\b|\\bcase\\b|\\bcast\\b|\\bcast_operator\\b"
                    + "|\\bcatalogs\\b|\\bchar_filters\\b|\\bcharacteristics\\b|\\bcheck\\b"
                    + "|\\bclose\\b|\\bcluster\\b|\\bclustered\\b|\\bcolon_ident\\b|\\bcolumn\\b"
                    + "|\\bcolumns\\b|\\bcomment\\b|\\bcommit\\b|\\bcommitted\\b|\\bconcat\\b"
                    + "|\\bconflict\\b|\\bconstraint\\b|\\bcopy\\b|\\bcreate\\b|\\bcross\\b|\\bcurrent\\b"
                    + "|\\bcurrent_date\\b|\\bcurrent_schema\\b|\\bcurrent_time\\b|\\bcurrent_timestamp\\b"
                    + "|\\bcurrent_user\\b|\\bdangling\\b|\\bday\\b|\\bdeallocate\\b|\\bdecimal_value\\b"
                    + "|\\bdecommission\\b|\\bdefault\\b|\\bdeferrable\\b|\\bdelete\\b|\\bdeny\\b"
                    + "|\\bdesc\\b|\\bdescribe\\b|\\bdigit_identifier\\b|\\bdirectory\\b|\\bdistinct\\b"
                    + "|\\bdistributed\\b|\\bdo\\b|\\bdouble\\b|\\bdrop\\b|\\bduplicate\\b|\\bdynamic\\b"
                    + "|\\belse\\b|\\bend\\b|\\beq\\b|\\bescape\\b|\\bescaped_string\\b|\\bexcept\\b"
                    + "|\\bexists\\b|\\bexplain\\b|\\bextends\\b|\\bextract\\b|\\bfailed\\b|\\bfalse\\b"
                    + "|\\bfilter\\b|\\bfirst\\b|\\bfloat\\b|\\bfollowing\\b|\\bfor\\b|\\bformat\\b"
                    + "|\\bfrom\\b|\\bfull\\b|\\bfulltext\\b|\\bfunction\\b|\\bfunctions\\b|\\bgc\\b"
                    + "|\\bgenerated\\b|\\bgeo_point\\b|\\bgeo_shape\\b|\\bglobal\\b|\\bgrant\\b"
                    + "|\\bgraphviz\\b|\\bgroup\\b|\\bgt\\b|\\bgte\\b|\\bhaving\\b|\\bhour\\b|\\bidentifier\\b"
                    + "|\\bif\\b|\\bignored\\b|\\bilike\\b|\\bin\\b|\\bindex\\b|\\binner\\b|\\binput\\b"
                    + "|\\binsert\\b|\\bint\\b|\\binteger\\b|\\binteger_value\\b|\\bintersect\\b"
                    + "|\\binterval\\b|\\binto\\b|\\bip\\b|\\bis\\b|\\bisolation\\b|\\bjoin\\b|\\bkey\\b"
                    + "|\\bkill\\b|\\blanguage\\b|\\blast\\b|\\bleading\\b|\\bleft\\b|\\blevel\\b"
                    + "|\\blicense\\b|\\blike\\b|\\blimit\\b|\\bllt\\b|\\blocal\\b|\\blogical\\b"
                    + "|\\blong\\b|\\blt\\b|\\blte\\b|\\bmatch\\b|\\bmaterialized\\b|\\bminus\\b"
                    + "|\\bminute\\b|\\bmonth\\b|\\bmove\\b|\\bnatural\\b|\\bneq\\b|\\bnot\\b|\\bnothing\\b"
                    + "|\\bnull\\b|\\bnulls\\b|\\bobject\\b|\\boff\\b|\\boffset\\b|\\bon\\b|\\bonly\\b"
                    + "|\\bopen\\b|\\boptimize\\b|\\bor\\b|\\border\\b|\\bouter\\b|\\bover\\b|\\bpartition\\b"
                    + "|\\bpartitioned\\b|\\bpartitions\\b|\\bpercent\\b|\\bpersistent\\b|\\bplain\\b"
                    + "|\\bplus\\b|\\bpreceding\\b|\\bprecision\\b|\\bprepare\\b|\\bprimary key\\b"
                    + "|\\bprimary_key\\b|\\bprivileges\\b|\\bpromote\\b|\\bquoted_identifier\\b"
                    + "|\\brange\\b|\\bread\\b|\\brecursive\\b|\\brefresh\\b|\\bregex_match\\b"
                    + "|\\bregex_match_ci\\b|\\bregex_no_match\\b|\\bregex_no_match_ci\\b"
                    + "|\\brename\\b|\\brepeatable\\b|\\breplace\\b|\\breplica\\b|\\brepository\\b"
                    + "|\\breroute\\b|\\breset\\b|\\brestore\\b|\\bretry\\b|\\breturn\\b|\\breturning\\b"
                    + "|\\breturns\\b|\\brevoke\\b|\\bright\\b|\\brow\\b|\\brows\\b|\\bschema\\b"
                    + "|\\bschemas\\b|\\bsecond\\b|\\bselect\\b|\\bsemicolon\\b|\\bserializable\\b"
                    + "|\\bsession\\b|\\bsession_user\\b|\\bset\\b|\\bshard\\b|\\bshards\\b|\\bshort\\b"
                    + "|\\bshow\\b|\\bslash\\b|\\bsnapshot\\b|\\bsome\\b|\\bstorage\\b|\\bstratify\\b"
                    + "|\\bstrict\\b|\\bstring\\b|\\bstring_type\\b|\\bsubstring\\b|\\bsummary\\b"
                    + "|\\bswap\\b|\\bsystem\\b|\\btable\\b|\\btables\\b|\\btablesample\\b|\\btext\\b"
                    + "|\\bthen\\b|\\btime\\b|\\btimestamp\\b|\\bto\\b|\\btoken_filters\\b|\\btokenizer\\b"
                    + "|\\btrailing\\b|\\btransaction\\b|\\btransaction_isolation\\b|\\btransient\\b"
                    + "|\\btrim\\b|\\btrue\\b|\\btry_cast\\b|\\btype\\b|\\bunbounded\\b|\\buncommitted\\b"
                    + "|\\bunion\\b|\\bunrecognized\\b|\\bupdate\\b|\\buser\\b|\\busing\\b|\\bvalues\\b"
                    + "|\\bview\\b|\\bwhen\\b|\\bwhere\\b|\\bwindow\\b|\\bwith\\b|\\bwithout\\b"
                    + "|\\bwork\\b|\\bwrite\\b|\\bws\\b|\\byear\\b|\\bzone\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
}
