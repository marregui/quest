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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import java.io.*;
import java.util.zip.GZIPOutputStream;

public class IOUtils {
    public static final int BUFFER_SIZE = 1024 * 8;
    public static final String CHARSET = "UTF-8";


    public static FSEntry list(String rootFolderPath) throws IOException {
        File folder = new File(rootFolderPath);
        return list(folder, FSEntry.rootFolder(rootFolderPath, folder.lastModified()), rootFolderPath);
    }

    private static FSEntry list(File folder, FSEntry parentFolder, String rootFolderPath) throws IOException {
        if (false == checkFolderAccess(folder)) {
            throw new IOException(String.format("Cannot access: %s", folder.getAbsolutePath()));
        }
        for (File f : folder.listFiles()) {
            if (checkFolderAccess(f)) {
                FSEntry folderEntry = FSEntry.folder(f.getName(), f.lastModified());
                parentFolder.addFSEntry(folderEntry);
                list(f, folderEntry, rootFolderPath);
            } else if (checkFileAccess(f)) {
                String path = folder.getAbsolutePath().substring(rootFolderPath.length() + 1);
                parentFolder.addFSEntry(FSEntry.file(f.getName(), path, f.length(), f.lastModified()));
            } else {
                throw new IOException(String.format("Cannot access: %s", f.getAbsolutePath()));
            }
        }
        return parentFolder;
    }

    public static boolean checkFolderAccess(File folder) {
        return folder.exists() && folder.isDirectory() && folder.canExecute() && folder.canRead();
    }

    public static boolean checkFileAccess(File file) {
        return file.exists() && file.isFile() && file.canRead();
    }

    public static String exceptionAsString(Exception e) {
        String content;
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            PrintStream ps = new PrintStream(buffer);
            e.printStackTrace(ps);
            ps.close();
            content = buffer.toString();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        }
        return content;
    }

    public static void tgz(String prefix, String folderName, OutputStream out) throws IOException {
        String tgzFolderPath = String.format("%s%s%s", prefix, File.separator, folderName);
        File tgzFolder = new File(tgzFolderPath);
        if (false == IOUtils.checkFolderAccess(tgzFolder)) {
            throw new IOException(String.format("Cannot access folder: %s", tgzFolderPath));
        }
        try (TarArchiveOutputStream tgzos = new TarArchiveOutputStream(new GZIPOutputStream(new BufferedOutputStream(out)))) {
            tgzos.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_STAR);
            tgzos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);
            tgzos.putArchiveEntry(new TarArchiveEntry(tgzFolder, folderName));
            tgzos.closeArchiveEntry();
            tgz(prefix, tgzFolder, tgzos);
            tgzos.flush();
        }
    }

    private static void tgz(String preffix, File folder, TarArchiveOutputStream tgzos) throws IOException {
        if (false == checkFolderAccess(folder)) {
            throw new IOException(String.format("Cannot access: %s", folder.getAbsolutePath()));
        }
        for (File f : folder.listFiles()) {
            String path = f.getAbsolutePath();
            path = path.substring(path.indexOf(preffix) + preffix.length() + 1);
            tgzos.putArchiveEntry(new TarArchiveEntry(f, path));
            if (checkFolderAccess(f)) {
                tgzos.closeArchiveEntry();
                tgz(preffix, f, tgzos);
            } else if (checkFileAccess(f)) {
                final int bufferSize = 1024 * 4;
                byte[] buffer = new byte[bufferSize];
                int bread;
                try (FileInputStream in = new FileInputStream(f)) {
                    while (-1 != (bread = in.read(buffer, 0, bufferSize))) {
                        tgzos.write(buffer, 0, bread);
                    }
                }
                tgzos.flush();
                tgzos.closeArchiveEntry();
            } else {
                throw new IOException(String.format("Cannot access: %s", f.getAbsolutePath()));
            }
        }
    }

    public static long readFromTo(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];
        long transferredBytes = 0;
        int bread = 0;
        while (-1 != (bread = in.read(buffer, 0, BUFFER_SIZE))) {
            out.write(buffer, 0, bread);
            out.flush();
            transferredBytes += bread;
        }
        return transferredBytes;
    }

    public static String humanReadableSize(long size) {
        final int unit = 1024;
        if (size < unit) {
            return String.format("%d bytes", size);
        }
        int exp = (int) (Math.log(size) / Math.log(unit));
        return String.format(
                "%d bytes (%.1f %cB)",
                size,
                size / Math.pow(unit, exp),
                "KMGTPE".charAt(exp - 1));
    }

    public static String fetchFileContents(String fileName) throws IOException {
        File file = new File(fileName);
        int fileSize = (int) file.length(); // Cannot exceed, 4Gb which is fine
        byte[] fileBytes = new byte[fileSize];
        try (FileInputStream fis = new FileInputStream(file)) {
            int readBytes = fis.read(fileBytes, 0, fileSize);
            if (readBytes != fileSize) {
                throw new IOException("Failed to read the content of the file");
            }
        }
        return new String(fileBytes, CHARSET);
    }
}