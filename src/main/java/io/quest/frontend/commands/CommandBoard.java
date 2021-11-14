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

package io.quest.frontend.commands;

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

import io.quest.model.*;
import io.quest.EventConsumer;
import io.quest.EventProducer;
import io.quest.frontend.GTk;
import io.quest.frontend.NoopMouseListener;
import io.quest.frontend.conns.ConnsManager;


public class CommandBoard extends QuestPanel implements EventProducer<CommandBoard.EventType>, Closeable {

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
        public final String getKey() {
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
    private final EventConsumer<CommandBoard, SQLRequest> eventConsumer;
    private final JComboBox<String> boardEntryNames;
    private final List<UndoManager> undoManagers; // same order as boardEntries' model
    private final JButton execButton;
    private final JButton execLineButton;
    private final JButton cancelButton;
    private final JLabel questLabel;
    private final JLabel connLabel;
    private final FindReplacePanel findPanel;
    private JMenu commandBoardMenu;
    private Store<Content> store;
    private Conn conn; // uses it when set
    private SQLRequest lastRequest;
    private Content content;

    public CommandBoard(EventConsumer<CommandBoard, SQLRequest> eventConsumer) {
        super();
        this.eventConsumer = eventConsumer;
        undoManagers = new ArrayList<>(5);
        boardEntryNames = new JComboBox<>();
        boardEntryNames.setEditable(false);
        boardEntryNames.setPreferredSize(new Dimension(200, 25));
        boardEntryNames.addActionListener(this::onChangeBoard);
        setupBoardMenu();
        JPanel topRightPanel = GTk.flowPanel(
                questLabel = createLabel(
                        GTk.Icon.COMMAND_QUEST,
                        "uest",
                        e -> commandBoardMenu
                                .getPopupMenu()
                                .show(e.getComponent(), e.getX() - 30, e.getY())),
                GTk.horizontalSpace(4),
                boardEntryNames,
                GTk.horizontalSpace(4),
                GTk.etchedFlowPanel(
                        execLineButton = GTk.button(
                                "L.Exec", false, GTk.Icon.COMMAND_EXEC_LINE,
                                "Execute entire line under caret", this::onExecLine),
                        execButton = GTk.button(
                                "Exec", false, GTk.Icon.COMMAND_EXEC,
                                "Execute selected text", this::onExec),
                        cancelButton = GTk.button(
                                "Cancel", false, GTk.Icon.COMMAND_EXEC_CANCEL,
                                "Cancel current execution", this::fireCancelEvent)));
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
                        CommandBoard.this,
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

    public JMenu getCommandBoardMenu() {
        return commandBoardMenu;
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
        refreshStore();
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
        refreshBoardEntryNames(0);
    }

    private void onFindReplace(Supplier<Integer> matchesCountSupplier) {
        if (!findPanel.isVisible()) {
            findPanel.setVisible(true);
        } else {
            findPanel.updateMatches(matchesCountSupplier.get());
        }
        findPanel.requestFocusInWindow();
    }

    private void onChangeBoard(ActionEvent event) {
        int idx = boardEntryNames.getSelectedIndex();
        if (idx >= 0) {
            if (content != null) {
                if (refreshStore()) {
                    if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
                            this,
                            "Content has changed, save?, if not -> discard it",
                            "Command board changed",
                            JOptionPane.YES_NO_OPTION,
                            JOptionPane.QUESTION_MESSAGE)) {
                        store.asyncSaveToFile();
                    }
                }
            }
            content = store.getEntry(idx, Content::new);
            textPane.setText(content.getContent());
            setUndoManager(undoManagers.get(idx));
        }
    }

