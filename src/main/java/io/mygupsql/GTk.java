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

package io.mygupsql;

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
import java.awt.datatransfer.Clipboard;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.*;
import javax.swing.border.Border;


public final class GTk {

    private static final Toolkit TK = Toolkit.getDefaultToolkit();
    private static final Logger LOGGER = LoggerFactory.getLogger(GTk.class);

    public static final Color APP_THEME_COLOR = new Color(200, 50, 100);
    public static final String MAIN_FONT_NAME = "Arial"; // excluding commands' TextPane
    public static final Font MENU_FONT = new Font(MAIN_FONT_NAME, Font.BOLD, 14);

    public static final Font TABLE_HEADER_FONT = new Font(MAIN_FONT_NAME, Font.BOLD, 18);
    public static final Color TABLE_HEADER_FONT_COLOR = Color.BLACK;
    public static final Font TABLE_CELL_FONT = new Font(MAIN_FONT_NAME, Font.PLAIN, 16);
    public static final int CMD_DOWN_MASK = GTk.TK.getMenuShortcutKeyMaskEx();
    public static final int NO_KEY_EVENT = -1;

    static {
        // anti-aliased fonts
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
    }

    public static Clipboard systemClipboard() {
        return TK.getSystemClipboard();
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

    public static Dimension frameDimension() {
        Dimension screenSize = TK.getScreenSize();
        int width = (int) (screenSize.getWidth() * 0.9);
        int height = (int) (screenSize.getHeight() * 0.9);
        return new Dimension(width, height);
    }

    public static Dimension frameLocation(Dimension frameDimension) {
        Dimension screenSize = TK.getScreenSize();
        int x = (int) (screenSize.getWidth() - frameDimension.getWidth()) / 2;
        int y = (int) (screenSize.getHeight() - frameDimension.getHeight()) / 2;
        return new Dimension(x, y);
    }

    public static JFrame createFrame() {
        return createFrame(null);
    }

    public static JFrame createFrame(String title) {
        JFrame frame = new JFrame();
        frame.setType(Window.Type.NORMAL);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
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

    public static JButton createButton(String text, Icon icon, String tooltip, ActionListener listener) {
        return createButton(text, true, icon, tooltip, listener);
    }

    public static JButton createButton(String text, boolean isEnabled, Icon icon, String tooltip, ActionListener listener) {
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

    public static JPanel createEtchedFlowPanel(JComponent... components) {
        return createFlowPanel(BorderFactory.createEtchedBorder(), 0, 0, components);
    }

    public static JPanel createFlowPanel(JComponent... components) {
        return createFlowPanel(null, 0, 0, components);
    }

    public static JPanel createFlowPanel(int hgap, int vgap, JComponent... components) {
        return createFlowPanel(null, hgap, vgap, components);
    }

    public static JPanel createFlowPanel(Border border, int hgap, int vgap, JComponent... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, hgap, vgap));
        if (border != null) {
            panel.setBorder(border);
        }
        for (JComponent comp : components) {
            panel.add(comp);
        }
        return panel;
    }

    public static JPanel createHorizontalSpace(int hgap) {
        return new JPanel(new FlowLayout(FlowLayout.CENTER, hgap, 0));
    }

    public static JMenuItem configureMenuItem(JMenuItem item,
                                              GTk.Icon icon,
                                              String title,
                                              int keyEvent,
                                              ActionListener listener) {
        if (icon != GTk.Icon.NO_ICON) {
            item.setIcon(icon.icon());
        }
        item.setFont(MENU_FONT);
        item.setText(title);
        if (keyEvent != NO_KEY_EVENT) {
            item.setMnemonic(keyEvent);
            item.setAccelerator(KeyStroke.getKeyStroke(keyEvent, CMD_DOWN_MASK));
        }
        item.addActionListener(listener);
        return item;
    }

    public enum Icon {
        // https://p.yusukekamiyamane.com/
        // 16x16 icons
        NO_ICON(null),
        APPLICATION("Application.png"),
        CONN_UP("ConnectionUp.png"),
        CONN_DOWN("ConnectionDown.png"),
        CONN_ADD("ConnectionAdd.png"),
        CONN_ASSIGN("ConnectionAssign.png"),
        CONN_CLONE("ConnectionClone.png"),
        CONN_CONNECT("ConnectionConnect.png"),
        CONN_DISCONNECT("ConnectionDisconnect.png"),
        CONN_REMOVE("ConnectionRemove.png"),
        CONN_SHOW("ConnectionShow.png"),
        CONN_HIDE("ConnectionHide.png"),
        CONN_TEST("ConnectionTest.png"),
        EXEC("Exec.png"),
        EXEC_CANCEL("ExecCancel.png"),
        EXEC_LINE("ExecLine.png"),
        COMMAND_ADD("CommandAdd.png"),
        COMMAND_REMOVE("CommandRemove.png"),
        COMMAND_CLEAR("CommandClear.png"),
        COMMAND_SAVE("CommandSave.png"),
        COMMAND_STORE_BACKUP("CommandStoreBackup.png"),
        COMMAND_STORE_LOAD("CommandStoreLoad.png"),
        COMMAND_FIND("CommandFind.png"),
        COMMAND_REPLACE("CommandReplace.png"),
        NEXT("Next.png"),
        PREV("Prev.png"),
        RELOAD("Reload.png");

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
