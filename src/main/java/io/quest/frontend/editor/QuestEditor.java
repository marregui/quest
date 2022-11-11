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

package io.quest.frontend.editor;

import java.awt.*;
import java.awt.event.*;
import java.awt.font.TextAttribute;
import java.io.Closeable;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.filechooser.FileFilter;
import javax.swing.undo.UndoManager;

import io.quest.backend.SQLExecutionRequest;
import io.quest.model.Store;
import io.quest.model.StoreEntry;
import io.quest.model.*;
import io.quest.model.EventConsumer;
import io.quest.model.EventProducer;
import io.quest.frontend.GTk;
import io.quest.frontend.NoopMouseListener;
import io.quest.frontend.conns.ConnsManager;


public class QuestEditor extends QuestPanel implements EventProducer<QuestEditor.EventType>, Closeable {

    public enum EventType {
        /**
         * L.Exec, or Exec, has been clicked.
         */
        COMMAND_AVAILABLE,
        /**
         * Previous command has been cancelled.
         */
        COMMAND_CANCEL,
        /**
         * User clicked on the connection status label.
         */
        CONNECTION_STATUS_CLICKED
    }

    public static class Content extends StoreEntry {
        private static final String ATTR_NAME = "content";

        public Content() {
            this("default");
        }

        public Content(String name) {
            super(name);
            setAttr(ATTR_NAME, GTk.BANNER);
        }

        public Content(StoreEntry other) {
            super(other);
        }

        @Override
        public final String getUniqueId() {
            return getName();
        }

        public String getContent() {
            return getAttr(ATTR_NAME);
        }

        public void setContent(String content) {
            setAttr(ATTR_NAME, content);
        }
    }

    private static final long serialVersionUID = 1L;
    private static final Color CONNECTED_COLOR = new Color(69, 191, 84);
    private static final Font HEADER_FONT = new Font(GTk.MAIN_FONT_NAME, Font.BOLD, 16);
    private static final Font HEADER_UNDERLINE_FONT = HEADER_FONT.deriveFont(Map.of(
            TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON));
    private static final Cursor HAND_CURSOR = new Cursor(Cursor.HAND_CURSOR);
    private static final String STORE_FILE_NAME = "default-notebook.json";
    private final EventConsumer<QuestEditor, SQLExecutionRequest> eventConsumer;
    private final JComboBox<String> questEntryNames;
    private final List<UndoManager> undoManagers;
    private final JButton execButton;
    private final JButton execLineButton;
    private final JButton cancelButton;
    private final JLabel questLabel;
    private final JLabel connLabel;
    private final FindReplacePanel findPanel;
    private JMenu questsMenu;
    private Store<Content> store;
    private Conn conn; // uses it when set
    private SQLExecutionRequest lastRequest;
    private Content content;

    public QuestEditor(EventConsumer<QuestEditor, SQLExecutionRequest> eventConsumer) {
        super();
        this.eventConsumer = eventConsumer;
        undoManagers = new ArrayList<>(5);
        questEntryNames = new JComboBox<>();
        questEntryNames.setEditable(false);
        questEntryNames.setPreferredSize(new Dimension(200, 25));
        questEntryNames.addActionListener(this::onChangeQuest);
        setupQuestsMenu();
        JPanel topRightPanel = GTk.flowPanel(
                questLabel = createLabel(
                        GTk.Icon.COMMAND_QUEST,
                        "uest",
                        e -> questsMenu
                                .getPopupMenu()
                                .show(e.getComponent(), e.getX() - 30, e.getY())),
                GTk.horizontalSpace(4),
                questEntryNames,
                GTk.horizontalSpace(4),
                GTk.etchedFlowPanel(
                        execLineButton = GTk.button(
                                "L.Exec", false, GTk.Icon.COMMAND_EXEC_LINE,
                                "Execute entire line under caret", this::onExecLine),
                        execButton = GTk.button(
                                "Exec", false, GTk.Icon.COMMAND_EXEC,
                                "Execute selected text", this::onExec),
                        cancelButton = GTk.button(
                                "Abort", false, GTk.Icon.COMMAND_EXEC_ABORT,
                                "Abort current execution", this::fireCancelEvent)));
        findPanel = new FindReplacePanel((source, event, eventData) -> {
            switch ((FindReplacePanel.EventType) EventProducer.eventType(event)) {
                case FIND:
                    onFind();
                    break;
                case REPLACE:
                    onReplace();
                    break;
            }
        });
        JPanel controlsPanel = new JPanel(new BorderLayout(0, 0));
        controlsPanel.add(
                connLabel = createLabel(e -> eventConsumer.onSourceEvent(
                        QuestEditor.this,
                        EventType.CONNECTION_STATUS_CLICKED,
                        null)),
                BorderLayout.WEST
        );
        controlsPanel.add(topRightPanel, BorderLayout.EAST);
        controlsPanel.add(findPanel, BorderLayout.SOUTH);
        add(controlsPanel, BorderLayout.NORTH);
        refreshControls();
        loadStoreEntries(STORE_FILE_NAME);
    }

