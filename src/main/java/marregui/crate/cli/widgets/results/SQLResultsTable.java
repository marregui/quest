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

package marregui.crate.cli.widgets.results;

import static marregui.crate.cli.GUITk.createFlowPanel;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.io.Closeable;
import java.util.concurrent.atomic.AtomicReference;

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

import marregui.crate.cli.GUITk;
import marregui.crate.cli.backend.SQLExecResponse;
import marregui.crate.cli.backend.SQLTable;
import marregui.crate.cli.widgets.InfiniteSpinnerPanel;
import marregui.crate.cli.widgets.MessagePane;


public class SQLResultsTable extends JPanel implements Closeable {

    private static final long serialVersionUID = 1L;
    private static final Dimension STATUS_LABEL_SIZE = new Dimension(600, 35);
    private static final Dimension NAVIGATION_LABEL_SIZE = new Dimension(300, 35);
    private static final Dimension NAVIGATION_BUTTON_SIZE = new Dimension(70, 35);
    private static final Color TABLE_GRID_COLOR = GUITk.APP_THEME_COLOR.darker().darker().darker();
    private static final Font TABLE_FOOTER_FONT = new Font(GUITk.MAIN_FONT_NAME, Font.BOLD, 14);
    private static final Color TABLE_FOOTER_FONT_COLOR = Color.BLACK;
    private static final int TABLE_ROW_HEIGHT = 30;
    private static final int TABLE_CELL_MIN_WIDTH = 300;
    private static final int TABLE_CELL_CHAR_WIDTH = 15;
    private static final int TABLE_HEADER_HEIGHT = 50;

    private enum Mode {
        INFINITE, TABLE, MESSAGE
    }

    private final JTable table;
    private final JScrollPane tableScrollPanel;
    private final PagedSQLTableModel tableModel;
    private final AtomicReference<SQLTable> results;
    private final MessagePane messagePane;
    private final JLabel rowRangeLabel;
    private final JLabel statusLabel;
    private final JButton prevButton;
    private final JButton nextButton;
    private final InfiniteSpinnerPanel infiniteSpinner;
    private Component currentModePanel;
    private Mode mode;

