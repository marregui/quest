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
 * Copyright 2020, Miguel Arregui a.k.a. marregui
 */

package marregui.crate.cli.widgets.command;

import static marregui.crate.cli.GUITk.createButton;
import static marregui.crate.cli.GUITk.createEtchedFlowPanel;
import static marregui.crate.cli.GUITk.createFlowPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Closeable;
import java.util.function.Supplier;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Utilities;

import marregui.crate.cli.EventConsumer;
import marregui.crate.cli.EventProducer;
import marregui.crate.cli.GUITk;
import marregui.crate.cli.backend.DBConn;
import marregui.crate.cli.backend.SQLExecRequest;
import marregui.crate.cli.backend.Store;
import marregui.crate.cli.widgets.MaskingMouseListener;
import marregui.crate.cli.widgets.MessagePane;


public class CommandBoard extends MessagePane implements EventProducer<CommandBoard.EventType>, Closeable {

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
    private static final String STORE_FILE_NAME = "command-board.json";
    private static final Color CONNECTED_COLOR = new Color(70, 225, 90);
    private static final Font HEADER_FONT = new Font(GUITk.MAIN_FONT_NAME, Font.BOLD, 16);
    private static final Font BODY_FONT = new Font(GUITk.MAIN_FONT_NAME, Font.BOLD, 18);
    private static final Cursor HAND_CURSOR = new Cursor(Cursor.HAND_CURSOR);

    private final Content content;
    private final Store<Content> store;
    private final EventConsumer<CommandBoard, SQLExecRequest> eventConsumer;
    private final JButton execButton;
    private final JButton execLineButton;
    private final JButton cancelButton;
    private final JLabel connLabel;
    private DBConn conn; // uses it when set
    private SQLExecRequest lastRequest;

    /**
     * Constructor.
     * 
     * @param eventConsumer receives the events fired as the user interacts
     */
    public CommandBoard(EventConsumer<CommandBoard, SQLExecRequest> eventConsumer) {
        super();
        this.eventConsumer = eventConsumer;
        store = new Store<>(STORE_FILE_NAME, Content.class);
        store.loadEntriesFromFile();
        content = store.size() > 0 ? store.getEntry(0) : new Content();
        textPane.setText(content.getContent());
        textPane.setEditable(true);
        textPane.setFont(BODY_FONT);
        connLabel = new JLabel();
        connLabel.setFont(HEADER_FONT);
        connLabel.setForeground(GUITk.TABLE_HEADER_FONT_COLOR);
        connLabel.addMouseListener((MouseListener) new MaskingMouseListener() {

            @Override
            public void mouseClicked(MouseEvent e) {
                eventConsumer.onSourceEvent(CommandBoard.this, EventType.CONNECTION_STATUS_CLICKED, null);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setCursor(HAND_CURSOR);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setCursor(Cursor.getDefaultCursor());
            }
        });
        execLineButton = GUITk.createButton("L.Exec", false, this::onExecLineEvent);
        execButton = GUITk.createButton("Exec", false, this::onExecEvent);
        cancelButton = GUITk.createButton("Cancel", false, this::onCancelEvent);
        JButton reloadButton = createButton("Reload", this::onReloadEvent);
        JButton clearButton = createButton("Clear", this::onClearEvent);
        JButton saveButton = createButton("Save", this::onSaveEvent);
        JPanel buttons = createFlowPanel(createEtchedFlowPanel(reloadButton, clearButton, saveButton),
            createEtchedFlowPanel(execLineButton, execButton, cancelButton));
        JPanel controlsPanel = new JPanel(new BorderLayout());
        controlsPanel.add(connLabel, BorderLayout.WEST);
        controlsPanel.add(buttons, BorderLayout.EAST);
        add(controlsPanel, BorderLayout.NORTH);
        refreshControls();
    }

    /**
     * @return the database connection used by the command board
     */
    public DBConn getConnection() {
        return conn;
    }

    /**
     * Sets the database connection used by the command board.
     * 
     * @param conn the connection
     */
    public void setConnection(DBConn conn) {
        this.conn = conn;
        refreshControls();
    }

    @Override
    public void requestFocus() {
        super.requestFocus();
        textPane.requestFocus();
    }

    /**
     * Clears the content of the board.
     * 
     * @param event it is effectively ignored, so it can be null
     */
    private void onClearEvent(ActionEvent event) {
        textPane.setText("");
    }

    /**
     * Reloads the content of from the store.
     * 
     * @param event it is effectively ignored, so it can be null
     */
    private void onReloadEvent(ActionEvent event) {
        textPane.setText(content.getContent());
    }

    /**
     * Saves the content to the store.
     * 
     * @param event it is effectively ignored, so it can be null
     */
    private void onSaveEvent(ActionEvent event) {
        content.setContent(getTextContent());
        store.asyncSaveToFile();
    }

    /**
     * If the connection is set, it fires COMMAND_AVAILABLE. The content of the
     * command is be the selected text on the board, or the full content if nothing
     * is selected.
     * 
     * @param event it is effectively ignored, so it can be null
     */
    public void onExecEvent(ActionEvent event) {
        fireCommandEvent(this::getCommand);
    }

    /**
     * If the connection is set, it fires COMMAND_AVAILABLE. The content of the
     * command is be the full line under the caret.
     * 
     * @param event it is effectively ignored, so it can be null
     */
    public void onExecLineEvent(ActionEvent event) {
        fireCommandEvent(this::getCurrentLine);
    }

    /**
     * If the connection is set and open, it fires COMMAND_CANCEL.
     * 
     * @param event it is effectively ignored, so it can be null
     */
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

    private String getCommand() {
        String cmd = textPane.getSelectedText();
        return cmd != null ? cmd.trim() : getTextContent();
    }

    /**
     * Saves the content of the board to its store file.
     */
    @Override
    public void close() {
        String txt = getTextContent();
        if (!content.getContent().equals(txt)) {
            content.setContent(txt);
            store.addEntry(content, true);
            store.close();
        }
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
        lastRequest = new SQLExecRequest(content.getKey(), conn, command);
        eventConsumer.onSourceEvent(this, EventType.COMMAND_AVAILABLE, lastRequest);
    }

    private void refreshControls() {
        boolean isConnected = conn != null && conn.isOpen();
        String connKey = conn != null ? conn.getKey() : "None set";
        connLabel.setText(String.format("  Connection: [%s]", connKey));
        connLabel.setForeground(isConnected ? CONNECTED_COLOR : Color.BLACK);
        boolean hasText = textPane.getStyledDocument().getLength() > 0;
        execLineButton.setEnabled(hasText);
        execButton.setEnabled(hasText);
        cancelButton.setEnabled(true);
    }

    private String getTextContent() {
        int len = textPane.getStyledDocument().getLength();
        return getFullContent(0, len);
    }

    private String getFullContent(int start, int len) {
        String txt = "";
        try {
            txt = textPane.getStyledDocument().getText(start, len);
        }
        catch (BadLocationException ignore) {
            // not expected to happen in this context
        }
        return txt.trim();
    }

    private String getCurrentLine() {
        int caretPos = textPane.getCaretPosition();
        try {
            int start = Utilities.getRowStart(textPane, caretPos);
            int end = Utilities.getRowEnd(textPane, caretPos) - 1;
            int len = end - start + 1;
            return getFullContent(start, len);
        }
        catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }
}
