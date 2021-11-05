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
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
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
import javax.swing.undo.UndoManager;

import io.quest.common.EventConsumer;
import io.quest.common.EventProducer;
import io.quest.common.GTk;
import io.quest.backend.Conn;
import io.quest.backend.SQLRequest;
import io.quest.backend.Store;
import io.quest.frontend.MaskingMouseListener;


public class CommandBoard extends TextPane implements EventProducer<CommandBoard.EventType>, Closeable {

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

    private static final long serialVersionUID = 1L;
    private static final Color CONNECTED_COLOR = new Color(69, 191, 84);
    private static final Font HEADER_FONT = new Font(GTk.MAIN_FONT_NAME, Font.BOLD, 16);
    private static final Font HEADER_UNDERLINE_FONT = HEADER_FONT.deriveFont(Map.of(
            TextAttribute.UNDERLINE, TextAttribute.UNDERLINE_ON));
    private static final Font FIND_FONT = new Font(GTk.MAIN_FONT_NAME, Font.BOLD, 14);
    private static final Color FIND_FONT_COLOR = new Color(58, 138, 138);
    private static final Cursor HAND_CURSOR = new Cursor(Cursor.HAND_CURSOR);
    private static final String STORE_FILE_NAME = "command-board.json";
    private final EventConsumer<CommandBoard, SQLRequest> eventConsumer;
    private final JComboBox<String> boardEntryNames;
    private final List<UndoManager> undoManagers; // same order as boardEntries' model
    private final JButton execButton;
    private final JButton execLineButton;
    private final JButton cancelButton;
    private final JLabel questLabel;
    private final JLabel connLabel;
    private JMenu commandBoardMenu;
    private JPanel findPanel;
    private JTextField findText;
    private JTextField replaceWithText;
    private JLabel findMatchesLabel;
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
        boardEntryNames.setPreferredSize(new Dimension(180, 25));
        boardEntryNames.addActionListener(this::onChangeBoardEvent);
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
                                "L.Exec", false, GTk.Icon.EXEC_LINE,
                                "Execute entire line under caret", this::onExecLineEvent),
                        execButton = GTk.button(
                                "Exec", false, GTk.Icon.EXEC,
                                "Execute selected text", this::onExecEvent),
                        cancelButton = GTk.button(
                                "Whack", false, GTk.Icon.EXEC_CANCEL,
                                "Whack current execution", this::fireCancelEvent)));
        setupFindPanel();
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

    public void onExecEvent(ActionEvent event) {
        fireCommandEvent(this::getCommand);
    }

    public void onExecLineEvent(ActionEvent event) {
        fireCommandEvent(this::getCurrentLine);
    }

    public void onFind(ActionEvent event) {
        onFindReplaceEvent(() -> highlightContent(findText.getText()));
    }

    public void onReplace(ActionEvent event) {
        onFindReplaceEvent(() -> replaceContent(findText.getText(), replaceWithText.getText()));
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

    @Override
    public boolean requestFocusInWindow() {
        return super.requestFocusInWindow() && textPane.requestFocusInWindow();
    }

    @Override
    public void close() {
        undoManagers.clear();
        commitContent();
        store.close();
    }

    private String getCommand() {
        String cmd = textPane.getSelectedText();
        return cmd != null ? cmd.trim() : getContent();
    }

    private void loadStoreEntries(String fileName) {
        store = new Store<>(fileName, Content.class) {
            @Override
            public Content[] defaultStoreEntries() {
                return new Content[]{new Content("default")};
            }
        };
        store.loadEntriesFromFile();
        questLabel.setToolTipText(String.format("file: %s", fileName));
        undoManagers.clear();
        for (int idx = 0; idx < store.size(); idx++) {
            undoManagers.add(new UndoManager() {
                @Override
                public void undoableEditHappened(UndoableEditEvent e) {
                    if (!"style change".equals(e.getEdit().getPresentationName())) {
                        super.undoableEditHappened(e);
                    }
                }
            });
        }
        refreshBoardEntryNames(0);
    }

    private void onFindReplaceEvent(Supplier<Integer> matchesCountSupplier) {
        if (!findPanel.isVisible()) {
            findPanel.setVisible(true);
        } else {
            int matches = matchesCountSupplier.get();
            findMatchesLabel.setText(String.format("%d %s", matches, matches == 1 ? "match" : "matches"));
        }
        findText.requestFocusInWindow();
    }

    private void onCloseFindReplaceView(ActionEvent event) {
        findPanel.setVisible(false);
    }

    private void onChangeBoardEvent(ActionEvent event) {
        int idx = boardEntryNames.getSelectedIndex();
        if (idx >= 0) {
            if (content != null) {
                // save content of current board if there are changes (all boards in fact)
                onSaveBoard(event);
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
                if (!"style change".equals(e.getEdit().getPresentationName())) {
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
        if (commitContent()) {
            store.asyncSaveToFile();
        }
    }

    private boolean commitContent() {
        String txt = getContent();
        if (!content.getContent().equals(txt)) {
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

    private void setupFindPanel() {
        JLabel findLabel = new JLabel("Find");
        findLabel.setFont(HEADER_FONT);
        findLabel.setForeground(GTk.TABLE_HEADER_FONT_COLOR);
        findText = new JTextField(35);
        findText.setFont(FIND_FONT);
        findText.setForeground(FIND_FONT_COLOR);

        findText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    onFind(null);
                } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    replaceWithText.requestFocusInWindow();
                } else {
                    super.keyReleased(e);
                }
            }
        });
        JLabel replaceWithLabel = new JLabel("replace with");
        replaceWithLabel.setFont(HEADER_FONT);
        replaceWithLabel.setForeground(GTk.TABLE_HEADER_FONT_COLOR);
        replaceWithText = new JTextField(35);
        replaceWithText.setFont(FIND_FONT);
        replaceWithText.setForeground(FIND_FONT_COLOR);
        replaceWithText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    onReplace(null);
                } else if (e.getKeyCode() == KeyEvent.VK_TAB) {
                    findText.requestFocusInWindow();
                } else {
                    super.keyReleased(e);
                }
            }
        });
        findMatchesLabel = new JLabel("0 matches");
        findMatchesLabel.setFont(HEADER_FONT);
        findMatchesLabel.setForeground(GTk.TABLE_HEADER_FONT_COLOR);
        findPanel = GTk.flowPanel(7, 2,
                findLabel,
                findText,
                replaceWithLabel,
                replaceWithText,
                findMatchesLabel,
                GTk.button(
                        "Find",
                        GTk.Icon.COMMAND_FIND,
                        "Find matching text in command board",
                        this::onFind),
                GTk.button(
                        "Replace",
                        GTk.Icon.COMMAND_REPLACE,
                        "Replace the matching text in selected area",
                        this::onReplace),
                GTk.button(
                        "X",
                        GTk.Icon.NO_ICON,
                        "Close find/replace view",
                        this::onCloseFindReplaceView));
        findPanel.setVisible(false);
    }

    private void setupBoardMenu() {
        commandBoardMenu = new JMenu();
        commandBoardMenu.setFont(GTk.MENU_FONT);
        commandBoardMenu.add(
                GTk.configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_STORE_BACKUP,
                        "Save All quests to new file",
                        GTk.NO_KEY_EVENT,
                        this::onBackupBoards));
        commandBoardMenu.add(
                GTk.configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_STORE_LOAD,
                        "Load quests from file",
                        GTk.NO_KEY_EVENT,
                        this::onLoadBoardsFromBackup));
        commandBoardMenu.add(
                GTk.configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_CLEAR,
                        "Clear current quest",
                        "Clears current quest board, does not save",
                        GTk.NO_KEY_EVENT,
                        this::onClearBoard));
        commandBoardMenu.add(
                GTk.configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.RELOAD,
                        "Reload last saved",
                        "Recovers current quest board from last save",
                        GTk.NO_KEY_EVENT,
                        this::onReloadBoard));
        commandBoardMenu.add(
                GTk.configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_SAVE,
                        "Save All quests",
                        GTk.NO_KEY_EVENT,
                        this::onSaveBoard));
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
                        GTk.Icon.COMMAND_REMOVE,
                        "Delete quest",
                        GTk.NO_KEY_EVENT,
                        this::onDeleteBoard));
        commandBoardMenu.add(
                GTk.configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_EDIT,
                        "Rename quest",
                        GTk.NO_KEY_EVENT,
                        this::onRenameBoard));
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

    private class LabelMouseListener implements MaskingMouseListener {
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
