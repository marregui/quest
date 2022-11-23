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

package io.quest.frontend.editor.meta;

import io.quest.frontend.GTk;
import io.questdb.std.Files;

import javax.swing.*;
import javax.swing.tree.*;
import java.awt.*;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

class FolderView extends JPanel {
    private static final Set<String> IGNORE_FOLDERS = new HashSet<>(Arrays.asList("questdb", "conf", "public"));

    private final JTree treeView;
    private final JFileChooser chooser;
    private final Consumer<File> onRootChange;
    private final Set<FileType> visibleFileTypes = new HashSet<>();
    private File root; // setRoot changes it

    FolderView(Consumer<File> onRootChange, Consumer<TreePath> onSelection) {
        super(new BorderLayout());
        this.onRootChange = onRootChange;

        treeView = new JTree(new DefaultMutableTreeNode("UNDEFINED"));
        treeView.setFont(GTk.MENU_FONT);
        treeView.setBackground(Color.BLACK);
        treeView.setBorder(BorderFactory.createEmptyBorder());
        treeView.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        final ImageIcon fileIcon = GTk.Icon.META_FILE.icon();
        final ImageIcon folderIcon = GTk.Icon.META_FOLDER.icon();
        treeView.setCellRenderer(new DefaultTreeCellRenderer() {
            @Override
            public Component getTreeCellRendererComponent(
                    JTree tree,
                    Object value,
                    boolean selected,
                    boolean expanded,
                    boolean leaf,
                    int row,
                    boolean hasFocus
            ) {
                super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
                setOpaque(true);
                setFont(GTk.TABLE_CELL_FONT);
                setBackground(Color.BLACK);
                setForeground(GTk.TERMINAL_COLOR);
                setText(value.toString());
                setIcon(leaf ? fileIcon : folderIcon);
                return this;
            }
        });
        treeView.setExpandsSelectedPaths(true);
        treeView.addTreeSelectionListener(e -> onSelection.accept(e.getPath()));
        JScrollPane scrollPane = new JScrollPane();
        scrollPane.getViewport().add(treeView);
        scrollPane.getViewport().setBackground(Color.BLACK);
        chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("."));
        chooser.setDialogTitle("Select a folder");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setAcceptAllFileFilterUsed(false);
        chooser.setMultiSelectionEnabled(false);

        JPanel checkBoxPane = new JPanel(new GridLayout(2, 5, 0, 0));
        checkBoxPane.setBackground(Color.BLACK);
        for (FileType type : FileType.values()) {
            if (type != FileType.UNKNOWN) {
                checkBoxPane.add(createVisibleFileTypeCheckBox(type));
            }
        }

        // button panel to be added to the south
        JPanel buttonPanel = new JPanel(new GridLayout(1, 3, 0, 0));
        buttonPanel.setBackground(Color.BLACK);
        buttonPanel.setBorder(BorderFactory.createLineBorder(Color.WHITE, 1, true));
        buttonPanel.add(GTk.button("Set", () -> {
            String root = JOptionPane.showInputDialog(null, "Root:", null, JOptionPane.QUESTION_MESSAGE);
            if (root != null) {
                File folder = new File(root);
                if (folder.exists() && folder.isDirectory()) {
                    setRoot(folder.getAbsoluteFile());
                }
            }
        }));
        buttonPanel.add(GTk.button("Select", () -> {
            if (FolderView.this.chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                setRoot(chooser.getSelectedFile());
            }
        }));
        buttonPanel.add(GTk.button("Reload", this::reloadModel));

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
        String[] folderContent = folder.list();
        if (folderContent == null || folderContent.length == 0) {
            return null;
        }

        DefaultMutableTreeNode folderNode = new DefaultMutableTreeNode(folder.getName() + Files.SEPARATOR);
        if (currentRoot != null) { // should only be null at root
            currentRoot.add(folderNode);
        }
        Arrays.sort(folderContent);
        String folderPath = folder.getPath();
        for (String itemName : folderContent) {
            if (IGNORE_FOLDERS.contains(itemName)) {
                continue;
            }
            File newPath = new File(folderPath, itemName).getAbsoluteFile();
            if (newPath.exists()) {
                if (newPath.isDirectory()) {
                    addNodes(folderNode, newPath);
                } else {
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
        checkBox.setFont(GTk.MENU_FONT);
        checkBox.setBackground(Color.BLACK);
        checkBox.setForeground(GTk.APP_THEME_COLOR);
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
}
