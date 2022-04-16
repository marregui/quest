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

package io.quest.frontend.results;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.io.Closeable;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.SwingConstants;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.BevelBorder;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

import io.quest.frontend.GTk;
import io.quest.backend.SQLExecutionResponse;
import io.quest.model.SQLTable;
import io.quest.frontend.InfiniteSpinnerPanel;
import io.quest.frontend.editor.QuestPanel;


public class SQLResultsTable extends JPanel implements Closeable {
    private static final long serialVersionUID = 1L;
    private static final Dimension STATUS_LABEL_SIZE = new Dimension(600, 35);
    private static final Dimension NAVIGATION_LABEL_SIZE = new Dimension(300, 35);
    private static final Dimension NAVIGATION_BUTTON_SIZE = new Dimension(100, 35);
    private static final int TABLE_ROW_HEIGHT = 30;
    private static final int TABLE_HEADER_HEIGHT = 50;

    private enum Mode {INFINITE, TABLE, MESSAGE}

    private final JTable table;
    private final JScrollPane tableScrollPanel;
    private final PagedSQLTableModel tableModel;
    private final AtomicReference<SQLTable> results;
    private final QuestPanel questPanel;
    private final JLabel rowRangeLabel;
    private final JLabel statsLabel;
    private final JButton prevButton;
    private final JButton nextButton;
    private final InfiniteSpinnerPanel infiniteSpinner;
    private Component currentModePanel;
    private Mode mode;

