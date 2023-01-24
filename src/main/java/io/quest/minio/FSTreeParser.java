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


import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.StringReader;
import java.net.URL;
import java.util.Stack;

public class FSTreeParser extends DefaultHandler {
    public static final String NAME_ATTR = "name";
    public static final String PATH_ATTR = "path";
    private static final String SIZE_ATTR = "size";
    private static final String LAST_MODIFIED_ATTR = "lastModified";
    private static final String IS_FOLDER_ATTR = "isFolder";
    private static final String ENCODING = "UTF-8";
    private static final String ENTRY_TAG = "entry";
    private static final String CONTENTS_TAG = "contents";
    private final XMLReader parser;
    private final Stack<FSEntry> entryStack;
    private final Stack<String> pathStack;

    private FSTreeParser() throws SAXException, ParserConfigurationException {
        SAXParserFactory parserFactory = SAXParserFactory.newInstance();
        SAXParser saxParser = parserFactory.newSAXParser();
        parser = saxParser.getXMLReader();
        parser.setContentHandler(this);
        parser.setErrorHandler(this);
        parser.setDTDHandler(this);
        parser.setEntityResolver(this);
        parser.setFeature("http://xml.org/sax/features/validation", true);
        parser.setFeature("http://xml.org/sax/features/namespaces", true);
        parser.setProperty("http://apache.org/xml/properties/input-buffer-size", IOUtils.BUFFER_SIZE);
        entryStack = new Stack<>();
        pathStack = new Stack<>();
    }

    public static FSEntry parse(URL url) throws Exception {
        return parse(IOUtils.fetchFileContents(url.getPath()));
    }

    public static FSEntry parse(String fileContents) throws Exception {
        InputSource source = new InputSource(new StringReader(fileContents));
        source.setEncoding(ENCODING);
        return new FSTreeParser().parse(source);
    }

    private FSEntry parse(InputSource source) throws Exception {
        parser.parse(source);
        return entryStack.isEmpty() ? null : entryStack.pop();
    }

    @Override
    public void startElement(String uri, String localName, String rawName, Attributes attr) {
        if (ENTRY_TAG.equals(rawName)) {
            String name = attr.getValue(NAME_ATTR);
            String path = attr.getValue(PATH_ATTR);
            long size = Long.parseLong(attr.getValue(SIZE_ATTR));
            long lastModified = Long.parseLong(attr.getValue(LAST_MODIFIED_ATTR));
            boolean isFolder = Boolean.parseBoolean(attr.getValue(IS_FOLDER_ATTR));
            if (isFolder) {
                if (!pathStack.isEmpty()) {
                    path = String.format("%s%s%s", pathStack.peek(), File.separator, name);
                }
                pathStack.push(path);
            } else {
                path = pathStack.peek();
            }
            FSEntry entry = FSEntry.entry(name, path, size, lastModified, isFolder);
            if (!entryStack.isEmpty()) {
                entryStack.peek().addFSEntry(entry);
            }
            if (isFolder) {
                entryStack.push(entry);
            }
        }
    }

    @Override
    public void characters(char[] chars, int start, int length) {
        // No - op
    }

    @Override
    public void endElement(String uri, String localName, String rawName) {
        if (CONTENTS_TAG.equals(rawName)) {
            if (entryStack.size() > 1) {
                entryStack.pop();
                pathStack.pop();
            }
        }
    }
}