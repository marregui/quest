package io.crate.cli.common;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;


public final class GUIToolkit {

    public static final String JDBC_DRIVER_URL_FORMAT = "jdbc:crate://%s:%s/";

    public static final String MAIN_FONT_NAME = "monospaced";
    public static final Color CRATE_COLOR = new Color(66, 188, 245);
    public static final Font REMARK_FONT = new Font(MAIN_FONT_NAME, Font.BOLD, 16);
    public static final Dimension SQL_CONNECTION_MANAGER_HEIGHT = new Dimension(0, 200);
    public static final Dimension COMMAND_BOARD_MANAGER_HEIGHT = new Dimension(0, 300);
    public static final String COMMAND_BOARD_MANAGER_STORE = "command_board.json";
    public static final String SQL_CONNECTION_MANAGER_STORE = "connections.json";

    public static final String ERROR_HEADER = "======= Error =======\n";
    public static final Font ERROR_FONT = new Font(MAIN_FONT_NAME, Font.BOLD, 14);
    public static final Color ERROR_FONT_COLOR = new Color(189, 4, 4); // blood

    public static final Color TABLE_HEADER_FONT_COLOR = Color.BLACK;
    public static final Color TABLE_GRID_COLOR = CRATE_COLOR.darker().darker().darker();
    public static final Color TABLE_FOOTER_FONT_COLOR = Color.BLACK;
    public static final Font TABLE_HEADER_FONT = new Font(MAIN_FONT_NAME, Font.BOLD, 18);
    public static final Font TABLE_CELL_FONT = new Font(MAIN_FONT_NAME, Font.PLAIN, 16);
    public static final Font TABLE_FOOTER_FONT = new Font(MAIN_FONT_NAME, Font.PLAIN, 14);
    public static final int TABLE_HEADER_HEIGHT = 50;
    public static final int TABLE_ROW_HEIGHT = 26;

    public static final Color COMMAND_BOARD_FONT_COLOR = Color.WHITE;
    public static final Color COMMAND_BOARD_KEYWORD_FONT_COLOR = CRATE_COLOR;
    public static final Color COMMAND_BOARD_BACKGROUND_COLOR = Color.BLACK;
    public static final Color COMMAND_BOARD_CARET_COLOR = Color.GREEN;
    public static final Font COMMAND_BOARD_HEADER_FONT = new Font(MAIN_FONT_NAME, Font.BOLD, 16);
    public static final Font COMMAND_BOARD_BODY_FONT = new Font(MAIN_FONT_NAME, Font.BOLD, 18);
    public static final Border COMMAND_BOARD_CONNECTED_BORDER = BorderFactory.createLineBorder(CRATE_COLOR, 4, true);
    public static final Border COMMAND_BOARD_DISCONNECTED_BORDER = BorderFactory.createLineBorder(Color.BLACK, 2, true);
    public static final Border COMMAND_BOARD_UNSELECTED_BORDER = BorderFactory.createLineBorder(Color.LIGHT_GRAY, 1, false);


    public static void invokeLater(Runnable ... runnable) {
        if (EventQueue.isDispatchThread()) {
            for (Runnable r : runnable) {
                if (null != r) {
                    r.run();
                }
            }
        } else {
            try {
                EventQueue.invokeLater(() -> {
                    for (Runnable r : runnable) {
                        if (null != r) {
                            r.run();
                        }
                    }
                });
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

    private GUIToolkit() {
        throw new IllegalStateException("not meant to me instantiated");
    }
}
