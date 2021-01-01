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

package marregui.crate.cli.persistence;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.junit.jupiter.api.Test;

import marregui.crate.cli.backend.DBConnAttrs;
import marregui.crate.cli.widgets.command.Content;


/**
 * Exemplifies the use of {@link Store} and tests that {@link DBConnAttrs}
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

        DBConnAttrs conn = new DBConnAttrs("master-node-0");
        conn.setAttribute("host", "prometheus");
        conn.setAttribute("port", "5433");
        conn.setAttribute("username", "patroclo");
        conn.setAttribute("password", "secret password");

        // Save connection
        try (Store<DBConnAttrs> store = new Store<>(fileName, DBConnAttrs.class)) {
            store.addEntry(conn, true);
        }

        // Load connection
        DBConnAttrs pConn;
        try (Store<DBConnAttrs> store = new Store<>(fileName, DBConnAttrs.class)) {
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
        assertThat(conn, is(pConn));
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

        assertThat(pContent.getName(), is("CommandBoard"));
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
                entry.setAttribute("id", String.valueOf(i));
                entry.setAttribute("age", "14_000");
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
                assertThat(entry.getAttribute("id"), is(String.valueOf(i)));
                assertThat(entry.getAttribute("age"), is("14_000"));
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
