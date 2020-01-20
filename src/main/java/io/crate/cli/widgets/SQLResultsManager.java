package io.crate.cli.widgets;

import io.crate.cli.backend.SQLExecutionResponse;
import io.crate.cli.backend.SQLExecutor;
import io.crate.cli.backend.SQLTable;
import io.crate.cli.backend.SQLTable.SQLTableRow;
import io.crate.cli.common.*;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;


public class SQLResultsManager extends JPanel implements Closeable {

    private static final String NO_RESULTS_LABEL = "  No timing results";
    private static final String NAVIGATION_BUTTON_PREV_TEXT = "PREV";
    private static final String NAVIGATION_BUTTON_NEXT_TEXT = "NEXT";
    private static final Dimension NAVIGATION_BUTTON_SIZE = new Dimension(70, 38);
    private static final Dimension STATUS_LABEL_SIZE = new Dimension(600, 35);
    private static final Dimension NAVIGATION_OFFSETS_LABEL_SIZE = new Dimension(400, 35);

    private static final int ROWS_PER_PAGE = 1000;


    private enum Mode {
        INFINITE, TABLE, ERROR
    }

    private final JTable windowTable;
    private final JScrollPane windowTablePane;
    private final SQLTable results;
    private final ObjectTableModel<SQLTableRow> windowedTableModel;
    private int currentPage;
    private final JTextPane errorPane;
    private boolean hasCompleted;
    private final JLabel navigationLabel;
    private final JLabel statusLabel;
    private final JButton leftButton;
    private final JButton rightButton;

    private final InfiniteSpinnerPanel infiniteSpinner;
    private Component currentModePanel;
    private Mode mode;


