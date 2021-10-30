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

package io.mygupsql.backend;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

import io.mygupsql.frontend.commands.Content;


/**
 * Exemplifies the use of {@link Store} and tests that {@link ConnAttrs}
 * instances are loaded/saved from/to the backing file correctly.
 */
public class StoreTest {

    @Test
    public void test_persist_load_DBConnection() {
        // File contents:
        // [
        // {
        // "name": "master-node-0",
        // "attrs": {
        // "host": "prometheus",
        // "password": "secret password",
        // "port": "5433",
        // "username": "patroclo"
        // }
        // }
        // ]

        String fileName = deleteIfExists("db-connection-persistence-test.json");

        ConnAttrs conn = new ConnAttrs("master-node-0");
        conn.setAttr("host", "prometheus");
        conn.setAttr("port", "5433");
        conn.setAttr("username", "patroclo");
        conn.setAttr("password", "secret password");

        // Save connection
        try (Store<ConnAttrs> store = new Store<>(fileName, ConnAttrs.class)) {
            store.addEntry(conn, true);
        }

        // Load connection
        ConnAttrs pConn;
        try (Store<ConnAttrs> store = new Store<>(fileName, ConnAttrs.class)) {
            store.loadEntriesFromFile();
            assertThat(store.size(), is(1));
            pConn = store.entries().get(0);
        }

        assertThat(pConn.getName(), is("master-node-0"));
        assertThat(pConn.getHost(), is("prometheus"));
        assertThat(pConn.getPort(), is("5433"));
        assertThat(pConn.getUsername(), is("patroclo"));
        assertThat(pConn.getPassword(), is("secret password"));
        assertThat(pConn.getUri(), is("jdbc:postgresql://prometheus:5433/"));
        assertThat(pConn.getKey(), is("master-node-0 patroclo@prometheus:5433"));
        assertThat(conn, Matchers.is(pConn));
    }

    @Test
    public void test_persist_load_Content() {
        // File content:
        // [
        // {
        // "name": "CommandBoard",
        // "attrs": {
        // "content": "Audentes fortuna iuvat"
        // }
        // }
        // ]

        String fileName = deleteIfExists("command-board-content-persistence-test.json");

        Content content = new Content();
        content.setContent("Audentes fortuna  iuvat");

        // Save content
        try (Store<Content> store = new Store<>(fileName, Content.class)) {
            store.addEntry(content, true);
        }

        // Load content
        Content pContent;
        try (Store<Content> store = new Store<>(fileName, Content.class)) {
            store.loadEntriesFromFile();
            assertThat(store.size(), is(1));
            pContent = store.entries().get(0);
        }

        assertThat(pContent.getName(), is("default"));
        assertThat(pContent.getContent(), is("Audentes fortuna  iuvat"));
        assertThat(pContent.getKey(), is(pContent.getName()));
        assertThat(content, is(pContent));
    }

    @Test
    public void test_iterator() {
        String fileName = deleteIfExists("store-iterator-test.json");
        try (Store<StoreEntry> store = new Store<>(fileName, StoreEntry.class)) {
            for (int i = 0; i < 10; i++) {
                StoreEntry entry = new StoreEntry("entry_" + i);
                entry.setAttr("id", String.valueOf(i));
                entry.setAttr("age", "14_000");
                store.addEntry(entry, i == 0);
            }
        }
        List<StoreEntry> entries;
        try (Store<StoreEntry> store = new Store<>(fileName, StoreEntry.class)) {
            store.loadEntriesFromFile();
            assertThat(store.size(), is(10));
            entries = store.entries();
            int i = 0;
            for (StoreEntry entry : store) {
                assertThat(entry.getName(), is("entry_" + i));
                assertThat(entry.getAttr("id"), is(String.valueOf(i)));
                assertThat(entry.getAttr("age"), is("14_000"));
                i++;
            }
        }
        assertThat(entries, is(Collections.<StoreEntry>emptyList()));
    }

    private static String deleteIfExists(String fileName) {
        Objects.requireNonNull(fileName);
        File rootPath = Store.getDefaultRootPath();
        File file = new File(rootPath, fileName);
        if (file.exists()) {
            file.delete();
        }
        assertThat(file.exists(), is(false));
        return fileName;
    }
}