    public Conn getConnection() {
        return conn;
    }

    public void setConnection(Conn conn) {
        this.conn = conn;
        refreshControls();
    }

    public JMenu getQuestsMenu() {
        return questsMenu;
    }

    public void onExec(ActionEvent event) {
        fireCommandEvent(this::getCommand);
    }

    public void onExecLine(ActionEvent event) {
        fireCommandEvent(this::getCurrentLine);
    }

    public void fireCancelEvent(ActionEvent event) {
        if (conn == null || !conn.isOpen()) {
            JOptionPane.showMessageDialog(this, "Not connected");
            return;
        }
        if (lastRequest != null) {
            eventConsumer.onSourceEvent(this, EventType.COMMAND_CANCEL, lastRequest);
            lastRequest = null;
        }
    }

    public void onFind() {
        onFindReplace(() -> highlightContent(findPanel.getFind()));
    }

    public void onReplace() {
        onFindReplace(() -> replaceContent(findPanel.getFind(), findPanel.getReplace()));
    }

    @Override
    public boolean requestFocusInWindow() {
        return super.requestFocusInWindow() && textPane.requestFocusInWindow();
    }

    @Override
    public void close() {
        undoManagers.clear();
        refreshQuests();
        store.close();
    }

    private String getCommand() {
        String cmd = textPane.getSelectedText();
        return cmd != null ? cmd : getContent();
    }

    private void loadStoreEntries(String fileName) {
        store = new Store<>(fileName, Content.class) {
            @Override
            public Content[] defaultStoreEntries() {
                return new Content[]{new Content()};
            }
        };
        store.loadEntriesFromFile();
        questLabel.setToolTipText(String.format("notebook: %s", fileName));
        undoManagers.clear();
        for (int idx = 0; idx < store.size(); idx++) {
            undoManagers.add(new UndoManager() {
                @Override
                public void undoableEditHappened(UndoableEditEvent e) {
                    if (!Highlighter.EVENT_TYPE.equals(e.getEdit().getPresentationName())) {
                        super.undoableEditHappened(e);
                    }
                }
            });
        }
        refreshQuestEntryNames(0);
    }

    private void onFindReplace(Supplier<Integer> matchesCountSupplier) {
        if (!findPanel.isVisible()) {
            findPanel.setVisible(true);
        } else {
            findPanel.updateMatches(matchesCountSupplier.get());
        }
        findPanel.requestFocusInWindow();
    }

    private void onChangeQuest(ActionEvent event) {
        int idx = questEntryNames.getSelectedIndex();
        if (idx >= 0) {
            if (content != null) {
                if (refreshQuests()) {
                    store.asyncSaveToFile();
                }
            }
            content = store.getEntry(idx, Content::new);
            textPane.setText(content.getContent());
            setUndoManager(undoManagers.get(idx));
        }
    }