    public SQLResultsManager() {
        results = SQLTable.emptyTable();
        windowedTableModel = new ObjectTableModel<>(new String[]{}, SQLTableRow::get, null) {
            @Override
            public boolean isCellEditable(int rowIdx, int colIdx) {
                return false;
            }
        };
        windowedTableModel.addTableModelListener(this::onTableModelEvent);
        windowTable = new JTable(windowedTableModel);
        windowTable.setAutoCreateRowSorter(false);
        windowTable.setRowSelectionAllowed(false);
        windowTable.setRowHeight(GUIToolkit.TABLE_ROW_HEIGHT + 5);
        windowTable.setGridColor(GUIToolkit.TABLE_GRID_COLOR);
        windowTable.setFont(GUIToolkit.TABLE_CELL_FONT);
        windowTable.setDefaultRenderer(String.class, new SQLCellRenderer(results));
        windowTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        windowTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JTableHeader header = windowTable.getTableHeader();
        TableColumnModel columnModel = header.getColumnModel();
        columnModel.setColumnSelectionAllowed(false);
        header.setReorderingAllowed(false);
        header.setFont(GUIToolkit.TABLE_HEADER_FONT);
        header.setForeground(GUIToolkit.TABLE_HEADER_FONT_COLOR);
        currentPage = 0;
        navigationLabel = new JLabel(NO_RESULTS_LABEL);
        navigationLabel.setFont(GUIToolkit.TABLE_FOOTER_FONT);
        navigationLabel.setForeground(GUIToolkit.TABLE_FOOTER_FONT_COLOR);
        navigationLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        navigationLabel.setPreferredSize(NAVIGATION_OFFSETS_LABEL_SIZE);
        navigationLabel.setSize(NAVIGATION_OFFSETS_LABEL_SIZE);
        navigationLabel.setHorizontalAlignment(JLabel.LEADING);
        statusLabel = new JLabel(NO_RESULTS_LABEL);
        statusLabel.setFont(GUIToolkit.TABLE_FOOTER_FONT);
        statusLabel.setForeground(GUIToolkit.TABLE_FOOTER_FONT_COLOR);
        statusLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        statusLabel.setPreferredSize(STATUS_LABEL_SIZE);
        statusLabel.setSize(STATUS_LABEL_SIZE);
        statusLabel.setHorizontalAlignment(JLabel.LEADING);
        leftButton = new JButton(NAVIGATION_BUTTON_PREV_TEXT);
        leftButton.setFont(GUIToolkit.TABLE_FOOTER_FONT);
        leftButton.setForeground(GUIToolkit.TABLE_FOOTER_FONT_COLOR);
        leftButton.setPreferredSize(NAVIGATION_BUTTON_SIZE);
        leftButton.addActionListener(this::onPrevButtonEvent);
        rightButton = new JButton(NAVIGATION_BUTTON_NEXT_TEXT);
        rightButton.setFont(GUIToolkit.TABLE_FOOTER_FONT);
        rightButton.setForeground(GUIToolkit.TABLE_FOOTER_FONT_COLOR);
        rightButton.setPreferredSize(NAVIGATION_BUTTON_SIZE);
        rightButton.addActionListener(this::onNextButtonEvent);
        errorPane = new JTextPane();
        errorPane.setEditable(false);
        errorPane.setCaretPosition(0);
        errorPane.setMargin(new Insets(5, 5, 5, 5));
        errorPane.setBackground(Color.BLACK);
        errorPane.setFont(GUIToolkit.ERROR_FONT);
        errorPane.setForeground(GUIToolkit.ERROR_FONT_COLOR);
        JPanel controlsPane = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 5));
        controlsPane.add(statusLabel);
        controlsPane.add(navigationLabel);
        controlsPane.add(leftButton);
        controlsPane.add(rightButton);
        windowTablePane = new JScrollPane(
                windowTable,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        windowTablePane.getViewport().setBackground(Color.BLACK);
        infiniteSpinner = new InfiniteSpinnerPanel();
        mode = Mode.TABLE;
        currentModePanel = windowTablePane;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        add(currentModePanel, BorderLayout.CENTER);
        add(controlsPane, BorderLayout.SOUTH);
        toggleComponents();
    }

    public void updateStatus(SQLExecutor.EventType event, SQLExecutionResponse response) {
        statusLabel.setText(String.format(
                Locale.ENGLISH,
                "  %s Q.Exec: %d, Q.Fetch: %d, Q.Total: %d (ms)",
                event,
                response.getQueryExecutionElapsedMs(),
                response.getFetchResultsElapsedMs(),
                response.getTotalElapsedMs()));
        repaint();
    }

    public void showInfiniteProgressPanel() {
        changeMode(Mode.INFINITE);
        infiniteSpinner.start();
    }

    public void removeInfiniteProgressPanel() {
        infiniteSpinner.close();
        changeMode(Mode.TABLE);
    }

    private void changeMode(Mode newMode) {
        if (mode == newMode) {
            return;
        }
        mode = newMode;
        Component toRemove = currentModePanel;
        switch (newMode) {
            case TABLE:
                currentModePanel = windowTablePane;
                break;

            case INFINITE:
                currentModePanel = infiniteSpinner;
                break;

            case ERROR:
                currentModePanel = errorPane;
                break;
        }
        remove(toRemove);
        add(currentModePanel, BorderLayout.CENTER);
        validate();
        repaint();
    }

    private void toggleComponents() {
        int currentCount = results.getSize();
        leftButton.setEnabled(currentPage > 0);
        int maxPage = (currentCount / ROWS_PER_PAGE) - 1;
        if (currentCount % ROWS_PER_PAGE > 0) {
            maxPage++;
        }
        rightButton.setEnabled(currentPage < maxPage);
        if (currentCount > 0) {
            int startOffset = 1 + currentPage * ROWS_PER_PAGE;
            int endOffset = startOffset + ROWS_PER_PAGE - 1;
            if (endOffset > currentCount) {
                endOffset = currentCount;
            }
            String text = String.format(
                    Locale.ENGLISH,
                    "  Showing %d to %d of %d [%s]",
                    startOffset, endOffset, currentCount, hasCompleted ? "finished" : "ongoing");
            navigationLabel.setText(text);
        } else {
            navigationLabel.setText(String.format(
                    Locale.ENGLISH,
                    "  Showing 0 results"));
        }
    }

    public void displayError(Throwable error) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(GUIToolkit.ERROR_HEADER).append("\n");
        try (StringWriter sw = new StringWriter();
             PrintWriter pw = new PrintWriter(sw)) {
            error.printStackTrace(pw);
            sb.append(sw.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        errorPane.setText(sb.toString());
        if (Mode.ERROR != mode) {
            changeMode(Mode.ERROR);
        }
    }

    private void updateWindowedTableModel() {
        int currentCount = results.getSize();
        int startOffset = ROWS_PER_PAGE * currentPage;
        int endOffset = startOffset + Math.min(currentCount, ROWS_PER_PAGE);
        windowedTableModel.setRows(results.getRows(startOffset, endOffset));
    }

    private void onPrevButtonEvent(ActionEvent event) {
        currentPage--;
        updateWindowedTableModel();
        toggleComponents();
    }

    private void onNextButtonEvent(ActionEvent event) {
        currentPage++;
        updateWindowedTableModel();
        toggleComponents();
    }

    private void onTableModelEvent(TableModelEvent event) {
        // nothing
    }

    public void clear() {
        results.clear();
        windowedTableModel.clear();
        infiniteSpinner.close();
        toggleComponents();
    }

    private void resetTableHeader() {
        JTableHeader header = windowTable.getTableHeader();
        header.setForeground(Color.WHITE);
        header.setBackground(Color.BLACK);
        header.setPreferredSize(new Dimension(0, GUIToolkit.TABLE_HEADER_HEIGHT));
        String [] colNames = results.getColumnNames();
        int [] colTypes = results.getColumnTypes();
        windowedTableModel.reset(colNames, colTypes);
        TableColumnModel tcm = windowTable.getColumnModel();
        int numCols = tcm.getColumnCount();
        int tableWidth = 0;
        for (int i=0; i < numCols; i++) {
            TableColumn col = tcm.getColumn(i);
            int minWidth = resolveColumnWidth(colNames[i], colTypes[i]);
            tableWidth += minWidth;
            col.setMinWidth(minWidth);
        }
        windowTable.setAutoResizeMode(tableWidth < getWidth() ?
                JTable.AUTO_RESIZE_ALL_COLUMNS : JTable.AUTO_RESIZE_OFF);
    }

    private static int resolveColumnWidth(String name, int type) {
        return Math.max(
                GUIToolkit.TABLE_CELL_MIN_WIDTH,
                GUIToolkit.TABLE_CELL_CHAR_WIDTH * (name.length() + SqlType.resolveName(type).length()));
    }

    public void addRows(SQLTable rows, boolean expectMore) {
        if (Mode.ERROR == mode) {
            changeMode(Mode.TABLE);
        }
        hasCompleted = false == expectMore;
        int newRowsCount = rows.getSize();
        if (newRowsCount > 0) {
            boolean needsNewHeader = 0 == results.getSize();
            results.merge(rows);
            if (needsNewHeader) {
                resetTableHeader();
            }
            int currentCount = windowedTableModel.getRowCount();
            if (currentCount + newRowsCount <= ROWS_PER_PAGE) {
                windowedTableModel.addRows(rows.getRows());
            } else {
                int allowedCount = ROWS_PER_PAGE - currentCount;
                if (allowedCount > 0) {
                    windowedTableModel.addRows(results.getRows(0, allowedCount));
                }
            }
        }
        toggleComponents();
    }

    @Override
    public void close() {
        clear();
    }
}
