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


public class SQLResultsTable extends JPanel implements Closeable {

    private static final Font CONTROLS_FONT = new Font("monospaced", Font.BOLD, 14);
    private static final Color CONTROLS_FONT_COLOR = Color.BLACK;
    private static final Color ERROR_FONT_COLOR = new Color(189, 4, 4);
    private static final String NO_RESULTS_LABEL = "No results";
    private static final Dimension BUTTON_SIZE = new Dimension(70, 42);
    private static final Dimension LABEL_SIZE = new Dimension(400, 40);
    private static final String ERROR_HEADER = "======= Error =======\n";
    private static final int ROWS_PER_PAGE = 100;


    private final List<SQLRowType> results;
    private final JTable windowTable;
    private final JTextPane errorPane;
    private JComponent currentPane;
    private final JScrollPane windowTablePane;
    private final ObjectTableModel<SQLRowType> windowedTableModel;
    private int currentPage;
    private boolean hasCompleted;
    private final JLabel offsetLabel;
    private final JButton prevButton;
    private final JButton nextButton;


    public SQLResultsTable() {
        results = new ArrayList<>();
        windowedTableModel = new ObjectTableModel<>(new String[]{}, Map::get, Map::put) {
            @Override
            public boolean isCellEditable(int rowIdx, int colIdx) {
                return false;
            }
        };
        windowedTableModel.addTableModelListener(this::onTableModelEvent);
        windowTable = GUIFactory.newTable(windowedTableModel, null);
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
        currentPane = windowTablePane;
        setLayout(new BorderLayout());
        add(windowTablePane, BorderLayout.CENTER);
        add(controlsPane, BorderLayout.SOUTH);
        toggleComponents();
    }

    private void toggleComponents() {
        int currentCount = results.size();
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
        if (currentPane == windowTablePane) {
            currentPane = errorPane;
            remove(windowTablePane);
            add(errorPane, BorderLayout.CENTER);
            validate();
            repaint();
        }
    }

    private void updateWindowedTableModel() {
        int currentCount = results.size();
        int startOffset = ROWS_PER_PAGE * currentPage;
        int endOffset = startOffset + currentCount % ROWS_PER_PAGE;
        windowedTableModel.setRows(results.subList(startOffset, endOffset));
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

    public void addRows(List<SQLRowType> rows, boolean expectMore) {
        if (currentPane == errorPane) {
            currentPane = windowTablePane;
            remove(errorPane);
            add(windowTablePane, BorderLayout.CENTER);
        }
        if (expectMore && hasCompleted) {
            results.clear();
            windowedTableModel.clear();
        }
        int newRowsCount = rows.size();
        if (0 == newRowsCount) {
            return;
        }
        int currentCount = results.size();
        if (0 == currentCount) {
            windowedTableModel.reset(rows.get(0).getColumnNames());
        }
        if (currentCount + newRowsCount <= ROWS_PER_PAGE) {
            windowedTableModel.addRows(rows);
        } else {
            int allowedCount = ROWS_PER_PAGE - currentCount;
            windowedTableModel.addRows(rows.subList(0, allowedCount));
        }
        results.addAll(rows);
        hasCompleted = false == expectMore;
        toggleComponents();
    }

    @Override
    public void close() {
        results.clear();
        windowedTableModel.clear();
    }
}
