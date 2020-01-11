package io.crate.cli.gui.widgets;

import io.crate.cli.connections.SQLConnection;
import io.crate.cli.connections.SQLExecutionRequest;
import io.crate.cli.gui.common.EventListener;
import io.crate.cli.gui.common.EventSpeaker;
import io.crate.cli.gui.common.GUIFactory;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Arrays;
import java.util.Locale;


public class CommandManager extends JPanel implements EventSpeaker<CommandManager.EventType> {

    public enum EventType {
        COMMAND_AVAILABLE,
        BUFFER_CHANGE
    }

    private static final Border CONNECTED_BUFFER_BORDER = BorderFactory.createLineBorder(Color.GREEN, 4, true);
    private static final Border DISCONNECTED_BUFFER_BORDER = BorderFactory.createLineBorder(Color.BLACK, 2, true);
    private static final Border UNSELECTED_BUFFER_BORDER = BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1, false);
    private static final int NUM_BUFFERS = 8;


    private final JTextPane textPane;
    private final JButton clearButton;
    private final JButton runButton;
    private final JButton[] bufferButtons;
    private final TitledBorder titleBorder;
    private final BufferData bufferData;
    private final EventListener<CommandManager, SQLExecutionRequest> eventListener;


    private static class BufferData {

        private static String toKey(int offset) {
            return String.valueOf((char) ('A' + offset));
        }

        private final String[] buffers;
        private final SQLConnection[] connections;
        private int currentIdx;
        private


        BufferData(int size) {
            buffers = new String[size];
            connections = new SQLConnection[size];
            Arrays.fill(buffers, "");
            Arrays.fill(connections, null);
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
            SQLConnection conn = connections[currentIdx];
            if (null == conn) {
                conn = findFirstConnection(true);
                if (null == conn) {
                    conn = findFirstConnection(false);
                }
                connections[currentIdx] = conn;
            }
            return conn;
        }

        void currentSQLConnection(SQLConnection connection) {
            connections[currentIdx] = connection;
        }

        String currentText() {
            return buffers[currentIdx];
        }

        void currentText(String text) {
            buffers[currentIdx] = text;
        }

        private SQLConnection findFirstConnection(boolean checkIsConnected) {
            for (int i = 0; i < connections.length; i++) {
                SQLConnection connection = connections[i];
                if (null != connection) {
                    if (checkIsConnected) {
                        if (connection.isConnected()) {
                            return connection;
                        }
                    } else {
                        return connection;
                    }
                }
            }
            return null;
        }
    }


    public CommandManager(EventListener<CommandManager, SQLExecutionRequest> eventListener) {
        this.eventListener = eventListener;
        textPane = GUIFactory.newTextComponent();
        textPane.addKeyListener(createKeyListener());
        bufferData = new BufferData(NUM_BUFFERS);
        JPanel bufferButtonsPanel = new JPanel(new GridLayout(1, NUM_BUFFERS, 0, 5));
        bufferButtons = new JButton[NUM_BUFFERS];
        for (int i = 0; i < NUM_BUFFERS; i++) {
            int offset = i;
            JButton button = new JButton();
            button.setText(BufferData.toKey(offset));
            button.setBorder(offset == bufferData.currentIdx() ? DISCONNECTED_BUFFER_BORDER : UNSELECTED_BUFFER_BORDER);
            button.addActionListener(e -> onChangeBufferEvent(offset));
            bufferButtons[offset] = button;
            bufferButtonsPanel.add(button);
        }
        clearButton = new JButton("Clear");
        clearButton.addActionListener(this::onClearButtonEvent);
        runButton = new JButton("Run");
        runButton.addActionListener(this::onRunButtonEvent);
        runButton.setEnabled(false);
        JPanel actionButtonsPanel = new JPanel(new GridLayout(1, 2, 0, 0));
        actionButtonsPanel.add(clearButton);
        actionButtonsPanel.add(runButton);
        JPanel bufferActionButtonsPanel = new JPanel(new BorderLayout());
        bufferActionButtonsPanel.add(bufferButtonsPanel, BorderLayout.CENTER);
        bufferActionButtonsPanel.add(actionButtonsPanel, BorderLayout.EAST);
        titleBorder = BorderFactory.createTitledBorder("Command Buffer");
        setBorder(titleBorder);
        JScrollPane scrollPane = new JScrollPane(textPane);
        scrollPane.setPreferredSize(new Dimension(200, 200));
        setLayout(new BorderLayout());
        add(bufferActionButtonsPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }

    public SQLConnection getSQLConnection() {
        return bufferData.currentSQLConnection();
    }

    public void setSQLConnection(SQLConnection conn) {
        bufferData.currentSQLConnection(conn);
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

    private void toggleComponents() {
        for (int i = 0; i < bufferButtons.length; i++) {
            bufferButtons[i].setBorder(UNSELECTED_BUFFER_BORDER);
        }
        SQLConnection conn = bufferData.currentSQLConnection();
        if (null != conn) {
            titleBorder.setTitle(String.format(
                    Locale.ENGLISH,
                    "Command Buffer on [%s]",
                    conn.getKey()));
            validate();
            repaint();
        }
        boolean isConnected = null != conn && conn.isConnected();
        Border border = isConnected ? CONNECTED_BUFFER_BORDER : DISCONNECTED_BUFFER_BORDER;
        bufferButtons[bufferData.currentIdx()].setBorder(border);
        runButton.setEnabled(isConnected);
    }

    private void onChangeBufferEvent(int offset) {
        String command = getFullTextContents();
        bufferData.currentText(command);
        bufferData.currentIdx(offset);
        textPane.setText(command);
        toggleComponents();
        eventListener.onSourceEvent(
                this,
                EventType.BUFFER_CHANGE,
                new SQLExecutionRequest(
                        bufferData.currentKey(),
                        bufferData.currentSQLConnection(),
                        bufferData.currentText()));
    }

    private void onClearButtonEvent(ActionEvent event) {
        bufferData.currentText("");
        textPane.setText("");
    }

    private void onRunButtonEvent(ActionEvent event) {
        SQLConnection conn = bufferData.currentSQLConnection();
        if (null == conn || false == conn.isConnected()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Not connected");
            return;
        }
        String command = getCommand();
        if (command.isEmpty()) {
            JOptionPane.showMessageDialog(
                    this,
                    "Command is empty");
            return;
        }
        eventListener.onSourceEvent(
                this,
                EventType.COMMAND_AVAILABLE,
                new SQLExecutionRequest(bufferData.currentKey(), conn, command));
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

                        //case 3: /* ctrl^c */
                        //case 4: /* ctrl^d */
                        //case 6: /* ctrl^f */
                        //case 12: /* ctrl^l */
                        //case 15: /* ctrl^o */

                        default:
                            System.out.println("CHAR: " + keyChar);
                            // Ctrl + [1..NUM_BUFFERS]
                            int offset = keyChar - 49; // 0..NUM_BUFFERS-1
                            if (offset >= 0 && offset < NUM_BUFFERS && offset != bufferData.currentIdx()) {
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
