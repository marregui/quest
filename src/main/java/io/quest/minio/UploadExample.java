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

import io.minio.MinioClient;
import io.minio.PutObjectArgs;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import javax.swing.*;

public class UploadExample extends JFrame {

    private static final String defaultObjectId = "manolo";
    private static final File defaultFile = new File("/Users/marregui/QUEST/hello.txt");


    UploadExample() {
        MinioClient cli = MinioClient.builder()
                .endpoint(Conf.ENDPOINT)
                .credentials(Conf.ACCESS_KEY, Conf.SECRET_KEY)
                .build();
        JButton button = new JButton("Click to upload");
        button.addActionListener(e -> {
            button.setEnabled(false);
            new SwingWorker<>() {
                @Override
                protected Object doInBackground() throws Exception {
                    try (
                            BufferedInputStream bis = new BufferedInputStream(new FileInputStream(defaultFile));
                            ProgressMonitorInputStream pmis = new ProgressMonitorInputStream(
                                    UploadExample.this,
                                    "Uploading... " + defaultFile.getAbsolutePath(),
                                    bis)
                    ) {
                        pmis.getProgressMonitor().setMillisToPopup(10);
                        cli.putObject(PutObjectArgs.builder()
                                .bucket(Conf.BUCKET)
                                .object(defaultObjectId)
                                .stream(pmis, pmis.available(), -1)
                                .build());
                    }
                    return null;
                }

                @Override
                protected void done() {
                    button.setEnabled(true);
                    JOptionPane.showMessageDialog(UploadExample.this, "upload complete!");
                }
            }.execute();
        });
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().add(button);
        setLocationRelativeTo(null);
        pack();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new UploadExample().setVisible(true));
    }
}