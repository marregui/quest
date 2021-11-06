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

package io.quest.common;

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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.text.AttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;


public final class GTk {

    // https://patorjk.com/software/taag/#p=display&h=0&f=Ivrit&t=quest
    public static final String BANNER = "\n" +
            ".                              _   \n" +
            "   __ _   _   _    ___   ___  | |_ \n" +
            "  / _` | | | | |  / _ \\ / __| | __|\n" +
            " | (_| | | |_| | |  __/ \\__ \\ | |_ \n" +
            "  \\__, |  \\__,_|  \\___| |___/  \\__|\n" +
            "     |_|";

    private static final Toolkit TK = Toolkit.getDefaultToolkit();
    private static final Logger LOGGER = LoggerFactory.getLogger(GTk.class);

    public static final Color APP_THEME_COLOR = new Color(200, 50, 100);
    public static final Color TABLE_HEADER_FONT_COLOR = Color.BLACK;
    public final static AttributeSet HIGHLIGHT_NORMAL = GTk.styleForegroundColor(255, 245, 222);
    public final static AttributeSet HIGHLIGHT_ERROR = GTk.styleForegroundColor(225, 125, 5);
    public final static AttributeSet HIGHLIGHT_KEYWORD = GTk.styleForegroundColor(200, 50, 100);
    public final static AttributeSet HIGHLIGHT_TYPE = GTk.styleForegroundColor(240, 10, 140);
    public final static AttributeSet HIGHLIGHT_MATCH = GTk.styleForegroundColor(50, 200, 185);

    public static final String MAIN_FONT_NAME = "Arial"; // excluding commands' TextPane
    public static final Font MENU_FONT = new Font(MAIN_FONT_NAME, Font.BOLD, 14);
    public static final Font TABLE_HEADER_FONT = new Font(MAIN_FONT_NAME, Font.BOLD, 18);
    public static final Font TABLE_CELL_FONT = new Font(MAIN_FONT_NAME, Font.PLAIN, 16);

    public static final int CMD_DOWN_MASK = InputEvent.META_DOWN_MASK | InputEvent.META_MASK;
    public static final int NO_KEY_EVENT = -1;

    static {
        // anti-aliased fonts
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
    }

    public static AttributeSet styleForegroundColor(int r, int g, int b) {
        StyleContext sc = StyleContext.getDefaultStyleContext();
        return sc.addAttribute(sc.getEmptySet(), StyleConstants.Foreground, new Color(r, g, b));
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

    public static void addCmdKeyAction(int keyEvent,
                                       JComponent component,
                                       ActionListener action) {
        Action cmd = new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                action.actionPerformed(e);
            }
        };
        component.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(keyEvent, CMD_DOWN_MASK), cmd);
        component.getActionMap().put(cmd, cmd);
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

    public static JButton button(String text, Icon icon, String tooltip, ActionListener listener) {
        return button(text, true, icon, tooltip, listener);
    }

    public static JButton button(String text, boolean isEnabled, Icon icon, String tooltip, ActionListener listener) {
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

    public static JPanel flowPanel(int hgap, int vgap, JComponent... components) {
        return flowPanel(null, hgap, vgap, components);
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

    public static JMenuItem configureMenuItem(JMenuItem item,
                                              GTk.Icon icon,
                                              String title,
                                              int keyEvent,
                                              ActionListener listener) {
        return configureMenuItem(item, icon, title, null, keyEvent, listener);
    }

    public static JMenuItem configureMenuItem(JMenuItem item,
                                              GTk.Icon icon,
                                              String title,
                                              String tooltip,
                                              int keyEvent,
                                              ActionListener listener) {
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
        COMMAND_QUEST("CommandQuestDB.png"),
        COMMAND_ADD("CommandAdd.png"),
        COMMAND_REMOVE("CommandRemove.png"),
        COMMAND_EDIT("CommandEdit.png"),
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
