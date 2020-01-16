package io.crate.cli.widgets;

import io.crate.cli.backend.SQLConnection;
import io.crate.cli.backend.SQLExecutionRequest;
import io.crate.cli.backend.SQLExecutionResponse;
import io.crate.cli.common.EventListener;
import io.crate.cli.common.EventSpeaker;
import io.crate.cli.common.GUIToolkit;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.text.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.Closeable;
import java.util.Locale;
import java.util.function.Supplier;


public class CommandBoardManager extends JPanel implements EventSpeaker<CommandBoardManager.EventType>, Closeable {

    public enum EventType {
        COMMAND_AVAILABLE,
        COMMAND_CANCEL,
        CONNECT_KEYBOARD_REQUEST,
        BOARD_CHANGE
    }


    private static final String BORDER_TITLE = "Command Board";


    private final JTextPane textPane;
    private final JButton clearButton;
    private final JButton runButton;
    private final JButton runLineButton;
    private final JButton cancelButton;
    private final JButton[] boardHeaderButtons;
    private final TitledBorder titleBorder;
    private final CommandBoardManagerData commandBoardManagerData;
    private final EventListener<CommandBoardManager, SQLExecutionRequest> eventListener;


    public CommandBoardManager(EventListener<CommandBoardManager, SQLExecutionRequest> eventListener) {
        this.eventListener = eventListener;
        commandBoardManagerData = new CommandBoardManagerData();
        textPane = GUIToolkit.newTextComponent();
        textPane.addKeyListener(createKeyListener());
        textPane.setText(commandBoardManagerData.getCurrentBoardContents());
        JPanel bufferButtonsPanel = new JPanel(new GridLayout(1, GUIToolkit.NUM_COMMAND_BOARDS, 0, 5));
        boardHeaderButtons = new JButton[GUIToolkit.NUM_COMMAND_BOARDS];
        for (int i = 0; i < GUIToolkit.NUM_COMMAND_BOARDS; i++) {
            int boardIdx = i;
            JButton button = new JButton(GUIToolkit.toCommandBoardKey(boardIdx));
            button.addActionListener(e -> onChangeBufferEvent(boardIdx));
            button.setFont(GUIToolkit.COMMAND_BOARD_HEADER_FONT);
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
        actionButtonsPanel.add(cancelButton);
        JPanel bufferActionButtonsPanel = new JPanel(new BorderLayout());
        bufferActionButtonsPanel.add(bufferButtonsPanel, BorderLayout.CENTER);
        bufferActionButtonsPanel.add(actionButtonsPanel, BorderLayout.EAST);
        titleBorder = BorderFactory.createTitledBorder(BORDER_TITLE);
        titleBorder.setTitleFont(GUIToolkit.COMMAND_BOARD_HEADER_FONT);
        titleBorder.setTitleColor(GUIToolkit.TABLE_HEADER_FONT_COLOR);
        setBorder(titleBorder);
        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        setLayout(new BorderLayout());
        add(bufferActionButtonsPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        setPreferredSize(GUIToolkit.COMMAND_BOARD_MANAGER_HEIGHT);
        toggleComponents();
    }

    public SQLConnection getSQLConnection() {
        return commandBoardManagerData.getCurrentSQLConnection();
    }

    public void setSQLConnection(SQLConnection conn) {
        commandBoardManagerData.setCurrentSQLConnection(conn);
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
            boardHeaderButtons[i].setBorder(GUIToolkit.COMMAND_BOARD_UNSELECTED_BORDER);
        }
        SQLConnection conn = commandBoardManagerData.getCurrentSQLConnection();
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
                GUIToolkit.COMMAND_BOARD_CONNECTED_BORDER :
                GUIToolkit.COMMAND_BOARD_DISCONNECTED_BORDER;
        boardHeaderButtons[commandBoardManagerData.getCurrentIdx()].setBorder(border);
        runButton.setEnabled(isConnected);
        runLineButton.setEnabled(isConnected);
        cancelButton.setEnabled(isConnected);
    }

    public void store() {
        commandBoardManagerData.setCurrentBoardContents(getFullTextContents());
        commandBoardManagerData.store();
    }

    @Override
    public void close() {
        store();
    }

    private void onChangeBufferEvent(int newIdx) {
        commandBoardManagerData.setCurrentBoardContents(getFullTextContents());
        commandBoardManagerData.setCurrentIdx(newIdx);
        textPane.setText(commandBoardManagerData.getCurrentBoardContents());
        toggleComponents();
        eventListener.onSourceEvent(
                this,
                EventType.BOARD_CHANGE,
                new SQLExecutionRequest(
                        commandBoardManagerData.getCurrentKey(),
                        commandBoardManagerData.getCurrentSQLConnection(),
                        commandBoardManagerData.getCurrentBoardContents()));
    }

    private void onClearButtonEvent(ActionEvent event) {
        commandBoardManagerData.setCurrentBoardContents("");
        textPane.setText("");
    }

    private void onCancelButtonEvent(ActionEvent event) {
        SQLConnection conn = commandBoardManagerData.getCurrentSQLConnection();
        if (null == conn || false == conn.isConnected()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Not connected");
            return;
        }
        eventListener.onSourceEvent(
                this,
                EventType.COMMAND_CANCEL,
                new SQLExecutionRequest(commandBoardManagerData.getCurrentKey(), conn, "CANCEL"));
    }

    private void onRunCurrentLineEvent(ActionEvent event) {
        fireCommandEvent(this::getCurrentLine);
    }

    private void onRunButtonEvent(ActionEvent event) {
        fireCommandEvent(this::getCommand);
    }

    private void fireCommandEvent(Supplier<String> commandSupplier) {
        SQLConnection conn = commandBoardManagerData.getCurrentSQLConnection();
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
                new SQLExecutionRequest(commandBoardManagerData.getCurrentKey(), conn, command));
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
                                            commandBoardManagerData.getCurrentKey(),
                                            commandBoardManagerData.getCurrentSQLConnection(),
                                            "ctrl^o"));
                            break;

                        default:
                            // Ctrl + [1..NUM_BUFFERS]
                            int offset = keyChar - 49; // 0..NUM_BUFFERS-1
                            if (offset >= 0 && offset < GUIToolkit.NUM_COMMAND_BOARDS && offset != commandBoardManagerData.getCurrentIdx()) {
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