    public SQLResultsTable(int width, int height) {
        Dimension size = new Dimension(width, height);
        results = new AtomicReference<SQLTable>();
        tableModel = new PagedSQLTableModel(results::get);
        table = new JTable(tableModel);
        table.setAutoCreateRowSorter(false);
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(false);
        table.setCellSelectionEnabled(true);
        table.setRowHeight(TABLE_ROW_HEIGHT);
        table.setGridColor(TABLE_GRID_COLOR);
        table.setFont(GUITk.TABLE_CELL_FONT);
        table.setDefaultRenderer(String.class, new SQLCellRenderer(results::get));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        JTableHeader header = table.getTableHeader();
        header.setReorderingAllowed(false);
        header.setFont(GUITk.TABLE_HEADER_FONT);
        header.setForeground(GUITk.TABLE_HEADER_FONT_COLOR);
        statusLabel = new JLabel();
        statusLabel.setFont(TABLE_FOOTER_FONT);
        statusLabel.setForeground(TABLE_FOOTER_FONT_COLOR);
        statusLabel.setPreferredSize(STATUS_LABEL_SIZE);
        statusLabel.setHorizontalAlignment(JLabel.RIGHT);
        rowRangeLabel = new JLabel();
        rowRangeLabel.setFont(TABLE_FOOTER_FONT);
        rowRangeLabel.setForeground(TABLE_FOOTER_FONT_COLOR);
        rowRangeLabel.setPreferredSize(NAVIGATION_LABEL_SIZE);
        rowRangeLabel.setHorizontalAlignment(JLabel.RIGHT);
        prevButton = new JButton("PREV");
        prevButton.setFont(TABLE_FOOTER_FONT);
        prevButton.setForeground(TABLE_FOOTER_FONT_COLOR);
        prevButton.setPreferredSize(NAVIGATION_BUTTON_SIZE);
        prevButton.addActionListener(this::onPrevButtonEvent);
        nextButton = new JButton("NEXT");
        nextButton.setFont(TABLE_FOOTER_FONT);
        nextButton.setForeground(TABLE_FOOTER_FONT_COLOR);
        nextButton.setPreferredSize(NAVIGATION_BUTTON_SIZE);
        nextButton.addActionListener(this::onNextButtonEvent);
        messagePane = new MessagePane();
        tableScrollPanel = new JScrollPane(table, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
            JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        tableScrollPanel.getViewport().setBackground(Color.BLACK);
        infiniteSpinner = new InfiniteSpinnerPanel();
        infiniteSpinner.setSize(size);
        changeMode(Mode.TABLE);
        setLayout(new BorderLayout());
        setPreferredSize(size);
        setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        add(currentModePanel, BorderLayout.CENTER);
        add(createFlowPanel(statusLabel, rowRangeLabel, prevButton, nextButton), BorderLayout.SOUTH);
        updateRowNavigationComponents();
    }

    public void updateStats(String eventType, SQLExecResponse res) {
        if (res != null) {
            statusLabel.setText(String.format("[%s]  Exec: %5d,  Fetch: %5d,  Total: %6d (ms)", eventType, res.getExecMs(),
                res.getFetchMs(), res.getTotalMs()));
        }
        else {
            statusLabel.setText("");
        }
    }

    public void onRowsAddedEvent(SQLExecResponse res) {
        SQLTable table = res.getTable();
        if (results.compareAndSet(null, table)) {
            resetTableHeader();
        }
        else if (table.size() > 0) {
            tableModel.fireTableDataChanged();
        }
        updateRowNavigationComponents();
        infiniteSpinner.close();
        if (table.isSingleRowSingleVarcharCol()) {
            messagePane.displayMessage((String) table.getValueAt(0, 0));
            changeMode(Mode.MESSAGE);
        }
        else {
            changeMode(Mode.TABLE);
        }
    }

    @Override
    public void close() {
        SQLTable table = results.getAndSet(null);
        if (table != null) {
            table.clear();
        }
        tableModel.fireTableStructureChanged();
        infiniteSpinner.close();
        updateStats(null, null);
        updateRowNavigationComponents();
        changeMode(Mode.TABLE);
    }

    /**
     * Displays the error's stack trace.
     * 
     * @param error carries the stack trace to be displayed
     */
    public void displayError(Throwable error) {
        messagePane.displayError(error);
        changeMode(Mode.MESSAGE);
    }

    /**
     * Shows the infinite spinner, to signify background activity is progressing.
     */
    public void showInfiniteSpinner() {
        infiniteSpinner.start();
        changeMode(Mode.INFINITE);
    }

    /**
     * Hides the infinite spinner.
     */
    public void hideInfiniteSpinner() {
        infiniteSpinner.close();
    }

    /**
     * To be called when we want to move a page back in the GUI.
     * 
     * @param event event that triggered this method's call
     */
    public void onPrevButtonEvent(ActionEvent event) {
        if (prevButton.isEnabled() && tableModel.canDecrPage()) {
            tableModel.decrPage();
            updateRowNavigationComponents();
        }
    }

    /**
     * To be called when we want to move a page forward in the GUI.
     * 
     * @param event event that triggered this method's call
     */
    public void onNextButtonEvent(ActionEvent event) {
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
        rowRangeLabel.setText(String.format("Rows %d to %d of %-10d", start, end, tableSize));
    }

    private void resetTableHeader() {
        JTableHeader header = table.getTableHeader();
        header.setForeground(Color.WHITE);
        header.setBackground(Color.BLACK);
        header.setPreferredSize(new Dimension(0, TABLE_HEADER_HEIGHT));
        SQLTable t = results.get();
        String[] colNames = t.getColNames();
        int[] colTypes = t.getColTypes();
        TableColumnModel tcm = table.getColumnModel();
        int numCols = tcm.getColumnCount();
        int tableWidth = 0;
        for (int i = 0; i < numCols; i++) {
            TableColumn col = tcm.getColumn(i);
            int minWidth = resolveColWidth(colNames[i], colTypes[i]);
            tableWidth += minWidth;
            col.setMinWidth(minWidth);
        }
        table.setAutoResizeMode(tableWidth < getWidth() ? JTable.AUTO_RESIZE_ALL_COLUMNS : JTable.AUTO_RESIZE_OFF);
        tableModel.fireTableStructureChanged();
    }

    private static int resolveColWidth(String name, int type) {
        return Math.max(TABLE_CELL_MIN_WIDTH, TABLE_CELL_CHAR_WIDTH * (name.length() + SQLType.resolveName(type).length()));
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
                    currentModePanel = messagePane;
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
