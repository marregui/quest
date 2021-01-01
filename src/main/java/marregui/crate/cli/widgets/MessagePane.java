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

package marregui.crate.cli.widgets;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;

import marregui.crate.cli.GUITk;


public class MessagePane extends JPanel {

    private static final long serialVersionUID = 1L;
    private static final String ERROR_HEADER = "==========  E R R O R  ==========\n";
    private static final Font FONT = new Font(GUITk.MAIN_FONT_NAME, Font.BOLD, 14);
    private static final Color CARET_COLOR = Color.GREEN;
    private static final Color BACKGROUND_COLOR = Color.BLACK;

    private final static Object FOREGROUND = StyleConstants.Foreground;
    private final static StyleContext STYLE = StyleContext.getDefaultStyleContext();
    private final static AttributeSet NORMAL = STYLE.addAttribute(STYLE.getEmptySet(), FOREGROUND, Color.WHITE);
    private final static AttributeSet ERROR = STYLE.addAttribute(STYLE.getEmptySet(), FOREGROUND, new Color(189, 4, 4));
    private final static AttributeSet KEYWORD = STYLE.addAttribute(STYLE.getEmptySet(), FOREGROUND, GUITk.APP_THEME_COLOR);

    protected final JTextPane textPane;

    public MessagePane() {
        textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setCaretColor(CARET_COLOR);
        textPane.setCaretPosition(0);
        textPane.setMargin(new Insets(5, 5, 5, 5));
        textPane.setFont(FONT);
        textPane.setBackground(BACKGROUND_COLOR);
        AbstractDocument doc = (AbstractDocument) textPane.getDocument();
        doc.setDocumentFilter(new HighlightKeywordsFilter(textPane.getStyledDocument()));
        JScrollPane scrollPane = new JScrollPane(textPane, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
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
        textPane.setText(extractStackTrace(error));
        repaint();
    }

    private static String extractStackTrace(Throwable error) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(ERROR_HEADER).append("\n");
        try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
            error.printStackTrace(pw);
            sb.append(sw.toString());
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
        return sb.toString();
    }

    private static class HighlightKeywordsFilter extends DocumentFilter {

        private final StyledDocument styledDocument;

        private HighlightKeywordsFilter(StyledDocument styledDocument) {
            this.styledDocument = Objects.requireNonNull(styledDocument);
        }

        @Override
        public void insertString(FilterBypass fb, int offset, String text, AttributeSet attributeSet)
            throws BadLocationException {
            super.insertString(fb, offset, text, attributeSet);
            handleTextChanged();
        }

        @Override
        public void remove(FilterBypass fb, int offset, int length) throws BadLocationException {
            super.remove(fb, offset, length);
            handleTextChanged();
        }

        @Override
        public void replace(FilterBypass fb, int offset, int length, String text, AttributeSet attrSet)
            throws BadLocationException {
            super.replace(fb, offset, length, text, attrSet);
            handleTextChanged();
        }

        private void handleTextChanged() throws BadLocationException {
            int len = styledDocument.getLength();
            String txt = styledDocument.getText(0, len);
            if (ERROR_HEADER_PATTERN.matcher(txt).find()) {
                styledDocument.setCharacterAttributes(0, len, ERROR, true);
            }
            else {
                styledDocument.setCharacterAttributes(0, len, NORMAL, true);

                Matcher matcher = KEYWORDS_PATTERN.matcher(txt);
                while (matcher.find()) {
                    int start = matcher.start();
                    len = matcher.end() - start;
                    styledDocument.setCharacterAttributes(start, len, KEYWORD, false);
                }
            }
        }

        private static final Pattern ERROR_HEADER_PATTERN = Pattern.compile(ERROR_HEADER);

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
                + "|\\blong\\b|\\bmatch\\b|\\bmaterialized\\b|\\bminute\\b|\\bmonth\\b"
                + "|\\bmove\\b|\\bnatural\\b|\\bnot\\b|\\bnothing\\b|\\bnull\\b"
                + "|\\bnulls\\b|\\bobject\\b|\\boff\\b|\\boffset\\b|\\bon\\b"
                + "|\\bonly\\b|\\bopen\\b|\\boptimize\\b|\\bor\\b|\\border\\b"
                + "|\\bouter\\b|\\bover\\b|\\bpartition\\b|\\bpartitioned\\b|\\bpartitions\\b"
                + "|\\bpersistent\\b|\\bplain\\b|\\bplans\\b|\\bpreceding\\b|\\bprecision\\b"
                + "|\\bprepare\\b|\\bprivileges\\b|\\bpromote\\b|\\brange\\b|\\bread\\b"
                + "|\\brecursive\\b|\\brefresh\\b|\\brename\\b|\\brepeatable\\b"
                + "|\\breplace\\b|\\breplica\\b|\\brepository\\b|\\breroute\\b"
                + "|\\breset\\b|\\brestore\\b|\\bretry\\b|\\breturn\\b|\\breturning\\b"
                + "|\\breturns\\b|\\brevoke\\b|\\bright\\b|\\brow\\b|\\brows\\b"
                + "|\\bschema\\b|\\bschemas\\b|\\bsecond\\b|\\bselect\\b|\\bsequences\\b"
                + "|\\bserializable\\b|\\bsession\\b|\\bset\\b|\\bshard\\b|\\bshards\\b"
                + "|\\bshort\\b|\\bshow\\b|\\bsnapshot\\b|\\bsome\\b|\\bstorage\\b"
                + "|\\bstratify\\b|\\bstrict\\b|\\bstring\\b|\\bsubstring\\b|\\bsummary\\b"
                + "|\\bswap\\b|\\bsystem\\b|\\btable\\b|\\btables\\b|\\btablesample\\b"
                + "|\\btemp\\b|\\btemporary\\b|\\btext\\b|\\bthen\\b|\\btime\\b"
                + "|\\btimestamp\\b|\\bto\\b|\\btokenizer\\b|\\btrailing\\b|\\btransaction\\b"
                + "|\\btransient\\b|\\btrim\\b|\\btrue\\b|\\btype\\b|\\bunbounded\\b"
                + "|\\buncommitted\\b|\\bunion\\b|\\bupdate\\b|\\buser\\b|\\busing\\b"
                + "|\\bvalues\\b|\\bvarying\\b|\\bview\\b|\\bwhen\\b|\\bwhere\\b"
                + "|\\bwindow\\b|\\bwith\\b|\\bwithout\\b|\\bwork\\b|\\bwrite\\b|\\byear\\b|\\bzone\\b",
            Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }
}
