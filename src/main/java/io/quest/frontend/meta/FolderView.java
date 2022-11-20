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

package io.quest.frontend.meta;

import io.questdb.cairo.TableUtils;
import io.questdb.std.Files;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

class FolderView extends JPanel {

    enum FileType {
        META(true), TXN(true), CV(true), SB, D, C, O, K, V, UNKNOWN;

        private final boolean defaultChecked;

        FileType() {
            this(false);
        }

        FileType(boolean checked) {
            defaultChecked = checked;
        }

        public boolean isDefaultChecked() {
            return defaultChecked;
        }


        public static FileType of(String fileName) {
            if (fileName == null || fileName.isBlank()) {
                return FileType.UNKNOWN;
            }

            if (fileName.contains(TableUtils.META_FILE_NAME)) {
                return FileType.META;
            }

            if (fileName.contains(TableUtils.TXN_SCOREBOARD_FILE_NAME)) {
                return FileType.SB;
            }

            if (fileName.contains(TableUtils.TXN_FILE_NAME)) {
                return FileType.TXN;
            }

            if (fileName.contains(TableUtils.COLUMN_VERSION_FILE_NAME)) {
                return FileType.CV;
            }

            if (fileName.contains(".k")) {
                return FileType.K;
            }

            if (fileName.contains(".o")) {
                return FileType.O;
            }

            if (fileName.contains(".c")) {
                return FileType.C;
            }

            if (fileName.contains(".v")) {
                return FileType.V;
            }

            if (fileName.contains(".d")) {
                return FileType.D;
            }

            return FileType.UNKNOWN;
        }
    }

    private final JTree treeView;
    private final JFileChooser chooser;
    private final Consumer<File> onRootChange;
    private final Set<FileType> visibleFileTypes = new HashSet<>();
    private File root; // setRoot changes it

    FolderView(Consumer<File> onRootChange, Consumer<TreePath> onSelection) {
        super(new BorderLayout());
        setBorder(BorderFactory.createRaisedBevelBorder());

        this.onRootChange = onRootChange;

        treeView = new JTree(new DefaultMutableTreeNode("UNDEFINED"));
        treeView.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        treeView.setExpandsSelectedPaths(true);
        treeView.addTreeSelectionListener(e -> onSelection.accept(e.getPath()));
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.getViewport().add(treeView);

        chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("."));
        chooser.setDialogTitle("Select a folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setMultiSelectionEnabled(false);

        JPanel checkBoxPane = new JPanel(new GridLayout(2, 5, 0, 0));
        for (FileType type : FileType.values()) {
            if (type != FileType.UNKNOWN) {
                checkBoxPane.add(createVisibleFileTypeCheckBox(type));
            }
        }

        // button panel to be added to the south
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 0, 0));
        buttonPanel.add(createButton("Set", () -> {
            String root = JOptionPane.showInputDialog(null, "Root:", null, JOptionPane.QUESTION_MESSAGE);
            if (root != null) {
                File folder = new File(root);
                if (folder.exists() && folder.isDirectory()) {
                    setRoot(folder.getAbsoluteFile());
                }
            }
        }));
        buttonPanel.add(createButton("Select", () -> {
            if (FolderView.this.chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                setRoot(chooser.getSelectedFile());
            }
        }));
        buttonPanel.add(createButton("Reload", this::reloadModel));

        // compose panels
        add(checkBoxPane, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    void setRoot(File root) {
        this.root = root;
        reloadModel();
        onRootChange.accept(root);
    }

    private void reloadModel() {
        if (root != null) {
            treeView.setModel(createModel(root));
        }
    }

    static String formatItemName(String itemName, long size) {
        if (size < 1024L) {
            return size + " B";
        }
        int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
        return String.format("%s (%.1f %sB)",
                itemName,
                (double) size / (1L << (z * 10)),
                " KMGTPE".charAt(z)
        );
    }

    static String extractItemName(String withSize) {
        if (withSize.endsWith("B)")) {
            return withSize.substring(0, withSize.indexOf(" ("));
        }
        return withSize;
    }

    private DefaultTreeModel createModel(File folder) {
        return new DefaultTreeModel(addNodes(null, folder));
    }

    private DefaultMutableTreeNode addNodes(DefaultMutableTreeNode currentRoot, File folder) {
        String folderPath = folder.getPath();
        DefaultMutableTreeNode folderNode = new DefaultMutableTreeNode(folder.getName() + Files.SEPARATOR);
        if (currentRoot != null) { // should only be null at root
            currentRoot.add(folderNode);
        }
        String[] folderContent = folder.list();
        if (folderContent != null && folderContent.length > 0) {
            Arrays.sort(folderContent);
            for (String itemName : folderContent) {
                File newPath = new File(folderPath, itemName);
                if (newPath.isDirectory()) {
                    addNodes(folderNode, newPath);
                } else if (newPath.length() > 0) {
                    if (visibleFileTypes.contains(FileType.of(itemName))) {
                        folderNode.add(new DefaultMutableTreeNode(formatItemName(itemName, newPath.length())));
                    }
                }
            }
        }
        return folderNode;
    }

    private JCheckBox createVisibleFileTypeCheckBox(FileType type) {
        JCheckBox checkBox = new JCheckBox(type.name(), type.isDefaultChecked());
        checkBox.addActionListener(e -> {
            if (checkBox.isSelected()) {
                visibleFileTypes.add(type);
            } else {
                visibleFileTypes.remove(type);
            }
            reloadModel();
        });
        if (type.isDefaultChecked()) {
            visibleFileTypes.add(type);
        }
        return checkBox;
    }

    private static JButton createButton(String title, Runnable runnable) {
        JButton button = new JButton(title);
        button.addActionListener(e -> runnable.run());
        return button;
    }
}
