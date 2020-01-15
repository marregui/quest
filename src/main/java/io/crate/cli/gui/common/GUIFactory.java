package io.crate.cli.gui.common;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;


public final class GUIFactory {

    static {
        System.setProperty("awt.useSystemAAFontSettings","on");
        System.setProperty("swing.aatext", "true");
    }

    public static final String LOGO_FILE_NAME = "/cratedb_logo.png";
    public static final int FRAME_WIDTH_AS_PERCENT_OF_SCREEN_WIDTH = 90;
    public static final int FRAME_HEIGHT_AS_PERCENT_OF_SCREEN_WIDTH = 90;
    public static final String MAIN_FONT_NAME = "monospaced";
    public static final Dimension SQL_CONNECTION_MANAGER_HEIGHT = new Dimension(0, 200);
    public static final Dimension COMMAND_BOARD_MANAGER_HEIGHT = new Dimension(0, 300);

    public static final Font ERROR_FONT = new Font(MAIN_FONT_NAME, Font.BOLD, 14);
    public static final Color ERROR_FONT_COLOR = new Color(189, 4, 4); // blood
    public static final String ERROR_HEADER = "======= Error =======\n";


    public static final Color TABLE_HEADER_FONT_COLOR = Color.BLACK;
    public static final Color TABLE_CELL_COLOR = new Color(66, 188, 245);
    public static final Color TABLE_FOOTER_COLOR = Color.BLACK;
    public static final Font TABLE_HEADER_FONT = new Font(MAIN_FONT_NAME, Font.BOLD, 18);
    public static final Font TABLE_CELL_FONT = new Font(MAIN_FONT_NAME, Font.PLAIN, 16);
    public static final Font TABLE_FOOTER_FONT = new Font(MAIN_FONT_NAME, Font.PLAIN, 14);

    public static final Color COMMAND_BOARD_FONT_COLOR = Color.WHITE;
    public static final Color COMMAND_BOARD_BACKGROUND_COLOR = Color.BLACK;
    public static final Color COMMAND_BOARD_CARET_COLOR = Color.GREEN;
    public static final Font COMMAND_BOARD_HEADER_FONT = new Font(MAIN_FONT_NAME, Font.BOLD, 16);
    public static final Font COMMAND_BOARD_BODY_FONT = new Font(MAIN_FONT_NAME, Font.BOLD, 18);

    public static final Border COMMAND_BOARD_CONNECTED_BORDER = BorderFactory.createLineBorder(TABLE_CELL_COLOR, 4, true);
    public static final Border COMMAND_BOARD_DISCONNECTED_BORDER = BorderFactory.createLineBorder(Color.BLACK, 2, true);
    public static final Border COMMAND_BOARD_UNSELECTED_BORDER = BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1, false);


    public static void addToSwingEventQueue(Runnable runnable) {
        if (EventQueue.isDispatchThread()) {
            runnable.run();
        } else {
            try {
                EventQueue.invokeLater(runnable);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

    public static JFrame newFrame(String title,
                                  int screenWidthPercent,
                                  int screenHeightPercent,
                                  JPanel mainPanel) {
        if (screenWidthPercent <= 0 || screenWidthPercent > 100) {
            throw new IllegalArgumentException("screenWidthPercent must be a value in [1, 100]");
        }
        if (screenHeightPercent <= 0 || screenHeightPercent > 100) {
            throw new IllegalArgumentException("screenHeightPercent must be a value in [1, 100]");
        }

        JFrame frame = new JFrame();
        frame.setTitle(title);
        frame.setType(Window.Type.NORMAL);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.add(mainPanel, BorderLayout.CENTER);
        ImageIcon logo = new ImageIcon(GUIFactory.class.getResource(LOGO_FILE_NAME));
        frame.setIconImage(logo.getImage());

        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        int width = (int) (screenSize.getWidth() * screenWidthPercent / 100);
        int height = (int) (screenSize.getHeight() * screenHeightPercent / 100);
        int x = (int) (screenSize.getWidth() - width) / 2;
        int y = (int) (screenSize.getHeight() - height) / 2;
        frame.setSize(width, height);
        frame.setLocation(x, y);
        return frame;
    }

    public static JTable newTable(TableModel tableModel, Runnable onListSelection) {
        JTable table = new JTable(tableModel);
        table.setAutoCreateRowSorter(true);
        table.setGridColor(TABLE_CELL_COLOR);
        table.setFont(TABLE_CELL_FONT);
        table.setDefaultRenderer(String.class, new StringCellRenderer(TABLE_CELL_FONT));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(e -> {
            if (false == e.getValueIsAdjusting()) {
                if (null != onListSelection) {
                    onListSelection.run();
                }
            }
        });
        JTableHeader header = table.getTableHeader();
        header.setFont(TABLE_HEADER_FONT);
        header.setForeground(TABLE_HEADER_FONT_COLOR);
        TableColumnModel columnModel = header.getColumnModel();
        columnModel.setColumnSelectionAllowed(false);
        return table;
    }

    public static  JTextPane newTextComponent() {
        JTextPane textPane = new JTextPane();
        textPane.setCaretPosition(0);
        textPane.setMargin(new Insets(5, 5, 5, 5));
        textPane.setFont(COMMAND_BOARD_BODY_FONT);
        textPane.setForeground(COMMAND_BOARD_FONT_COLOR);
        textPane.setBackground(COMMAND_BOARD_BACKGROUND_COLOR);
        textPane.setCaretColor(COMMAND_BOARD_CARET_COLOR);
        return textPane;
    }

    private GUIFactory() {
        throw new IllegalStateException("not meant to me instantiated");
    }
}
