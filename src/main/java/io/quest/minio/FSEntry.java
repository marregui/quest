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

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class FSEntry implements Comparable<FSEntry> {
    private static final String ROOT = "Root";
    public final String path;
    public final String name;
    public final long size;
    public final long lastModified;
    public final boolean isFolder;
    private final StringBuilder sb;
    private List<FSEntry> content;

    private FSEntry(String name, String path, long size, long lastModified, boolean isFolder) {
        this.name = name;
        this.path = path;
        this.size = size;
        this.lastModified = lastModified;
        this.isFolder = isFolder;
        if (isFolder) {
            content = new ArrayList<>();
        }
        sb = new StringBuilder();
    }

    public static FSEntry file(String name, String path, long size, long lastModified) {
        return new FSEntry(name, path, size, lastModified, false);
    }

    public static FSEntry folder(String name, long lastModified) {
        return new FSEntry(name, ".", 0, lastModified, true);
    }

    public static FSEntry rootFolder(String path, long lastModified) {
        return new FSEntry(ROOT, path, 0, lastModified, true);
    }

    public static FSEntry entry(String name, String path, long size, long lastModified, boolean isFolder) {
        return new FSEntry(name, path, size, lastModified, isFolder);
    }

    public static boolean isRoot(String name) {
        return ROOT.equals(name);
    }

    public String parentFolder() {
        sb.setLength(0);
        sb.append(path);
        sb.setLength(sb.length() - (name.length() + 1));
        return sb.toString();
    }

    public boolean isRoot() {
        return isRoot(name);
    }

    public List<FSEntry> content() {
        return Collections.unmodifiableList(content);
    }

    public void addFSEntry(FSEntry entry) {
        if (isFolder) {
            content.add(entry);
        }
    }

    public void traversePrint() {
        System.out.println(asString());
        if (isFolder) {
            for (FSEntry entry : content) {
                entry.traversePrint();
            }
        }
    }

    public String asString() {
        sb.setLength(0);
        if (isFolder) {
            sb.append(path).append(" (*)");
        } else {
            sb.append(path).append(File.separator).append(name)
                    .append(String.format(" - %s", IOUtils.humanReadableSize(size)))
                    .append(" - last modified on '").append(new Date(lastModified));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        sb.setLength(0);
        sb.append(name);
        if (isFolder) {
            if (content.isEmpty()) {
                sb.append(" (Empty)");
            }
        } else {
            sb.append(" (").append(size).append(" bytes)");
        }
        return sb.toString();
    }

    @Override
    public int hashCode() {
        return name.hashCode() + path.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (null != o && o instanceof FSEntry) {
            FSEntry that = (FSEntry) o;
            boolean equals = name.equals(that.name)
                    && path.equals(that.path)
                    && isFolder == that.isFolder
                    && lastModified == that.lastModified
                    && size == that.size;
            if (equals && isFolder) {
                equals = (content.size() == that.content.size());
                if (equals) {
                    Collections.sort(content);
                    Collections.sort(that.content);
                    for (int i = 0; i < content().size() && equals; i++) {
                        equals = content.get(i).equals(that.content.get(i));
                    }
                }
            }
            return equals;
        }
        return false;
    }

    @Override
    public int compareTo(FSEntry that) {
        int comp = name.compareTo(that.name);
        if (0 == comp) {
            comp = (int) (lastModified - that.lastModified);
        }
        return comp;
    }
}
