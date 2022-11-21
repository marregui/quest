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

package io.quest.frontend;

import io.quest.model.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import javax.swing.*;
import javax.swing.border.Border;

public final class GTk {

    static {
        // anti-aliased fonts
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
    }

    // https://patorjk.com/software/taag/#p=display&h=0&f=Ivrit&t=quest
    public static final String BANNER = "\n" +
            ".                              _   \n" +
            "   __ _   _   _    ___   ___  | |_ \n" +
            "  / _` | | | | |  / _ \\ / __| | __|\n" +
            " | (_| | | |_| | |  __/ \\__ \\ | |_ \n" +
            "  \\__, |  \\__,_|  \\___| |___/  \\__|\n" +
            "     |_|\n" +
            "  Copyright (c) 2019 - " + Calendar.getInstance().get(Calendar.YEAR) + "\n";
    public static final String MAIN_FONT_NAME = "Arial"; // excluding commands' TextPane, which is Monospaced
    public static final Color APP_THEME_COLOR = new Color(200, 50, 90);
    public static final Color TABLE_HEADER_FONT_COLOR = Color.BLACK;
    public static final Font TABLE_HEADER_FONT = new Font(MAIN_FONT_NAME, Font.BOLD, 18);
    public static final Font TABLE_CELL_FONT = new Font(MAIN_FONT_NAME, Font.PLAIN, 17);
    public static final Font MENU_FONT = new Font(MAIN_FONT_NAME, Font.BOLD, 14);
    public static final int CMD_DOWN_MASK = InputEvent.META_DOWN_MASK;
    public static final int CMD_SHIFT_DOWN_MASK = CMD_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;
    public static final int ALT_DOWN_MASK = InputEvent.ALT_DOWN_MASK;
    public static final int ALT_SHIFT_DOWN_MASK = ALT_DOWN_MASK | InputEvent.SHIFT_DOWN_MASK;
    public static final int NO_KEY_EVENT = -1;

    private static final String QUESTDB_DOCUMENTATION_URL = "https://questdb.io/docs/introduction/";
    private static final Logger LOGGER = LoggerFactory.getLogger(GTk.class);
    private static final Toolkit TK = Toolkit.getDefaultToolkit();
    private static final DataFlavor[] SUPPORTED_COPY_PASTE_FLAVOR = {DataFlavor.stringFlavor};


