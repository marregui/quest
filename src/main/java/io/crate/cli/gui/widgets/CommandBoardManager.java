package io.crate.cli.gui.widgets;

import io.crate.cli.connections.SQLConnection;
import io.crate.cli.connections.SQLExecutionRequest;
import io.crate.cli.connections.SQLExecutionResponse;
import io.crate.cli.gui.common.EventListener;
import io.crate.cli.gui.common.EventSpeaker;
import io.crate.cli.gui.common.GUIFactory;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Utilities;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.Locale;
import java.util.function.Supplier;


public class CommandBoardManager extends JPanel implements EventSpeaker<CommandBoardManager.EventType> {

    public enum EventType {
        COMMAND_AVAILABLE,
        COMMAND_CANCEL,
        CONNECT_KEYBOARD_REQUEST,
        BOARD_CHANGE
    }

    private static final int NUM_BOARDS = 8;
    private static final String BORDER_TITLE = "Command Board";


    private final JTextPane textPane;
    private final JButton clearButton;
    private final JButton runButton;
    private final JButton runLineButton;
    private final JButton cancelButton;
    private final JButton[] boardHeaderButtons;
    private final TitledBorder titleBorder;
    private final BoardData boardData;
    private final EventListener<CommandBoardManager, SQLExecutionRequest> eventListener;


    private static class BoardData {

        private static String toKey(int offset) {
            return String.valueOf((char) ('A' + offset));
        }

        private final String[] boardText;
        private final SQLConnection[] boardConnection;
        private int currentIdx;

        private BoardData(int size) {
            boardText = new String[size];
            boardConnection = new SQLConnection[size];
            Arrays.fill(boardText, "");
            Arrays.fill(boardConnection, null);
            currentIdx = 0;
        }

        String currentKey() {
            return toKey(currentIdx);
        }

        int currentIdx() {
            return currentIdx;
        }

        void currentIdx(int idx) {
            currentIdx = idx;
        }

        SQLConnection currentSQLConnection() {
            SQLConnection conn = boardConnection[currentIdx];
            if (null == conn) {
                conn = findFirstConnection(true);
                if (null == conn) {
                    conn = findFirstConnection(false);
                }
                boardConnection[currentIdx] = conn;
            }
            return conn;
        }

        void currentSQLConnection(SQLConnection connection) {
            boardConnection[currentIdx] = connection;
        }

        String currentText() {
            return boardText[currentIdx];
        }

        void currentText(String text) {
            boardText[currentIdx] = text;
        }

        private SQLConnection findFirstConnection(boolean checkIsConnected) {
            for (int i = 0; i < boardConnection.length; i++) {
                SQLConnection conn = boardConnection[i];
                if (null != conn) {
                    if (checkIsConnected) {
                        if (conn.isConnected()) {
                            return conn;
                        }
                    } else {
                        return conn;
                    }
                }
            }
            return null;
        }
    }


    public CommandBoardManager(EventListener<CommandBoardManager, SQLExecutionRequest> eventListener) {
        this.eventListener = eventListener;
        textPane = GUIFactory.newTextComponent();
        textPane.addKeyListener(createKeyListener());
        boardData = new BoardData(NUM_BOARDS);
        JPanel bufferButtonsPanel = new JPanel(new GridLayout(1, NUM_BOARDS, 0, 5));
        boardHeaderButtons = new JButton[NUM_BOARDS];
        for (int i = 0; i < NUM_BOARDS; i++) {
            int boardIdx = i;
            JButton button = new JButton(BoardData.toKey(boardIdx));
            button.setBorder(boardIdx == boardData.currentIdx() ?
                    GUIFactory.COMMAND_BOARD_DISCONNECTED_BORDER :
                    GUIFactory.COMMAND_BOARD_UNSELECTED_BORDER);
            button.addActionListener(e -> onChangeBufferEvent(boardIdx));
            button.setFont(GUIFactory.COMMAND_BOARD_HEADER_FONT);
            boardHeaderButtons[boardIdx] = button;
            bufferButtonsPanel.add(button);
        }
        clearButton = new JButton("Clear");
        clearButton.addActionListener(this::onClearButtonEvent);
        runButton = new JButton("Run");
        runButton.addActionListener(this::onRunButtonEvent);
        runButton.setEnabled(false);
        runLineButton = new JButton("L.Run");
        runLineButton.addActionListener(this::onRunCurrentLineEvent);
        runLineButton.setEnabled(false);
        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this::onCancelButtonEvent);
        cancelButton.setEnabled(false);
        JPanel actionButtonsPanel = new JPanel(new GridLayout(1, 3, 0, 0));
        actionButtonsPanel.setBorder(BorderFactory.createEtchedBorder());
        actionButtonsPanel.add(clearButton);
        actionButtonsPanel.add(runButton);
        actionButtonsPanel.add(runLineButton);
        JPanel bufferActionButtonsPanel = new JPanel(new BorderLayout());
        bufferActionButtonsPanel.add(bufferButtonsPanel, BorderLayout.CENTER);
        bufferActionButtonsPanel.add(actionButtonsPanel, BorderLayout.EAST);
        titleBorder = BorderFactory.createTitledBorder(BORDER_TITLE);
        titleBorder.setTitleFont(GUIFactory.COMMAND_BOARD_HEADER_FONT);
        titleBorder.setTitleColor(GUIFactory.TABLE_HEADER_FONT_COLOR);
        setBorder(titleBorder);
        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        setLayout(new BorderLayout());
        add(bufferActionButtonsPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        setPreferredSize(GUIFactory.COMMAND_BOARD_MANAGER_HEIGHT);
    }