    private void onCreateQuest(ActionEvent event) {
        String entryName = JOptionPane.showInputDialog(
                this,
                "Name",
                "New quest",
                JOptionPane.INFORMATION_MESSAGE);
        if (entryName == null || entryName.isEmpty()) {
            return;
        }
        store.addEntry(new Content(entryName), false);
        questEntryNames.addItem(entryName);
        undoManagers.add(new UndoManager() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                if (!Highlighter.EVENT_TYPE.equals(e.getEdit().getPresentationName())) {
                    super.undoableEditHappened(e);
                }
            }
        });
        questEntryNames.setSelectedItem(entryName);
    }

    private void onDeleteQuest(ActionEvent event) {
        int idx = questEntryNames.getSelectedIndex();
        if (idx > 0) {
            if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
                    this,
                    String.format("Delete %s?",
                            questEntryNames.getSelectedItem()),
                    "Deleting quest",
                    JOptionPane.YES_NO_OPTION)) {
                store.removeEntry(idx);
                content = null;
                questEntryNames.removeItemAt(idx);
                undoManagers.remove(idx);
                questEntryNames.setSelectedIndex(idx - 1);
            }
        }
    }

    private void onRenameQuest(ActionEvent event) {
        int idx = questEntryNames.getSelectedIndex();
        if (idx >= 0) {
            String currentName = (String) questEntryNames.getSelectedItem();
            String newName = JOptionPane.showInputDialog(
                    this,
                    "New name",
                    "Renaming quest",
                    JOptionPane.QUESTION_MESSAGE);
            if (newName != null && !newName.isBlank() && !newName.equals(currentName)) {
                store.getEntry(idx, null).setName(newName);
                refreshQuestEntryNames(idx);
            }
        }
    }

    private void onBackupQuests(ActionEvent event) {
        JFileChooser choose = new JFileChooser(store.getRootPath());
        choose.setDialogTitle("Backing up quests");
        choose.setDialogType(JFileChooser.SAVE_DIALOG);
        choose.setFileSelectionMode(JFileChooser.FILES_ONLY);
        choose.setMultiSelectionEnabled(false);
        if (JFileChooser.APPROVE_OPTION == choose.showSaveDialog(this)) {
            File selectedFile = choose.getSelectedFile();
            try {
                if (!selectedFile.exists()) {
                    store.saveToFile(selectedFile);
                } else {
                    if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
                            this,
                            "Override file?",
                            "Dilemma",
                            JOptionPane.YES_NO_OPTION)) {
                        store.saveToFile(selectedFile);
                    }
                }
            } catch (Throwable t) {
                JOptionPane.showMessageDialog(
                        this,
                        String.format("Could not save file '%s': %s",
                                selectedFile.getAbsolutePath(),
                                t.getMessage()),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void onLoadQuestsFromBackup(ActionEvent event) {
        JFileChooser choose = new JFileChooser(store.getRootPath());
        choose.setDialogTitle("Loading quests from backup");
        choose.setDialogType(JFileChooser.OPEN_DIALOG);
        choose.setFileSelectionMode(JFileChooser.FILES_ONLY);
        choose.setMultiSelectionEnabled(false);
        choose.setFileFilter(new FileFilter() {
            @Override
            public boolean accept(File f) {
                String name = f.getName();
                return name.endsWith(".json") && !name.equals(ConnsManager.STORE_FILE_NAME);
            }

            @Override
            public String getDescription() {
                return "JSON files";
            }
        });

        if (JFileChooser.APPROVE_OPTION == choose.showOpenDialog(this)) {
            File selectedFile = choose.getSelectedFile();
            try {
                loadStoreEntries(selectedFile.getName());
            } catch (Throwable t) {
                JOptionPane.showMessageDialog(
                        this,
                        String.format("Could not load file '%s': %s",
                                selectedFile.getAbsolutePath(),
                                t.getMessage()),
                        "Error",
                        JOptionPane.ERROR_MESSAGE
                );
            }
        }
    }

    private void onClearQuest(ActionEvent event) {
        textPane.setText("");
    }

    private void onReloadQuest(ActionEvent event) {
        textPane.setText(content.getContent());
    }

    private void onSaveQuest(ActionEvent event) {
        if (refreshQuests()) {
            store.asyncSaveToFile();
        }
    }

    private boolean refreshQuests() {
        if (content != null) {
            String txt = getContent();
            String current = content.getContent();
            if (current != null && !current.equals(txt)) {
                content.setContent(txt);
                return true;
            }
        }
        return false;
    }

    private void refreshQuestEntryNames(int idx) {
        questEntryNames.removeAllItems();
        for (String item : store.entryNames()) {
            questEntryNames.addItem(item);
        }
        if (idx >= 0 && idx < questEntryNames.getItemCount()) {
            questEntryNames.setSelectedIndex(idx);
        }
    }

    private void fireCommandEvent(Supplier<String> commandSupplier) {
        if (conn == null) {
            JOptionPane.showMessageDialog(
                    this,
                    "Connection not set, assign one");
            return;
        }
        String command = commandSupplier.get();
        if (command == null || command.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Command not available, type something");
            return;
        }
        if (lastRequest != null) {
            eventConsumer.onSourceEvent(this, EventType.COMMAND_CANCEL, lastRequest);
            lastRequest = null;
        }
        lastRequest = new SQLExecutionRequest(content.getUniqueId(), conn, command);
        eventConsumer.onSourceEvent(this, EventType.COMMAND_AVAILABLE, lastRequest);
    }

    private void refreshControls() {
        boolean isConnected = conn != null && conn.isOpen();
        String connKey = conn != null ? conn.getUniqueId() : "None set";
        connLabel.setText(String.format("[%s]", connKey));
        connLabel.setForeground(isConnected ? CONNECTED_COLOR : Color.BLACK);
        connLabel.setIcon(isConnected ? GTk.Icon.CONN_UP.icon() : GTk.Icon.CONN_DOWN.icon());
        boolean hasText = textPane.getStyledDocument().getLength() > 0;
        execLineButton.setEnabled(hasText);
        execButton.setEnabled(hasText);
        cancelButton.setEnabled(true);
    }

    private void setupQuestsMenu() {
        questsMenu = new JMenu();
        questsMenu.setFont(GTk.MENU_FONT);
        questsMenu.add(
                GTk.configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_CLEAR,
                        "Clear quest",
                        GTk.NO_KEY_EVENT,
                        this::onClearQuest));
        questsMenu.add(
                GTk.configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_RELOAD,
                        "Reload quest",
                        "Recovers quest from last save",
                        GTk.NO_KEY_EVENT,
                        this::onReloadQuest));
        questsMenu.add(
                GTk.configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_SAVE,
                        "Save quest",
                        GTk.NO_KEY_EVENT,
                        this::onSaveQuest));
        questsMenu.addSeparator();
        questsMenu.add(
                GTk.configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_ADD,
                        "New quest",
                        GTk.NO_KEY_EVENT,
                        this::onCreateQuest));
        questsMenu.add(
                GTk.configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_EDIT,
                        "Rename quest",
                        GTk.NO_KEY_EVENT,
                        this::onRenameQuest));
        questsMenu.add(
                GTk.configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_REMOVE,
                        "Delete quest",
                        GTk.NO_KEY_EVENT,
                        this::onDeleteQuest));
        questsMenu.addSeparator();
        questsMenu.add(
                GTk.configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_STORE_LOAD,
                        "Load quests from notebook",
                        GTk.NO_KEY_EVENT,
                        this::onLoadQuestsFromBackup));
        questsMenu.add(
                GTk.configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_STORE_BACKUP,
                        "Save quests to new notebook",
                        GTk.NO_KEY_EVENT,
                        this::onBackupQuests));
    }

    private JLabel createLabel(Consumer<MouseEvent> consumer) {
        return createLabel(GTk.Icon.NO_ICON, null, consumer);
    }

    private JLabel createLabel(GTk.Icon icon, String text, Consumer<MouseEvent> consumer) {
        JLabel label = new JLabel();
        if (text != null) {
            label.setText(text);
        }
        if (icon != GTk.Icon.NO_ICON) {
            label.setIcon(icon.icon());
        }
        label.setFont(HEADER_FONT);
        label.setForeground(GTk.TABLE_HEADER_FONT_COLOR);
        label.addMouseListener(new LabelMouseListener(label) {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                consumer.accept(e);
            }
        });
        return label;
    }

    private class LabelMouseListener implements NoopMouseListener {
        private final JLabel label;

        private LabelMouseListener(JLabel label) {
            this.label = label;
        }

        @Override
        public void mouseEntered(MouseEvent e) {
            setCursor(HAND_CURSOR);
            label.setFont(HEADER_UNDERLINE_FONT);
        }

        @Override
        public void mouseExited(MouseEvent e) {
            setCursor(Cursor.getDefaultCursor());
            label.setFont(HEADER_FONT);
        }
    }
}