    private void onCreateBoard(ActionEvent event) {
        String entryName = JOptionPane.showInputDialog(
                this,
                "Name",
                "New Command Board",
                JOptionPane.INFORMATION_MESSAGE);
        if (entryName == null || entryName.isEmpty()) {
            return;
        }
        store.addEntry(new Content(entryName), false);
        boardEntryNames.addItem(entryName);
        undoManagers.add(new UndoManager() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                if (!Highlighter.EVENT_TYPE.equals(e.getEdit().getPresentationName())) {
                    super.undoableEditHappened(e);
                }
            }
        });
        boardEntryNames.setSelectedItem(entryName);
    }

    private void onDeleteBoard(ActionEvent event) {
        int idx = boardEntryNames.getSelectedIndex();
        if (idx > 0) {
            if (JOptionPane.YES_OPTION == JOptionPane.showConfirmDialog(
                    this,
                    String.format("Delete %s?",
                            boardEntryNames.getSelectedItem()),
                    "Deleting board",
                    JOptionPane.YES_NO_OPTION)) {
                store.removeEntry(idx);
                boardEntryNames.removeItemAt(idx);
                undoManagers.remove(idx);
                boardEntryNames.setSelectedIndex(idx - 1);
            }
        }
    }

    private void onRenameBoard(ActionEvent event) {
        int idx = boardEntryNames.getSelectedIndex();
        if (idx >= 0) {
            String currentName = (String) boardEntryNames.getSelectedItem();
            String newName = JOptionPane.showInputDialog(
                    this,
                    "New name",
                    "Renaming board",
                    JOptionPane.QUESTION_MESSAGE);
            if (newName != null && !newName.isBlank() && !newName.equals(currentName)) {
                store.getEntry(idx, null).setName(newName);
                refreshBoardEntryNames(idx);
            }
        }
    }

    private void onBackupBoards(ActionEvent event) {
        JFileChooser choose = new JFileChooser(store.getRootPath());
        choose.setDialogTitle("Backing up store");
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

    private void onLoadBoardsFromBackup(ActionEvent event) {
        JFileChooser choose = new JFileChooser(store.getRootPath());
        choose.setDialogTitle("Loading store from backup");
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

    private void onClearBoard(ActionEvent event) {
        textPane.setText("");
    }

    private void onReloadBoard(ActionEvent event) {
        textPane.setText(content.getContent());
    }

    private void onSaveBoard(ActionEvent event) {
        if (refreshStore()) {
            store.asyncSaveToFile();
        }
    }

    private boolean refreshStore() {
        String txt = getContent();
        String current = content.getContent();
        if (current != null && !current.equals(txt)) {
            content.setContent(txt);
            return true;
        }
        return false;
    }

    private void refreshBoardEntryNames(int idx) {
        boardEntryNames.removeAllItems();
        for (String item : store.entryNames()) {
            boardEntryNames.addItem(item);
        }
        if (idx >= 0 && idx < boardEntryNames.getItemCount()) {
            boardEntryNames.setSelectedIndex(idx);
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
        lastRequest = new SQLRequest(content.getKey(), conn, command);
        eventConsumer.onSourceEvent(this, EventType.COMMAND_AVAILABLE, lastRequest);
    }

    private void refreshControls() {
        boolean isConnected = conn != null && conn.isOpen();
        String connKey = conn != null ? conn.getKey() : "None set";
        connLabel.setText(String.format("[%s]", connKey));
        connLabel.setForeground(isConnected ? CONNECTED_COLOR : Color.BLACK);
        connLabel.setIcon(isConnected ? GTk.Icon.CONN_UP.icon() : GTk.Icon.CONN_DOWN.icon());
        boolean hasText = textPane.getStyledDocument().getLength() > 0;
        execLineButton.setEnabled(hasText);
        execButton.setEnabled(hasText);
        cancelButton.setEnabled(true);
    }

    private void setupBoardMenu() {
        commandBoardMenu = new JMenu();
        commandBoardMenu.setFont(GTk.MENU_FONT);
        commandBoardMenu.add(
                GTk.configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_CLEAR,
                        "Clear quest",
                        GTk.NO_KEY_EVENT,
                        this::onClearBoard));
        commandBoardMenu.add(
                GTk.configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_RELOAD,
                        "Reload quest",
                        "Recovers quest from last save",
                        GTk.NO_KEY_EVENT,
                        this::onReloadBoard));
        commandBoardMenu.add(
                GTk.configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_SAVE,
                        "Save quest",
                        GTk.NO_KEY_EVENT,
                        this::onSaveBoard));
        commandBoardMenu.addSeparator();
        commandBoardMenu.add(
                GTk.configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_ADD,
                        "New quest",
                        GTk.NO_KEY_EVENT,
                        this::onCreateBoard));
        commandBoardMenu.add(
                GTk.configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_EDIT,
                        "Rename quest",
                        GTk.NO_KEY_EVENT,
                        this::onRenameBoard));
        commandBoardMenu.add(
                GTk.configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_REMOVE,
                        "Delete quest",
                        GTk.NO_KEY_EVENT,
                        this::onDeleteBoard));
        commandBoardMenu.addSeparator();
        commandBoardMenu.add(
                GTk.configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_STORE_LOAD,
                        "Load quests from notebook",
                        GTk.NO_KEY_EVENT,
                        this::onLoadBoardsFromBackup));
        commandBoardMenu.add(
                GTk.configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_STORE_BACKUP,
                        "Save quests to new notebook",
                        GTk.NO_KEY_EVENT,
                        this::onBackupBoards));
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
