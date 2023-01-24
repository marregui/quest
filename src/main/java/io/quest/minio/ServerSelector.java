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

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class ServerSelector extends JPanel {
    private static final int COMBO_WIDTH = 300;
    private static final int SMALL_BUTTON_WIDTH = 45;
    private static final int NORMAL_BUTTON_WIDTH = 85;
    private static final int COMPONENT_HEIGHT = 18;
    private static final String SERVER_URLS_FILE_NAME = ".servers";
    private JComboBox serverNames;
    private DefaultComboBoxModel serverNamesModel;
    private Callback callback;

    public ServerSelector(String defaultServerURL, Callback callback) {
        this.callback = callback;
        this.serverNamesModel = new DefaultComboBoxModel(getServerURLs(defaultServerURL));
        this.serverNames = new JComboBox(this.serverNamesModel);
        this.serverNames.setEditable(false);

        // Add button
        Dimension smallButtonSize = new Dimension(SMALL_BUTTON_WIDTH, COMPONENT_HEIGHT);
        Dimension normalButtonSize = new Dimension(NORMAL_BUTTON_WIDTH, COMPONENT_HEIGHT);
        JButton addButton = new JButton("+");
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                addButtonAction();
            }
        });
        addButton.setPreferredSize(smallButtonSize);
        addButton.setSize(smallButtonSize);

        // Remove button
        final JButton removeButton = new JButton("-");
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removeButtonAction();
            }
        });
        removeButton.setPreferredSize(smallButtonSize);
        removeButton.setSize(smallButtonSize);

        // Refresh button
        final JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                refreshButtonAction();
            }
        });
        refreshButton.setPreferredSize(normalButtonSize);
        refreshButton.setSize(normalButtonSize);

        // Server names
        Dimension serverNamesSize = new Dimension(COMBO_WIDTH, COMPONENT_HEIGHT);
        this.serverNames.setSize(serverNamesSize);
        this.serverNames.setPreferredSize(serverNamesSize);
        this.serverNamesModel.setSelectedItem(defaultServerURL);

        // Stitch it all up
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 10));
        setBorder(BorderFactory.createTitledBorder("Datachest server"));
        add(Box.createHorizontalStrut(10));
        add(this.serverNames);
        add(Box.createHorizontalStrut(5));
        add(addButton);
        add(removeButton);
        add(Box.createHorizontalStrut(20));
        add(refreshButton);

        // Persist the server list
        saveServerURLs();
    }

    private static boolean contains(DefaultComboBoxModel model, String target) {
        boolean alreadyIn = false;
        for (int i = 0; i < model.getSize(); i++) {
            if (((String) model.getElementAt(i)).equals(target)) {
                alreadyIn = true;
                break;
            }
        }
        return alreadyIn;
    }

    private static final Object[] getServerURLs(String defaultServerURL) {
        List<String> serverURLs = new ArrayList<>();
        serverURLs.add(defaultServerURL);
        File backingFile = new File(SERVER_URLS_FILE_NAME);
        if (backingFile.exists() && backingFile.canRead()) {
            BufferedReader br = null;
            try {
                br = new BufferedReader(new FileReader(backingFile));
                for (String serverURL; null != (serverURL = br.readLine()); ) {
                    serverURL = serverURL.trim();
                    if (false == serverURL.isEmpty() && false == serverURLs.contains(serverURL)) {
                        serverURLs.add(serverURL);
                    }
                }
            } catch (Throwable t) {
                System.err.printf("Nuisance: %s", t.getMessage());
            } finally {
                if (null != br) {
                    try {
                        br.close();
                    } catch (Throwable t) { /* no-op */ }
                }
            }
        }
        return serverURLs.toArray();
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 200);
        frame.getContentPane().add(new ServerSelector("URL", new Callback() {
            @Override
            public void serverSelected(String serverURL) {
                System.out.println("Selected: " + serverURL);
            }
        }));
        frame.setVisible(true);
    }

    private void addButtonAction() {
        String newServerName = JOptionPane.showInputDialog("Server URL");
        if (null != newServerName && false == newServerName.trim().isEmpty()) {
            if (false == contains(this.serverNamesModel, newServerName)) {
                this.serverNamesModel.addElement(newServerName);
                this.serverNamesModel.setSelectedItem(newServerName);
                saveServerURLs();
            }
        }
    }

    protected void removeItem(Object item) {
        if (null != item) {
            this.serverNamesModel.removeElement(item);
            saveServerURLs();
        } else {
            JOptionPane.showMessageDialog(
                    ServerSelector.this,
                    "Select an item first",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeButtonAction() {
        if (this.serverNamesModel.getSize() > 1) {
            removeItem(this.serverNamesModel.getSelectedItem());
        } else {
            JOptionPane.showMessageDialog(
                    ServerSelector.this,
                    "Cannot remove the last remaining item",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void refreshButtonAction() {
        Object serverURL = this.serverNamesModel.getSelectedItem();
        if (null != serverURL) {
            if (null != ServerSelector.this.callback) {
                ServerSelector.this.callback.serverSelected((String) serverURL);
            }
        } else {
            JOptionPane.showMessageDialog(
                    ServerSelector.this,
                    "Select an item first",
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void saveServerURLs() {
        File backingFile = new File(SERVER_URLS_FILE_NAME);
        if (backingFile.exists()) {
            backingFile.delete();
        }
        PrintWriter pw = null;
        try {
            pw = new PrintWriter(backingFile);
            for (int i = 0; i < this.serverNamesModel.getSize(); i++) {
                String serverName = (String) this.serverNamesModel.getElementAt(i);
                pw.println(serverName);
            }
        } catch (Throwable t) {
            System.err.printf("Nuisance: %s", t.getMessage());
        } finally {
            if (null != pw) {
                try {
                    pw.close();
                } catch (Throwable t) { /* no-op */ }
            }
        }
    }

    public interface Callback {
        /**
         * @param serverURL Selected server URL at the moment of pressing refresh
         */
        public void serverSelected(String serverURL);
    }
}