    public SQLConnection getSQLConnection() {
        return boardData.currentSQLConnection();
    }

    public void setSQLConnection(SQLConnection conn) {
        boardData.currentSQLConnection(conn);
        toggleComponents();
    }

    private String getFullTextContents() {
        try {
            int len = textPane.getStyledDocument().getLength();
            return textPane.getStyledDocument().getText(0, len);
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    private String getCommand() {
        String command = textPane.getSelectedText();
        if (null == command) {
            command = getFullTextContents();
        }
        return command.trim();
    }

    private String getCurrentLine() {
        try {
            int caretPos = textPane.getCaretPosition();
            int start = Utilities.getRowStart(textPane, caretPos);
            int end = Utilities.getRowEnd(textPane, caretPos) - 1;
            String command = textPane.getStyledDocument().getText(start, end - start + 1);
            return command.trim();
        } catch (BadLocationException e) {
            throw new RuntimeException(e);
        }
    }

    private void toggleComponents() {
        for (int i = 0; i < boardHeaderButtons.length; i++) {
            boardHeaderButtons[i].setBorder(GUIFactory.COMMAND_BOARD_UNSELECTED_BORDER);
        }
        SQLConnection conn = boardData.currentSQLConnection();
        if (null != conn) {
            titleBorder.setTitle(String.format(
                    Locale.ENGLISH,
                    "%s [%s]",
                    BORDER_TITLE,
                    conn.getKey()));
            validate();
            repaint();
        }
        boolean isConnected = null != conn && conn.isConnected();
        Border border = isConnected ?
                GUIFactory.COMMAND_BOARD_CONNECTED_BORDER :
                GUIFactory.COMMAND_BOARD_DISCONNECTED_BORDER;
        boardHeaderButtons[boardData.currentIdx()].setBorder(border);
        runButton.setEnabled(isConnected);
        cancelButton.setEnabled(isConnected);
    }

    private void onChangeBufferEvent(int newIdx) {
        boardData.currentText(getFullTextContents());
        boardData.currentIdx(newIdx);
        textPane.setText(boardData.currentText());
        toggleComponents();
        eventListener.onSourceEvent(
                this,
                EventType.BOARD_CHANGE,
                new SQLExecutionRequest(
                        boardData.currentKey(),
                        boardData.currentSQLConnection(),
                        boardData.currentText()));
    }

    private void onClearButtonEvent(ActionEvent event) {
        boardData.currentText("");
        textPane.setText("");
    }

    private void onCancelButtonEvent(ActionEvent event) {
        SQLConnection conn = boardData.currentSQLConnection();
        if (null == conn || false == conn.isConnected()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Not connected");
            return;
        }
        eventListener.onSourceEvent(
                this,
                EventType.COMMAND_CANCEL,
                new SQLExecutionRequest(boardData.currentKey(), conn, "CANCEL"));
    }

    private void onRunCurrentLineEvent(ActionEvent event) {
        fireCommandEvent(this::getCurrentLine);
    }

    private void onRunButtonEvent(ActionEvent event) {
        fireCommandEvent(this::getCommand);
    }

    private void fireCommandEvent(Supplier<String> commandSupplier) {
        SQLConnection conn = boardData.currentSQLConnection();
        if (null == conn || false == conn.isConnected()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Not connected");
            return;
        }
        String command = commandSupplier.get();
        if (null == command || command.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Command is empty");
            return;
        }
        eventListener.onSourceEvent(
                this,
                EventType.COMMAND_AVAILABLE,
                new SQLExecutionRequest(boardData.currentKey(), conn, command));
    }

    private KeyListener createKeyListener() {
        return new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.isControlDown()) {
                    int keyChar = e.getKeyChar();
                    switch (keyChar) {
                        case 13: /* ctrl^enter */
                        case 18: /* ctrl^r */
                            onRunButtonEvent(null);
                            break;

                        case 12: /* ctrl^l */
                            onRunCurrentLineEvent(null);
                            break;

                        //case 3: /* ctrl^c */
                        //case 4: /* ctrl^d */
                        //case 6: /* ctrl^f */
                        case 15: /* ctrl^o */
                            eventListener.onSourceEvent(
                                    CommandBoardManager.this,
                                    EventType.CONNECT_KEYBOARD_REQUEST,
                                    new SQLExecutionResponse(
                                            boardData.currentKey(),
                                            boardData.currentSQLConnection(),
                                            "ctrl^o"));
                            break;

                        default:
                            System.out.println("CHAR: " + keyChar);
                            // Ctrl + [1..NUM_BUFFERS]
                            int offset = keyChar - 49; // 0..NUM_BUFFERS-1
                            if (offset >= 0 && offset < NUM_BOARDS && offset != boardData.currentIdx()) {
                                onChangeBufferEvent(offset);
                            }
                    }
                }
            }

            @Override
            public void keyPressed(KeyEvent e) {
                // not interested
            }

            @Override
            public void keyReleased(KeyEvent e) {
                // not interested
            }
        };
    }
}
