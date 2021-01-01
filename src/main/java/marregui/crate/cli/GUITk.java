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

package marregui.crate.cli;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionListener;
import java.util.Objects;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.Border;


/**
 * Definitions.
 */
public final class GUITk {

    static {
        // anti-aliased fonts
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
    }

    public static final Color APP_THEME_COLOR = new Color(66, 188, 245);
    public static final String MAIN_FONT_NAME = "Arial";
    public static final Font TABLE_HEADER_FONT = new Font(MAIN_FONT_NAME, Font.BOLD, 18);
    public static final Color TABLE_HEADER_FONT_COLOR = Color.BLACK;
    public static final Font TABLE_CELL_FONT = new Font(MAIN_FONT_NAME, Font.PLAIN, 16);

    /**
     * @return dimension instance where width/height are 90% of the screen's
     *         width/height
     */
    public static Dimension frameDimension() {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        int width = (int) (screenSize.getWidth() * 0.9);
        int height = (int) (screenSize.getHeight() * 0.9);
        return new Dimension(width, height);
    }

    /**
     * @param frameDimension as produced by {@link #frameDimension()}
     * @return dimension representing the location for the frame to be screen
     *         centred
     */
    public static Dimension frameLocation(Dimension frameDimension) {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        int x = (int) (screenSize.getWidth() - frameDimension.getWidth()) / 2;
        int y = (int) (screenSize.getHeight() - frameDimension.getHeight()) / 2;
        return new Dimension(x, y);
    }

    /**
     * Creates a top level frame.
     * 
     * @return the frame
     */
    static JFrame createFrame() {
        return createFrame(null);
    }

    /**
     * Creates a top level frame.
     * 
     * @param title title for the frame
     * @return the frame
     */
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

    /**
     * Creates a simple button.
     * 
     * @param text      text to display on the button
     * @param isEnabled whether the button is enabled
     * @param listener  listener/action on button events
     * @return
     */
    public static JButton createButton(String text, boolean isEnabled, ActionListener listener) {
        JButton button = new JButton(Objects.requireNonNull(text));
        button.addActionListener(Objects.requireNonNull(listener));
        button.setEnabled(isEnabled);
        return button;
    }

    /**
     * Creates a simple enabled button.
     * 
     * @param text     text to display on the button
     * @param listener listener/action on button events
     * @return
     */
    public static JButton createButton(String text, ActionListener listener) {
        return createButton(text, true, listener);
    }

    /**
     * Creates a panel with a right aligned flow layout and adds the components. The
     * panel features an "etched" border.
     * 
     * @param components to be added to the panel
     * @return the panel, containing the components
     */
    public static JPanel createEtchedFlowPanel(JComponent... components) {
        return createFlowPanel(BorderFactory.createEtchedBorder(), components);
    }

    /**
     * Creates a panel with a right aligned flow layout and adds the components.
     * 
     * @param components to be added to the panel
     * @return the panel, containing the components
     */
    public static JPanel createFlowPanel(JComponent... components) {
        return createFlowPanel(null, components);
    }

    private static JPanel createFlowPanel(Border border, JComponent... components) {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        if (border != null) {
            panel.setBorder(border);
        }
        for (JComponent comp : components) {
            panel.add(comp);
        }
        return panel;
    }

    /**
     * Ensures the tasks are run by the AWT EventQueue event processing thread.
     * 
     * @param tasks tasks to be run. If the caller is not the GUI main processing
     *              queue, these are added to the {@link java.awt.EventQueue} and
     *              run later, otherwise they are run on the spot.
     */
    public static void invokeLater(Runnable... tasks) {
        if (EventQueue.isDispatchThread()) {
            for (Runnable r : tasks) {
                if (r != null) {
                    r.run();
                }
            }
        }
        else {
            try {
                EventQueue.invokeLater(() -> {
                    for (Runnable r : tasks) {
                        if (r != null) {
                            r.run();
                        }
                    }
                });
            }
            catch (Throwable fail) {
                throw new RuntimeException(fail);
            }
        }
    }

    private GUITk() {
        throw new IllegalStateException("not meant to be instantiated");
    }
}
