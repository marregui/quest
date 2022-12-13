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

package io.quest.frontend.meta;

import io.quest.frontend.editor.QuestHighlighter;

import javax.swing.*;
import javax.swing.text.*;
import java.util.regex.Pattern;

public class MetaHighlighter extends QuestHighlighter {

    // https://docs.oracle.com/javase/tutorial/essential/regex/bounds.html
    static final Pattern KEY_PATTERN = Pattern.compile(
        "\\bcolumn\\b|\\bname\\b|\\btype\\b|\\bindexed\\b|\\bindexBlockCapacity\\b|"
            + "\\bpartition\\b|\\btxn\\b|\\bversion\\b|\\bsymbol\\b|\\bcount\\b|\\bindexed\\n|\\bwith\\b|"
            + "\\bisRO\\b|\\bmask\\b|\\bav0\\b|\\bav1\\b|\\bav2\\b|\\bz\\b|\\bsymbolCount\\b|\\bcapacity\\b|"
            + "\\bmicro\\b|\\bmicros\\b|\\bsec\\b|\\bsecs\\b|\\bmin\\b|\\bindex\\b|\\boffset\\b|"
            + "\\btab\\b|\\btableId\\b|\\bstructureVersion\\b|\\btimestampIndex\\b|\\bpartitionBy\\b|"
            + "\\bmaxUncommittedRows\\b|\\bO3MaxLag\\b|\\bcolumnCount\\b|\\bentryCount\\b|"
            + "\\bpartitionTimestamp\\b|\\bcolumnIndex\\b|\\bcolumnNameTxn\\b|\\bcolumnTop\\b|"
            + "\\bcolumnVersion\\b|\\bdataVersion\\b|\\btruncateVersion\\b|\\bpartitionTableVersion\\b|"
            + "\\browCount\\b|\\bfixedRowCount\\b|\\btransientRowCount\\b|\\bminTimestamp\\b|"
            + "\\bmaxTimestamp\\b|\\brecordSize\\b|\\bsymbolColumnCount\\b|\\bpartitionCount\\b|"
            + "\\bsymbolCapacity\\b|\\bisCached\\b|\\bisDeleted\\b|\\bcontainsNullValue\\b|\\brecord\\b|"
            + "\\bcv\\b|\\bor\\b|\\bentry\\b|\\bsize\\b|\\btotal\\b|"
            + "-|\\+|:|>|<|=|\\.|,|;|\\(|\\)|\\[|\\]"
            + "",
        PATTERN_FLAGS);

    MetaHighlighter(StyledDocument styledDocument) {
        super(styledDocument);
    }

    public static MetaHighlighter of(JTextPane textPane) {
        MetaHighlighter highlighter = new MetaHighlighter(textPane.getStyledDocument());
        AbstractDocument doc = (AbstractDocument) textPane.getDocument();
        doc.putProperty(DefaultEditorKit.EndOfLineStringProperty, "\n");
        doc.setDocumentFilter(highlighter);
        return highlighter;
    }

    @Override
    public void handleTextChanged(String txt) {
        applyStyle(KEY_PATTERN.matcher(txt), HIGHLIGHT_KEYWORD, false);
    }
}