    public static void addCmdKeyAction(int keyEvent, JComponent component, ActionListener action) {
        Action cmd = createAction(action);
        component.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(keyEvent, CMD_DOWN_MASK),
                cmd
        );
        component.getActionMap().put(cmd, cmd);
    }

    public static void addCmdShiftKeyAction(int keyEvent, JComponent component, ActionListener action) {
        Action cmd = createAction(action);
        component.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(keyEvent, CMD_SHIFT_DOWN_MASK),
                cmd
        );
        component.getActionMap().put(cmd, cmd);
    }

    public static void addAltKeyAction(int keyEvent, JComponent component, ActionListener action) {
        Action cmd = createAction(action);
        component.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(keyEvent, ALT_DOWN_MASK),
                cmd
        );
        component.getActionMap().put(cmd, cmd);
    }

    public static void addAltShiftKeyAction(int keyEvent, JComponent component, ActionListener action) {
        Action cmd = createAction(action);
        component.getInputMap(JComponent.WHEN_FOCUSED).put(
                KeyStroke.getKeyStroke(keyEvent, ALT_SHIFT_DOWN_MASK),
                cmd
        );
        component.getActionMap().put(cmd, cmd);
    }

    private static Action createAction(ActionListener action) {
        return new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.actionPerformed(e);
            }
        };
    }

    public static void setupTableCmdKeyActions(JTable table) {
        addCmdKeyAction(KeyEvent.VK_A, table, e -> table.selectAll()); // cmd-a, select all
        final StringBuilder sb = new StringBuilder();
        addCmdKeyAction(KeyEvent.VK_C, table, e -> { // cmd-c, copy selection/all to clipboard
            int[] selectedRows = table.getSelectedRows();
            int[] selectedCols = table.getSelectedColumns();
            if (selectedRows.length <= 0) {
                table.selectAll();
                selectedRows = table.getSelectedRows();
            }
            int[] widths = new int[selectedCols.length];
            for (int c = 0; c < selectedCols.length; c++) {
                for (int r = 0; r < selectedRows.length; r++) {
                    int len = table.getValueAt(r, c).toString().length();
                    if (widths[c] < len) {
                        widths[c] = len;
                    }
                }
            }
            sb.setLength(0);
            int rowIdx;
            int colIdx;
            for (int r = 0; r < selectedRows.length; r++) {
                rowIdx = selectedRows[r];
                for (int c = 0; c < selectedCols.length; c++) {
                    colIdx = selectedCols[c];
                    if (!table.getColumnName(colIdx).equals(Table.ROWID_COL_NAME)) {
                        String value = table.getValueAt(rowIdx, colIdx).toString();
                        int len = value.length();
                        sb.append(value);
                        for (int i = 0; i < widths[c] - len; i++) {
                            sb.append(" ");
                        }
                        sb.append(", ");
                    }
                }
                sb.setLength(sb.length() - 2);
                sb.append("\n");
            }
            if (sb.length() > 0) {
                sb.setLength(sb.length() - 1); // last \n
                setClipboardContent(sb.toString());
            }
        });
    }

    public static void invokeLater(Runnable... tasks) {
        if (EventQueue.isDispatchThread()) {
            for (Runnable r : tasks) {
                if (r != null) {
                    r.run();
                }
            }
        } else {
            try {
                EventQueue.invokeLater(() -> {
                    for (Runnable r : tasks) {
                        if (r != null) {
                            r.run();
                        }
                    }
                });
            } catch (Throwable fail) {
                throw new RuntimeException(fail);
            }
        }
    }

    public static void setClipboardContent(final String str) {
        TK.getSystemClipboard().setContents(
                new Transferable() {
                    @Override
                    public DataFlavor[] getTransferDataFlavors() {
                        return SUPPORTED_COPY_PASTE_FLAVOR;
                    }

                    @Override
                    public boolean isDataFlavorSupported(DataFlavor flavor) {
                        return DataFlavor.stringFlavor.equals(flavor);
                    }

                    @Override
                    public Object getTransferData(DataFlavor flavor) {
                        return isDataFlavorSupported(flavor) ? str : "";
                    }
                },
                null
        );
    }

    public static String getClipboardContent() {
        try {
            return (String) TK.getSystemClipboard().getData(DataFlavor.stringFlavor);
        } catch (IOException | UnsupportedFlavorException err) {
            return "";
        }
    }

    public static Dimension frameDimension() {
        return frameDimension(0.9F, 0.9F);
    }

    public static Dimension frameDimension(float xScale, float yScale) {
        assert xScale > 0.5 && xScale < 1.0; // 50..99% percent of screen
        assert yScale > 0.5 && yScale < 1.0;
        Dimension screenSize = TK.getScreenSize();
        int width = (int) (screenSize.getWidth() * xScale);
        int height = (int) (screenSize.getHeight() * yScale);
        return new Dimension(width, height);
    }

    public static Dimension frameLocation(Dimension frameDimension) {
        Dimension screenSize = TK.getScreenSize();
        int x = (int) (screenSize.getWidth() - frameDimension.getWidth()) / 2;
        int y = (int) (screenSize.getHeight() - frameDimension.getHeight()) / 2;
        return new Dimension(x, y);
    }

    public static JFrame createFrame(String title, Runnable onExit) {
        JFrame frame = new JFrame() {
            @Override
            public void dispose() {
                if (onExit != null) {
                    onExit.run();
                }
                super.dispose();
                System.exit(0);
            }
        };
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.setType(Window.Type.NORMAL);
        if (title != null && !title.isEmpty()) {
            frame.setTitle(title);
        }
        Dimension dimension = frameDimension();
        Dimension location = frameLocation(dimension);
        frame.setSize(dimension.width, dimension.height);
        frame.setLocation(location.width, location.height);
        frame.setLayout(new BorderLayout());
        return frame;
    }

    public static JButton button(String text, Icon icon, String tooltip, ActionListener listener) {
        return button(text, true, icon, tooltip, listener);
    }

    public static JButton button(
            String text,
            boolean isEnabled,
            Icon icon,
            String tooltip,
            ActionListener listener
    ) {
        JButton button = new JButton(Objects.requireNonNull(text));
        if (icon != Icon.NO_ICON) {
            button.setIcon(icon.icon());
        }
        if (tooltip != null && !tooltip.isBlank()) {
            button.setToolTipText(tooltip);
        }
        button.addActionListener(Objects.requireNonNull(listener));
        button.setEnabled(isEnabled);
        return button;
    }

    public static JPanel etchedFlowPanel(JComponent... components) {
        return flowPanel(BorderFactory.createEtchedBorder(), 0, 0, components);
    }

    public static JPanel flowPanel(JComponent... components) {
        return flowPanel(null, 0, 0, components);
    }

    public static JPanel flowPanel(Border border, int hgap, int vgap, JComponent... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, hgap, vgap));
        if (border != null) {
            panel.setBorder(border);
        }
        for (JComponent comp : components) {
            panel.add(comp);
        }
        return panel;
    }

    public static JPanel horizontalSpace(int hgap) {
        return new JPanel(new FlowLayout(FlowLayout.CENTER, hgap, 0));
    }

    public static JMenuItem configureMenuItem(
            JMenuItem item,
            GTk.Icon icon,
            String title,
            int keyEvent,
            ActionListener listener
    ) {
        return configureMenuItem(item, icon, title, null, keyEvent, listener);
    }

    public static JMenuItem configureMenuItem(
            JMenuItem item,
            GTk.Icon icon,
            String title,
            String tooltip,
            int keyEvent,
            ActionListener listener
    ) {
        if (icon != GTk.Icon.NO_ICON) {
            item.setIcon(icon.icon());
        }
        item.setFont(MENU_FONT);
        item.setText(title);
        if (tooltip != null) {
            item.setToolTipText(tooltip);
        }
        if (keyEvent != NO_KEY_EVENT) {
            item.setMnemonic(keyEvent);
            item.setAccelerator(KeyStroke.getKeyStroke(keyEvent, CMD_DOWN_MASK));
        }
        item.addActionListener(listener);
        return item;
    }

    public static void openQuestDBDocumentation(ActionEvent ignore) {
        Runtime rt = Runtime.getRuntime();
        String os = System.getProperty("os.name").toLowerCase();
        try {
            if (os.indexOf("mac") >= 0) {
                rt.exec(String.format(
                        "open %s",
                        QUESTDB_DOCUMENTATION_URL
                ));
            } else if (os.indexOf("win") >= 0) {
                rt.exec(String.format(
                        "rundll32 url.dll,FileProtocolHandler %s",
                        QUESTDB_DOCUMENTATION_URL
                ));
            } else if (os.indexOf("nix") >= 0 || os.indexOf("nux") >= 0) {
                String[] browsers = {
                        "google-chrome", "firefox", "mozilla",
                        "epiphany", "konqueror", "netscape",
                        "opera", "links", "lynx"
                };
                StringBuilder cmd = new StringBuilder();
                for (int i = 0; i < browsers.length; i++) {
                    if (i != 0) {
                        cmd.append(" || ");
                    }
                    cmd.append(browsers[i])
                            .append("\"")
                            .append(QUESTDB_DOCUMENTATION_URL)
                            .append("\"");
                }
                // If the first didn't work, try the next
                rt.exec(new String[]{"sh", "-c", cmd.toString()});
            }
        } catch (IOException err) {
            JOptionPane.showMessageDialog(
                    null,
                    String.format(
                            "Failed to open browser [%s:%s]: %s",
                            os,
                            QUESTDB_DOCUMENTATION_URL,
                            err.getMessage()
                    ),
                    "Helpless",
                    JOptionPane.INFORMATION_MESSAGE
            );
        }
    }

    public enum Icon {
        // https://p.yusukekamiyamane.com/
        // 16x16 icons
        NO_ICON(null),
        APPLICATION("Application.png"),
        HELP("Help.png"),
        CONNS("Conns.png"),
        CONN_UP("ConnUp.png"),
        CONN_DOWN("ConnDown.png"),
        CONN_ADD("ConnAdd.png"),
        CONN_ASSIGN("ConnAssign.png"),
        CONN_CLONE("ConnClone.png"),
        CONN_CONNECT("ConnConnect.png"),
        CONN_DISCONNECT("ConnDisconnect.png"),
        CONN_REMOVE("ConnRemove.png"),
        CONN_SHOW("ConnShow.png"),
        CONN_HIDE("ConnHide.png"),
        CONN_TEST("ConnTest.png"),
        COMMANDS("Commands.png"),
        COMMAND_QUEST("CommandQuestDB.png"),
        COMMAND_ADD("CommandAdd.png"),
        COMMAND_REMOVE("CommandRemove.png"),
        COMMAND_EDIT("CommandEdit.png"),
        COMMAND_CLEAR("CommandClear.png"),
        COMMAND_SAVE("CommandSave.png"),
        COMMAND_RELOAD("CommandReload.png"),
        COMMAND_STORE_BACKUP("CommandStoreBackup.png"),
        COMMAND_STORE_LOAD("CommandStoreLoad.png"),
        COMMAND_FIND("CommandFind.png"),
        COMMAND_REPLACE("CommandReplace.png"),
        COMMAND_EXEC("CommandExec.png"),
        COMMAND_EXEC_ABORT("CommandExecAbort.png"),
        COMMAND_EXEC_LINE("CommandExecLine.png"),
        META("Meta.png"),
        PLOT_CHANGE_RANGES("PlotChangeRanges.png"),
        PLOT_RESTORE_RANGES("PlotRestoreRanges.png"),
        RESULTS("Results.png"),
        RESULTS_NEXT("ResultsNext.png"),
        RESULTS_PREV("ResultsPrev.png");

        private static final String FOLDER = "images";
        private static final Map<String, ImageIcon> ICON_MAP = new HashMap<>();

        private final String iconName;

        Icon(String iconName) {
            this.iconName = iconName;
        }

        public ImageIcon icon() {
            if (this == NO_ICON) {
                throw new UnsupportedOperationException();
            }
            ImageIcon icon = ICON_MAP.get(iconName);
            try {
                if (icon == null) {
                    URL url = GTk.class.getResource("/" + FOLDER + "/" + iconName);
                    ICON_MAP.put(iconName, icon = new ImageIcon(TK.getImage(url)));
                }
            } catch (Throwable err) {
                LOGGER.error("Icon not available: [/{}/{}] -> {}", FOLDER, iconName, err.getMessage());
            }
            return icon;
        }
    }

    private GTk() {
        throw new IllegalStateException("not meant to be instantiated");
    }
}
