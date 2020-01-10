package io.crate.cli.gui.common;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.net.URL;


public final class GUIFactory {

    public static final Font TABLE_CELL_FONT = new Font("monospaced", Font.PLAIN, 16);
    public static final Font TABLE_HEADER_FONT = new Font("monospaced", Font.BOLD, 18);
    public static final Color TABLE_HEADER_FONT_COLOR = Color.BLACK;
    public static final Color TABLE_GRID_COLOR = new Color(66, 188, 245);
    public static final String LOGO_FILE_NAME = "/cratedb_logo.png";
    private static final Color TEXT_PANE_FONT_COLOR = Color.WHITE;
    private static final Color TEXT_PANE_BACKGROUND_COLOR = Color.BLACK;
    private static final Color TEXT_PANE_CARET_COLOR = Color.GREEN;
    private static final Font TEXT_PANE_FONT = new Font("monospaced", Font.BOLD, 18);


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
        table.setGridColor(TABLE_GRID_COLOR);
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
        textPane.setFont(TEXT_PANE_FONT);
        textPane.setForeground(TEXT_PANE_FONT_COLOR);
        textPane.setBackground(TEXT_PANE_BACKGROUND_COLOR);
        textPane.setCaretColor(TEXT_PANE_CARET_COLOR);
        return textPane;
    }

    private GUIFactory() {
        throw new IllegalStateException("not meant to me instantiated");
    }
}
