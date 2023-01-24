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
 * Copyright (c) 2019 - 2023, Miguel Arregui a.k.a. marregui
 */

package io.quest.minio;

public class FSTreePersister {
    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
    private static final int INDENTATION = 4;

    public static String persist(FSEntry root) {
        StringBuilder sb = new StringBuilder();
        sb.append(XML_HEADER).append("\n");
        persist(root, sb, 0);
        return sb.toString();
    }

    private static void persist(FSEntry entry, StringBuilder sb, int indentLevel0) {
        sb.append(makeEntryTag(entry, indentLevel0)).append("\n");
        int indentLevel1 = indentLevel0 + INDENTATION;
        int indentLevel2 = indentLevel0 + (2 * INDENTATION);
        if (entry.isFolder) {
            sb.append(indent("<contents>", indentLevel1)).append("\n");
            for (FSEntry e : entry.content()) {
                persist(e, sb, indentLevel2);
            }
            sb.append(indent("</contents>", indentLevel1)).append("\n");
            sb.append(indent("</entry>", indentLevel0)).append("\n");
        }
    }

    private static String indent(String txt, int indentLevel) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < indentLevel; i++) {
            sb.append(" ");
        }
        return sb.append(txt).toString();
    }

    private static String makeEntryTag(FSEntry entry, int indentLevel) {
        return indent(String.format(
                        "<entry name=\"%s\" path=\"%s\" isFolder=\"%b\" size=\"%d\" lastModified=\"%d\"%s>",
                        entry.name,
                        entry.isRoot() ? entry.path : ".",
                        entry.isFolder,
                        entry.size,
                        entry.lastModified,
                        entry.isFolder ? "" : "/"),
                indentLevel);
    }
}