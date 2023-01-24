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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public abstract class Protocol {
    private static final String LIST_COMMAND_URL = "/do?list";
    private static final String GET_FILE_COMMAND_URL_TPT = "/do?get&" + FSTreeParser.PATH_ATTR + "=%s&" + FSTreeParser.NAME_ATTR + "=%s";

    public static FSEntry list(String serverUrl) throws Exception {
        String content;
        try (HttpClient client = new HttpClient(serverUrl, LIST_COMMAND_URL)) {
            try (ByteArrayOutputStream buffer = new ByteArrayOutputStream(IOUtils.BUFFER_SIZE)) {
                client.download(buffer);
                content = buffer.toString(IOUtils.CHARSET);
            }
        }
        return FSTreeParser.parse(content);
    }

    public static File get(String serverUrl, FSEntry entry, File downloadFolder) throws Exception {
        if (null == entry) {
            return null;
        }
        String path = (entry.isFolder && false == entry.isRoot()) ? entry.parentFolder() : entry.path;
        File dstFile = new File(downloadFolder, entry.isFolder ? String.format("%s.tgz", entry.name) : entry.name);
        try (
                HttpClient client = new HttpClient(serverUrl, String.format(GET_FILE_COMMAND_URL_TPT, path, entry.name));
                FileOutputStream fos = new FileOutputStream(dstFile)
        ) {
            client.download(fos);
        }
        return dstFile;
    }

    private static class HttpClient implements Closeable {
        private HttpURLConnection conn;

        private HttpClient(String serverUrlStr, String commandStr) throws IOException {
            String urlStr = String.format("%s%s", serverUrlStr, commandStr);
            conn = (HttpURLConnection) new URL(urlStr).openConnection();
            conn.setRequestProperty("Content-Type", "application/octet-stream");
            conn.setReadTimeout(Integer.MAX_VALUE);
            conn.setRequestMethod("GET");
            conn.setUseCaches(false);
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.connect();
        }

        private void download(OutputStream out) throws IOException {
            IOUtils.readFromTo(this.conn.getInputStream(), out);
        }

        @Override
        public void close() throws IOException {
            conn.disconnect();
        }
    }
}