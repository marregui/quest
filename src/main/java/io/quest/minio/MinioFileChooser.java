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


import io.quest.GTk;
import io.questdb.log.Log;
import io.questdb.log.LogFactory;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;


/**
 * Dialog that allows the user to select a file or a folder from a remote
 * datachest server and download it. Files and folders are downloaded and
 * stored into a local folder which path is configured in the configuration
 * properties file by key 'datachest.downloads.folder.name'. Files and folders
 * remain in this local folder remain
 *
 * @author marregui
 */
public class MinioFileChooser extends JDialog {
    private static final Log LOG = LogFactory.getLog(MinioFileChooser.class);
    private static final long MILLIS_IN_A_DAY = 24 * 60 * 60 * 1000;
    private static final int ALLOWED_DAYS_IN_CACHE = 1;
    private static final int WIDTH = 550;
    private static final int HEIGHT = 350;

    private static final ImageIcon ROOT_ICON = GTk.Icon.ROOT_FOLDER.icon();
    private static final ImageIcon NODE_ICON = GTk.Icon.FOLDER.icon();
    private static final ImageIcon LEAF_ICON = GTk.Icon.FILE.icon();
    private static final Color SELECTED_COLOR = new Color(40, 200, 40);
    private JTree tree;
    private FSEntry selectedFSEntry;
    private JButton openButton;
    private String serverURL;

    private MinioFileChooser(String defaultServerURL, final SelectionMode selectionMode) {
        this.serverURL = defaultServerURL;
        setTitle(String.format("Select %s", selectionMode));
        setSize(WIDTH, HEIGHT);
        setModalityType(ModalityType.DOCUMENT_MODAL);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        setLocation((screen.width - WIDTH) / 2, (screen.height - HEIGHT) / 2);
        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) { /* no-op */ }

            @Override
            public void windowIconified(WindowEvent e) { /* no-op */ }

            @Override
            public void windowDeiconified(WindowEvent e) { /* no-op */ }

            @Override
            public void windowDeactivated(WindowEvent e) { /* no-op */ }

            @Override
            public void windowClosing(WindowEvent e) {
                closeAction();
            }

            @Override
            public void windowActivated(WindowEvent e) { /* no-op */ }