    public SQLResultsTable(int width, int height) {
        Dimension size = new Dimension(width, height);
        results = new AtomicReference<>();
        tableModel = new PagedSQLTableModel(results::get);
        table = new JTable(tableModel);
        table.setAutoCreateRowSorter(false);
        table.setRowSelectionAllowed(true);
        table.setColumnSelectionAllowed(true);
        table.setCellSelectionEnabled(true);
        table.setRowHeight(TABLE_ROW_HEIGHT);
        table.setGridColor(GTk.APP_THEME_COLOR.darker().darker().darker());
        table.setFont(GTk.TABLE_CELL_FONT);
        table.setDefaultRenderer(String.class, new SQLCellRenderer(results::get));
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        GTk.setupTableCmdKeyActions(table);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setFont(GTk.TABLE_HEADER_FONT);
        header.setForeground(GTk.TABLE_HEADER_FONT_COLOR);
        statsLabel = new JLabel();
        statsLabel.setFont(GTk.MENU_FONT);
        statsLabel.setForeground(GTk.TABLE_HEADER_FONT_COLOR);
        statsLabel.setPreferredSize(STATUS_LABEL_SIZE);
        statsLabel.setHorizontalAlignment(JLabel.RIGHT);
        rowRangeLabel = new JLabel();
        rowRangeLabel.setFont(GTk.MENU_FONT);
        rowRangeLabel.setForeground(GTk.TABLE_HEADER_FONT_COLOR);
        rowRangeLabel.setPreferredSize(NAVIGATION_LABEL_SIZE);
        rowRangeLabel.setHorizontalAlignment(JLabel.RIGHT);
        prevButton = GTk.button(
                "Prev",
                GTk.Icon.RESULTS_PREV,
                "Go to previous page",
                this::onPrevButton);
        prevButton.setFont(GTk.MENU_FONT);
        prevButton.setForeground(GTk.TABLE_HEADER_FONT_COLOR);
        prevButton.setPreferredSize(NAVIGATION_BUTTON_SIZE);
        nextButton = GTk.button(
                "Next",
                GTk.Icon.RESULTS_NEXT,
                "Go to next page",
                this::onNextButton);
        nextButton.setFont(GTk.MENU_FONT);
        nextButton.setForeground(GTk.TABLE_HEADER_FONT_COLOR);
        nextButton.setPreferredSize(NAVIGATION_BUTTON_SIZE);
        nextButton.setHorizontalTextPosition(SwingConstants.LEFT);
        questPanel = new QuestPanel(true);
        tableScrollPanel = new JScrollPane(
                table,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScrollPanel.getViewport().setBackground(GTk.TABLE_HEADER_FONT_COLOR);
        infiniteSpinner = new InfiniteSpinnerPanel();
        infiniteSpinner.setSize(size);
        changeMode(Mode.TABLE);
        setLayout(new BorderLayout());
        setPreferredSize(size);
        setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        add(currentModePanel, BorderLayout.CENTER);
        add(GTk.flowPanel(
                        statsLabel, rowRangeLabel, prevButton, nextButton),
                BorderLayout.SOUTH);
        updateRowNavigationComponents();
    }

    public void updateStats(String eventType, SQLExecutionResponse res) {
        if (res != null) {
            statsLabel.setText(String.format(
                    "[%s]  Exec: %5d,  Fetch: %5d,  Total: %6d (ms)",
                    eventType,
                    res.getExecMs(),
                    res.getFetchMs(),
                    res.getTotalMs()));
        } else {
            statsLabel.setText("");
        }
    }

    public void onRowsAdded(SQLExecutionResponse res) {
        SQLTable table = res.getTable();
        if (results.compareAndSet(null, table)) {
            resetTableHeader();
        } else if (table.size() > 0) {
            tableModel.fireTableDataChanged();
        }
        updateRowNavigationComponents();
        infiniteSpinner.close();
        if (table.isSingleRowSingleVarcharCol()) {
            questPanel.displayMessage((String) table.getValueAt(0, 0));
            changeMode(Mode.MESSAGE);
        } else {
            if (table.size() == 0) {
                questPanel.displayMessage("OK.\n\nNo results for query:\n" + res.getSqlCommand());
                changeMode(Mode.MESSAGE);
            } else {
                changeMode(Mode.TABLE);
            }
        }
    }

    @Override
    public void close() {
        SQLTable table = results.getAndSet(null);
        if (table != null) {
            table.close();
        }
        tableModel.fireTableStructureChanged();
        infiniteSpinner.close();
        updateStats(null, null);
        updateRowNavigationComponents();
        changeMode(Mode.TABLE);
    }

    public void displayError(Throwable error) {
        questPanel.displayError(error);
        changeMode(Mode.MESSAGE);
    }

    public void showInfiniteSpinner() {
        infiniteSpinner.start();
        changeMode(Mode.INFINITE);
    }

    public void onPrevButton(ActionEvent event) {
        if (prevButton.isEnabled() && tableModel.canDecrPage()) {
            tableModel.decrPage();
            updateRowNavigationComponents();
        }
    }

    public void onNextButton(ActionEvent event) {
        if (nextButton.isEnabled() && tableModel.canIncrPage()) {
            tableModel.incrPage();
            updateRowNavigationComponents();
        }
    }

    private void updateRowNavigationComponents() {
        prevButton.setEnabled(tableModel.canDecrPage());
        nextButton.setEnabled(tableModel.canIncrPage());
        int start = tableModel.getPageStartOffset();
        int end = tableModel.getPageEndOffset();
        int tableSize = tableModel.getTableSize();
        if (tableSize > 0) {
            start++;
        }
        rowRangeLabel.setText(String.format(
                "Rows %d to %d of %-10d", start, end, tableSize));
    }

    private void resetTableHeader() {
        tableModel.refreshTableStructure();
        JTableHeader header = table.getTableHeader();
        header.setForeground(Color.WHITE);
        header.setBackground(Color.BLACK);
        header.setPreferredSize(new Dimension(0, TABLE_HEADER_HEIGHT));
        SQLTable t = results.get();
        TableColumnModel tcm = table.getColumnModel();
        int numCols = tcm.getColumnCount();
        int tableWidth = 0;
        for (int colIdx = 0; colIdx < numCols; colIdx++) {
            TableColumn col = tcm.getColumn(colIdx);
            int width = SQLType.resolveColWidth(t, colIdx);
            tableWidth += width;
            col.setMinWidth(width);
            col.setPreferredWidth(width);
        }
        table.setAutoResizeMode(tableWidth < getWidth() ?
                JTable.AUTO_RESIZE_ALL_COLUMNS : JTable.AUTO_RESIZE_OFF);
        tableModel.fireTableDataChanged();
    }

    private void changeMode(Mode newMode) {
        if (mode != newMode) {
            mode = newMode;
            Component toRemove = currentModePanel;
            switch (newMode) {
                case TABLE:
                    currentModePanel = tableScrollPanel;
                    break;
                case INFINITE:
                    currentModePanel = infiniteSpinner;
                    break;
                case MESSAGE:
                    currentModePanel = questPanel;
                    break;
            }
            if (toRemove != null) {
                remove(toRemove);
            }
            add(currentModePanel, BorderLayout.CENTER);
            validate();
            repaint();
        }
    }
}
