package io.crate.cli.widgets;

import io.crate.cli.backend.SQLExecutionResponse;
import io.crate.cli.backend.SQLExecutor;
import io.crate.cli.backend.SQLRowType;
import io.crate.cli.common.GUIToolkit;
import io.crate.cli.common.ObjectTableModel;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.TableModelEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.Closeable;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.List;


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


    private final List<SQLRowType> fullResults;
    private final JTable windowTable;
    private final JTextPane errorPane;
    private final JScrollPane windowTablePane;
    private final ObjectTableModel<SQLRowType> windowedResultsTableModel;
    private int currentPage;
    private boolean hasCompleted;
    private final JLabel navigationLabel;
    private final JLabel statusLabel;
    private final JButton leftButton;
    private final JButton rightButton;
    private final InfiniteProgressPanel infiniteProgressPanel;
    private Component currentPanel;
    private Mode mode;


    public SQLResultsManager() {
        fullResults = new ArrayList<>();
        windowedResultsTableModel = new ObjectTableModel<>(new String[]{}, SQLRowType::get, SQLRowType::set) {
            @Override
            public boolean isCellEditable(int rowIdx, int colIdx) {
                return false;
            }
        };
        windowedResultsTableModel.addTableModelListener(this::onTableModelEvent);
        windowTable = GUIToolkit.newTable(windowedResultsTableModel, null);
        currentPage = 0;
        navigationLabel = new JLabel(NO_RESULTS_LABEL);
        navigationLabel.setFont(GUIToolkit.TABLE_FOOTER_FONT);
        navigationLabel.setForeground(GUIToolkit.TABLE_FOOTER_COLOR);
        navigationLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        navigationLabel.setPreferredSize(NAVIGATION_OFFSETS_LABEL_SIZE);
        navigationLabel.setSize(NAVIGATION_OFFSETS_LABEL_SIZE);
        navigationLabel.setHorizontalAlignment(JLabel.LEADING);
        statusLabel = new JLabel(NO_RESULTS_LABEL);
        statusLabel.setFont(GUIToolkit.TABLE_FOOTER_FONT);
        statusLabel.setForeground(GUIToolkit.TABLE_FOOTER_COLOR);
        statusLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        statusLabel.setPreferredSize(STATUS_LABEL_SIZE);
        statusLabel.setSize(STATUS_LABEL_SIZE);
        statusLabel.setHorizontalAlignment(JLabel.LEADING);
        leftButton = new JButton(NAVIGATION_BUTTON_PREV_TEXT);
        leftButton.setFont(GUIToolkit.TABLE_FOOTER_FONT);
        leftButton.setForeground(GUIToolkit.TABLE_FOOTER_COLOR);
        leftButton.setPreferredSize(NAVIGATION_BUTTON_SIZE);
        leftButton.addActionListener(this::onPrevButtonEvent);
        rightButton = new JButton(NAVIGATION_BUTTON_NEXT_TEXT);
        rightButton.setFont(GUIToolkit.TABLE_FOOTER_FONT);
        rightButton.setForeground(GUIToolkit.TABLE_FOOTER_COLOR);
        rightButton.setPreferredSize(NAVIGATION_BUTTON_SIZE);
        rightButton.addActionListener(this::onNextButtonEvent);
        errorPane = GUIToolkit.newTextComponent();
        errorPane.setFont(GUIToolkit.ERROR_FONT);
        errorPane.setForeground(GUIToolkit.ERROR_FONT_COLOR);
        errorPane.setEditable(false);
        JPanel controlsPane = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 5));
        controlsPane.add(statusLabel);
        controlsPane.add(navigationLabel);
        controlsPane.add(leftButton);
        controlsPane.add(rightButton);
        windowTablePane = new JScrollPane(windowTable);
        windowTablePane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        windowTablePane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        mode = Mode.TABLE;
        currentPanel = windowTablePane;
        setLayout(new BorderLayout());
        add(windowTablePane, BorderLayout.CENTER);
        add(controlsPane, BorderLayout.SOUTH);
        infiniteProgressPanel = new InfiniteProgressPanel();
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
        validate();
        repaint();
    }

    public void showInfiniteProgressPanel() {
        changeMode(Mode.INFINITE);
        infiniteProgressPanel.start();
    }

    public void removeInfiniteProgressPanel() {
        infiniteProgressPanel.close();
        changeMode(Mode.TABLE);
    }

    private void changeMode(Mode newMode) {
        if (mode == newMode) {
            return;
        }
        mode = newMode;
        Component toRemove = currentPanel;
        switch (newMode) {
            case TABLE:
                currentPanel = windowTablePane;
                break;

            case INFINITE:
                currentPanel = infiniteProgressPanel;
                break;

            case ERROR:
                currentPanel = errorPane;
                break;
        }
        remove(toRemove);
        add(currentPanel, BorderLayout.CENTER);
        validate();
        repaint();
    }

    private void toggleComponents() {
        int currentCount = fullResults.size();
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
        int currentCount = fullResults.size();
        int startOffset = ROWS_PER_PAGE * currentPage;
        int endOffset = startOffset + Math.min(currentCount, ROWS_PER_PAGE);
        windowedResultsTableModel.setRows(fullResults.subList(startOffset, endOffset));
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
        fullResults.clear();
        windowedResultsTableModel.clear();
        infiniteProgressPanel.close();
        toggleComponents();
    }

    public void addRows(List<SQLRowType> rows, boolean needsClearing, boolean expectMore) {
        if (needsClearing) {
            clear();
        }
        if (Mode.ERROR == mode) {
            changeMode(Mode.TABLE);
        }
        hasCompleted = false == expectMore;
        int newRowsCount = rows.size();
        if (newRowsCount > 0) {
            if (0 == fullResults.size()) {
                SQLRowType firstRow = rows.get(0);
                windowedResultsTableModel.reset(
                        firstRow.getColumnNames(),
                        firstRow.getColumnTypes());
            }
            fullResults.addAll(rows);
            int currentCount = windowedResultsTableModel.getRowCount();
            if (currentCount + newRowsCount <= ROWS_PER_PAGE) {
                windowedResultsTableModel.addRows(rows);
            } else {
                int allowedCount = ROWS_PER_PAGE - currentCount;
                if (allowedCount > 0) {
                    windowedResultsTableModel.addRows(rows.subList(0, allowedCount));
                }
            }
        }
        toggleComponents();
    }

    public int getRowCount() {
        return windowedResultsTableModel.getRowCount();
    }

    @Override
    public void close() {
        clear();
    }
}
