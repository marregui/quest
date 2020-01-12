package io.crate.cli.gui.widgets;

import io.crate.cli.connections.SQLRowType;
import io.crate.cli.gui.common.GUIFactory;
import io.crate.cli.gui.common.ObjectTableModel;

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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;


public class SQLResultsTable extends JPanel implements Closeable {

    private static final Font CONTROLS_FONT = new Font("monospaced", Font.BOLD, 14);
    private static final Color CONTROLS_FONT_COLOR = Color.BLACK;
    private static final Color ERROR_FONT_COLOR = new Color(189, 4, 4);
    private static final String NO_RESULTS_LABEL = "No results";
    private static final Dimension BUTTON_SIZE = new Dimension(70, 42);
    private static final Dimension LABEL_SIZE = new Dimension(400, 40);
    private static final String ERROR_HEADER = "======= Error =======\n";
    private static final int ROWS_PER_PAGE = 1000;


    private enum Mode {
        TABLE, ERROR
    }


    private final List<SQLRowType> fullResults;
    private final JTable windowTable;
    private final JTextPane errorPane;
    private final JScrollPane windowTablePane;
    private final ObjectTableModel<SQLRowType> windowedResultsTableModel;
    private int currentPage;
    private boolean hasCompleted;
    private final JLabel offsetLabel;
    private final JButton prevButton;
    private final JButton nextButton;
    private Mode mode;


    public SQLResultsTable() {
        fullResults = new ArrayList<>();
        windowedResultsTableModel = new ObjectTableModel<>(new String[]{}, SQLRowType::get, SQLRowType::set) {
            @Override
            public boolean isCellEditable(int rowIdx, int colIdx) {
                return false;
            }
        };
        windowedResultsTableModel.addTableModelListener(this::onTableModelEvent);
        windowTable = GUIFactory.newTable(windowedResultsTableModel, null);
        currentPage = 0;
        offsetLabel = new JLabel(NO_RESULTS_LABEL);
        offsetLabel.setFont(CONTROLS_FONT);
        offsetLabel.setForeground(CONTROLS_FONT_COLOR);
        offsetLabel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        offsetLabel.setPreferredSize(LABEL_SIZE);
        offsetLabel.setSize(LABEL_SIZE);
        offsetLabel.setHorizontalAlignment(JLabel.CENTER);
        prevButton = new JButton("PREV");
        prevButton.setFont(CONTROLS_FONT);
        prevButton.setForeground(CONTROLS_FONT_COLOR);
        prevButton.setPreferredSize(BUTTON_SIZE);
        prevButton.addActionListener(this::onPrevButtonEvent);
        nextButton = new JButton("NEXT");
        nextButton.setFont(CONTROLS_FONT);
        nextButton.setForeground(CONTROLS_FONT_COLOR);
        nextButton.setPreferredSize(BUTTON_SIZE);
        nextButton.addActionListener(this::onNextButtonEvent);
        errorPane = GUIFactory.newTextComponent();
        errorPane.setForeground(ERROR_FONT_COLOR);
        errorPane.setEditable(false);
        JPanel controlsPane = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 5));
        controlsPane.add(offsetLabel);
        controlsPane.add(prevButton);
        controlsPane.add(nextButton);
        windowTablePane = new JScrollPane(windowTable);
        windowTablePane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        windowTablePane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        mode = Mode.TABLE;
        setLayout(new BorderLayout());
        add(windowTablePane, BorderLayout.CENTER);
        add(controlsPane, BorderLayout.SOUTH);
        toggleComponents();
    }

    private void changeMode(Mode newMode) {
        mode = newMode;
        Component toRemove = null;
        Component toAdd = null;
        switch (newMode) {
            case TABLE:
                toRemove = errorPane;
                toAdd = windowTablePane;
                break;

            case ERROR:
                toRemove = windowTablePane;
                toAdd = errorPane;
                break;
        }
        remove(toRemove);
        add(toAdd, BorderLayout.CENTER);
        validate();
        repaint();
    }

    private void toggleComponents() {
        int currentCount = fullResults.size();
        prevButton.setEnabled(currentPage > 0);
        int maxPage = (currentCount / ROWS_PER_PAGE) - 1;
        if (currentCount % ROWS_PER_PAGE > 0) {
            maxPage++;
        }
        nextButton.setEnabled(currentPage < maxPage);
        if (currentCount > 0) {
            int startOffset = 1 + currentPage * ROWS_PER_PAGE;
            int endOffset = startOffset + ROWS_PER_PAGE - 1;
            if (endOffset > currentCount) {
                endOffset = currentCount;
            }
            String text = String.format(
                    Locale.ENGLISH,
                    "Showing %d to %d of %d [%s]",
                    startOffset, endOffset, currentCount, hasCompleted ? "finished" : "ongoing");
            offsetLabel.setText(text);
        }
    }

    public void displayError(Throwable error) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(ERROR_HEADER).append("\n");
        try (StringWriter sw = new StringWriter();
             PrintWriter pw = new PrintWriter(sw)) {
            error.printStackTrace(pw);
            sb.append(sw.toString());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        errorPane.setText(sb.toString());
        if (Mode.TABLE == mode) {
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
    }

    public void addRow(SQLRowType row, boolean needsClearing, boolean expectMore) {
        if (needsClearing) {
            clear();
        }
        if (Mode.ERROR == mode) {
            changeMode(Mode.TABLE);
        }
        hasCompleted = false == expectMore;
        if (0 == fullResults.size()) {
            windowedResultsTableModel.reset(row.getColumnNames());
        }
        fullResults.add(row);
        int currentCount = windowedResultsTableModel.getRowCount();
        if (currentCount + 1 <= ROWS_PER_PAGE) {
            windowedResultsTableModel.addRow(row);
        }
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
                windowedResultsTableModel.reset(rows.get(0).getColumnNames());
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


    public static void main(String[] args) throws Exception {
        int maxRows = 100;
        SQLResultsTable table = new SQLResultsTable();
        GUIFactory.newFrame(
                "SQLResultsTable",
                80,
                80, table)
                .setVisible(true);
        ScheduledExecutorService sched = Executors.newSingleThreadScheduledExecutor();
        AtomicInteger ai = new AtomicInteger(0);
        String[] colNames = {"key", "name"};
        sched.scheduleAtFixedRate(() -> {
            int i = ai.getAndIncrement();
            boolean expectMore = i + 1 < maxRows;
            boolean needsClearing = 0 == i;
            table.addRow(new SQLRowType(
                    String.valueOf(i),
                    colNames,
                    new Object[]{i, "banana"}),
                    needsClearing,
                    expectMore);
        }, 2000, 10, TimeUnit.MILLISECONDS);
    }
}
