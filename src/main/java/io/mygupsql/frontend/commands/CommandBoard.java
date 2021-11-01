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

package io.mygupsql.frontend.commands;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.font.TextAttribute;
import java.io.Closeable;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.function.Supplier;

import javax.swing.*;
import javax.swing.event.UndoableEditEvent;
import javax.swing.undo.UndoManager;

import io.mygupsql.EventConsumer;
import io.mygupsql.EventProducer;
import io.mygupsql.GTk;
import io.mygupsql.backend.Conn;
import io.mygupsql.backend.SQLRequest;
import io.mygupsql.backend.Store;
import io.mygupsql.frontend.MaskingMouseListener;

import static io.mygupsql.GTk.configureMenuItem;


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
    private static final Cursor HAND_CURSOR = new Cursor(Cursor.HAND_CURSOR);
    private static final String STORE_FILE_NAME = "command-board.json";
    private final JMenu storeMenu;
    private final EventConsumer<CommandBoard, SQLRequest> eventConsumer;
    private final JComboBox<String> storeEntries;
    private final List<UndoManager> storeUndoManagers;
    private final JButton execButton;
    private final JButton execLineButton;
    private final JButton cancelButton;
    private final JLabel commandBoardLabel;
    private final JLabel connLabel;
    private Store<Content> store;
    private Conn conn; // uses it when set
    private SQLRequest lastRequest;
    private Content content;

    public CommandBoard(EventConsumer<CommandBoard, SQLRequest> eventConsumer) {
        super();
        this.eventConsumer = eventConsumer;
        connLabel = new JLabel();
        connLabel.setFont(HEADER_FONT);
        connLabel.setForeground(GTk.TABLE_HEADER_FONT_COLOR);
        connLabel.addMouseListener(new MaskingMouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                eventConsumer.onSourceEvent(CommandBoard.this, EventType.CONNECTION_STATUS_CLICKED, null);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setCursor(HAND_CURSOR);
                connLabel.setFont(HEADER_UNDERLINE_FONT);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
                connLabel.setFont(HEADER_FONT);
            }
        });
        storeEntries = new JComboBox<>();
        storeEntries.setEditable(false);
        storeEntries.setPreferredSize(new Dimension(150, 25));
        storeEntries.addActionListener(this::onChangeCommandBoardEvent);
        storeUndoManagers = new ArrayList<>();
        storeMenu = new JMenu();
        storeMenu.setFont(GTk.MENU_FONT);
        storeMenu.add(
                configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_STORE_BACKUP,
                        "Backup to file",
                        GTk.NO_KEY_EVENT,
                        this::onBackupCommandBoardsEvent));
        storeMenu.add(
                configureMenuItem(
                        new JMenuItem(),
                        GTk.Icon.COMMAND_STORE_LOAD,
                        "Load from backup",
                        GTk.NO_KEY_EVENT,
                        this::onLoadCommandBoardsFromBackupEvent));
        commandBoardLabel = new JLabel("Command board:");
        commandBoardLabel.setFont(HEADER_FONT);
        commandBoardLabel.setForeground(GTk.TABLE_HEADER_FONT_COLOR);
        commandBoardLabel.addMouseListener(new MaskingMouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                storeMenu.getPopupMenu().show(e.getComponent(), e.getX() - 30, e.getY());
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setCursor(HAND_CURSOR);
                commandBoardLabel.setFont(HEADER_UNDERLINE_FONT);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
                commandBoardLabel.setFont(HEADER_FONT);
            }
        });
        JPanel buttons = GTk.createFlowPanel(
                commandBoardLabel,
                GTk.createHorizontalSpace(2),
                storeEntries,
                GTk.createHorizontalSpace(4),
                GTk.createEtchedFlowPanel(
                        GTk.createButton("", true, GTk.Icon.COMMAND_CLEAR,
                                "Clear selected board", this::onClearEvent),
                        GTk.createButton("", true, GTk.Icon.RELOAD,
                                "Reload last saved content for selected board", this::onReloadEvent),
                        GTk.createButton("", true, GTk.Icon.COMMAND_SAVE,
                                "Save selected board", this::onSaveEvent),
                        GTk.createButton("", true, GTk.Icon.COMMAND_ADD,
                                "Create new board", this::onCreateCommandBoardEvent),
                        GTk.createButton("", true, GTk.Icon.COMMAND_REMOVE,
                                "Delete selected board", this::onDeleteCommandBoardEvent)),
                GTk.createHorizontalSpace(37),
                GTk.createEtchedFlowPanel(
                        execLineButton = GTk.createButton(
                                "L.Exec", false, GTk.Icon.EXEC_LINE,
                                "Execute entire line under caret", this::onExecLineEvent),
                        execButton = GTk.createButton(
                                "Exec", false, GTk.Icon.EXEC,
                                "Execute selected text", this::onExecEvent),
                        cancelButton = GTk.createButton(
                                "Cancel", false, GTk.Icon.EXEC_CANCEL,
                                "Cancel current execution", this::onCancelEvent)));
        JPanel controlsPanel = new JPanel(new BorderLayout());
        controlsPanel.add(connLabel, BorderLayout.WEST);
        controlsPanel.add(buttons, BorderLayout.EAST);
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

    @Override
    public void requestFocus() {
        super.requestFocus();
        textPane.requestFocus();
    }

    public void onExecEvent(ActionEvent event) {
        fireCommandEvent(this::getCommand);
    }

    public void onExecLineEvent(ActionEvent event) {
        fireCommandEvent(this::getCurrentLine);
    }

    private void loadStoreEntries(String fileName) {
        store = new Store<>(fileName, Content.class) {

            @Override
            public Content[] defaultStoreEntries() {
                return new Content[]{
                        new Content("default")
                };
            }
        };
        store.loadEntriesFromFile();
        commandBoardLabel.setToolTipText(fileName);
        storeUndoManagers.clear();
        for (int idx = 0; idx < store.size(); idx++) {
            storeUndoManagers.add(new UndoManager() {
                @Override
                public void undoableEditHappened(UndoableEditEvent e) {
                    if (!"style change".equals(e.getEdit().getPresentationName())) {
                        super.undoableEditHappened(e);
                    }
                }
            });
        }
        storeEntries.removeAllItems();
        for (String item :store.entryNames()) {
            storeEntries.addItem(item);
        }
        storeEntries.setSelectedIndex(0);
    }

    private void onChangeCommandBoardEvent(ActionEvent event) {
        int idx = storeEntries.getSelectedIndex();
        if (idx >= 0) {
            if (content != null) {
                // save content of current board if there are changes (all boards in fact)
                onSaveEvent(event);
            }
            content = store.getEntry(idx, Content::new);
            textPane.setText(content.getContent());
            setUndoManager(storeUndoManagers.get(idx));
        }
    }

    private void onCreateCommandBoardEvent(ActionEvent event) {
        String entryName = JOptionPane.showInputDialog(
                this,
                "Name",
                "New Command Board",
                JOptionPane.INFORMATION_MESSAGE);
        if (entryName == null || entryName.isEmpty()) {
            return;
        }
        store.addEntry(new Content(entryName), false);
        storeEntries.addItem(entryName);
        storeUndoManagers.add(new UndoManager() {
            @Override
            public void undoableEditHappened(UndoableEditEvent e) {
                if (!"style change".equals(e.getEdit().getPresentationName())) {
                    super.undoableEditHappened(e);
                }
            }
        });
        storeEntries.setSelectedItem(entryName);
    }

    private void onDeleteCommandBoardEvent(ActionEvent event) {
        int idx = storeEntries.getSelectedIndex();
        if (idx > 0) {
            store.removeEntry(idx);
            storeEntries.removeItemAt(idx);
            storeUndoManagers.remove(idx);
            storeEntries.setSelectedIndex(idx - 1);
        }
    }

    private void onBackupCommandBoardsEvent(ActionEvent event) {
        JFileChooser chooseBackupFile = new JFileChooser(store.getRootPath());
        chooseBackupFile.setDialogTitle("Backing up store");
        chooseBackupFile.setSelectedFile(new File("command-board-backup.json"));
        chooseBackupFile.setDialogType(JFileChooser.SAVE_DIALOG);
        chooseBackupFile.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooseBackupFile.setMultiSelectionEnabled(false);
        if (JFileChooser.APPROVE_OPTION == chooseBackupFile.showSaveDialog(this)) {
            File selectedFile = chooseBackupFile.getSelectedFile();
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

    private void onLoadCommandBoardsFromBackupEvent(ActionEvent event) {
        JFileChooser chooseLoadFile = new JFileChooser(store.getRootPath());
        chooseLoadFile.setDialogTitle("Loading store from backup");
        chooseLoadFile.setDialogType(JFileChooser.OPEN_DIALOG);
        chooseLoadFile.setFileSelectionMode(JFileChooser.FILES_ONLY);
        chooseLoadFile.setMultiSelectionEnabled(false);
        if (JFileChooser.APPROVE_OPTION == chooseLoadFile.showOpenDialog(this)) {
            File selectedFile = chooseLoadFile.getSelectedFile();
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

    private void onClearEvent(ActionEvent event) {
        textPane.setText("");
    }

    private void onReloadEvent(ActionEvent event) {
        textPane.setText(content.getContent());
    }

    private void onSaveEvent(ActionEvent event) {
        if (updateContent()) {
            store.asyncSaveToFile();
        }
    }

    public void onCancelEvent(ActionEvent event) {
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
    public void close() {
        storeUndoManagers.clear();
        updateContent();
        store.close();
    }

    private String getCommand() {
        String cmd = textPane.getSelectedText();
        return cmd != null ? cmd.trim() : getContent();
    }

    private boolean updateContent() {
        String txt = getContent();
        if (!content.getContent().equals(txt)) {
            content.setContent(txt);
            return true;
        }
        return false;
    }

    private void fireCommandEvent(Supplier<String> commandSupplier) {
        if (conn == null) {
            JOptionPane.showMessageDialog(this, "Connection not set, assign one");
            return;
        }
        String command = commandSupplier.get();
        if (command == null || command.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Command not available, type something");
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
}