            @Override
            public void windowClosed(WindowEvent e) { /* no-op */ }
        });

        // Tree
        this.tree = new JTree(createNodes(defaultServerURL));
        this.tree.setBorder(BorderFactory.createTitledBorder("Data"));
        this.tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        this.tree.addTreeSelectionListener(e -> {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) e.getPath().getLastPathComponent();
            FSEntry entry = (FSEntry) node.getUserObject();
            setSelectedFSEntry(entry.isRoot() ? null : (selectionMode == SelectionMode.Folder && entry.isFolder && e.isAddedPath()) || (selectionMode == SelectionMode.File && false == entry.isFolder && e.isAddedPath()) ? entry : null);
        });
        this.tree.setCellRenderer((tree, value, isSelected, isExpanded, isLeaf, row, hasFocus) -> {
            FSEntry entry = (FSEntry) ((DefaultMutableTreeNode) value).getUserObject();
            JLabel label = new JLabel(entry.toString());
            label.setIcon(entry.isFolder ? entry.isRoot() ? ROOT_ICON : NODE_ICON : LEAF_ICON);
            label.setForeground(Color.BLACK);
            if (isSelected) {
                label.setText(String.format("<html><b><u>%s</u></b></html>", entry));
                label.setForeground(SELECTED_COLOR);
            }
            return label;
        });

        // Buttons
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> {
            setSelectedFSEntry(null);
            MinioFileChooser.this.dispose();
        });
        this.openButton = new JButton("Open");
        this.openButton.addActionListener(e -> MinioFileChooser.this.dispose());
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonsPanel.add(this.openButton);
        buttonsPanel.add(cancelButton);

        // Server selector
        ServerSelector serverSelector = new ServerSelector(defaultServerURL, serverURL -> changeDataChestServerURL(serverURL));

        setLayout(new BorderLayout());
        add(serverSelector, BorderLayout.NORTH);
        add(new JScrollPane(this.tree), BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.SOUTH);
        setSelectedFSEntry(null);
    }

    /**
     * @return Shows a selection dialog for a remote file of the
     * Datachest server. The file is downloaded to the local
     * file system (overriding any previous version) and a
     * File handle is returned
     */
    public static File selectFile() {
        return select(SelectionMode.File);
    }

    /**
     * @return Shows a selection dialog for a remote folder of the
     * Datachest server. The folder is downloaded to the local
     * file system (overriding any previous version) in tar/gz
     * format with '.tgz' extension and a File handle is returned
     */
    public static File selectFolder() {
        File targz = select(SelectionMode.Folder);
        JOptionPane.showMessageDialog(null, String.format("File available at: %s", targz.getAbsolutePath()));
        return targz;
    }

    private static File select(SelectionMode selectionMode) {
        // Choose a file
        MinioFileChooser fileChooser = new MinioFileChooser(Conf.getProperty(Conf.Key.ServiceUrl), selectionMode);
        fileChooser.setVisible(true);
        FSEntry selectedEntry = fileChooser.getSelectedFSEntry();

        // Download the file to local
        File file = null;
        try {
            file = Protocol.get(fileChooser.getServerURL(), selectedEntry, manageDownloadsFolder());
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(null, String.format("Problem: %s", IOUtils.exceptionAsString(e)), "Error", JOptionPane.ERROR_MESSAGE);
        }
        return file;
    }

    private static File manageDownloadsFolder() {
        File downloadsFolder = new File(Conf.getProperty(Conf.Key.ClientDownloadsFolderName));
        if (false == downloadsFolder.exists()) {
            downloadsFolder.mkdirs();
        }

        // Delete files from the downloads folder older than one day
        long currentTimeMillis = System.currentTimeMillis();
        for (File file : downloadsFolder.listFiles()) {
            long days = Math.abs(currentTimeMillis - file.lastModified()) / MILLIS_IN_A_DAY;
            if (days >= ALLOWED_DAYS_IN_CACHE) {
                file.delete();
            }
        }
        return downloadsFolder;
    }

    private static DefaultMutableTreeNode createNodes(String serverURL) {
        FSEntry rootEntry = null;
        try {
            rootEntry = Protocol.list(serverURL);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, String.format("Cannot reach server: %s", serverURL), "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
            return new DefaultMutableTreeNode(FSEntry.folder("Server not accessible", 0), false);
        }

        return createNodes(new DefaultMutableTreeNode(rootEntry, rootEntry.isFolder), rootEntry);
    }

    private static DefaultMutableTreeNode createNodes(DefaultMutableTreeNode parent, FSEntry entry) {
        for (FSEntry child : entry.content()) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(child, child.isFolder);
            parent.add(node);
            if (child.isFolder) {
                createNodes(node, child);
            }
        }
        return parent;
    }

    public static void main(String[] args) throws Exception {
        File file = MinioFileChooser.selectFile();
        System.out.println("File: " + file);
        file = MinioFileChooser.selectFolder();
        System.out.println("File: " + file);
    }

    private String getServerURL() {
        return this.serverURL;
    }

    private void changeDataChestServerURL(String serverURL) {
        try {
            this.tree.setModel(new DefaultTreeModel(createNodes(serverURL)));
            this.serverURL = serverURL;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, String.format("Problem with the server URL: %s", e.getMessage()), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private FSEntry getSelectedFSEntry() {
        return this.selectedFSEntry;
    }

    private void setSelectedFSEntry(FSEntry entry) {
        this.selectedFSEntry = entry;
        this.openButton.setEnabled(null != entry);
        if (null == entry) {
            this.tree.getSelectionModel().setSelectionPath(null);
        }
    }

    private void closeAction() {
        setSelectedFSEntry(null);
        dispose();
    }

    private enum SelectionMode {File, Folder;}
}
