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

package io.quest.frontend.plot;

import io.quest.frontend.GTk;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.Stack;

import static io.quest.frontend.GTk.configureMenuItem;

public class Plot extends JPanel implements MouseListener, MouseMotionListener {

    private static final long serialVersionUID = 1L;
    private static final Color BORDER_COLOR = new Color(153, 153, 153);
    private static final Color UNITS_COLOR = new Color(105, 105, 105);
    private static final float[] DASHED_LINE = new float[]{1, 8};
    private static final int X_RANGE_NUMBER_OF_TICKS = 15;
    private static final int Y_RANGE_NUMBER_OF_TICKS = 10;
    private static final int INSET_TOP = 10;
    private static final int INSET_BOTTOM = 50;
    private static final int INSET_LEFT = 80;
    private static final int INSET_RIGHT = 10;
    private static final Insets PLOT_INSETS = new Insets(
            INSET_TOP, INSET_LEFT, INSET_BOTTOM, INSET_RIGHT
    );
    private static final double X_AXIS_EXTRA_VISIBILITY_DELTA = 0.01F;
    private static final double Y_AXIS_EXTRA_VISIBILITY_DELTA = 0.04F;

    private DataSet dataSet;
    private String xAxisLabel;
    private AxisLabels xTickLabels, yTickLabels;
    private Range range;
    private Point2D.Double selectionAreaStartPoint, selectionAreaEndPoint;
    private boolean selectionAreaFirstPointIsInsidePlotArea, selectionOriginatesInOtherPlot;
    private Stack<Range> zoomStack;
    private int clickedMouseButton, plotHeight, plotWidth;
    private double xRange, yRange, xScale, yScale, pointSizeFactor;
    private AffineTransform pointTransformForZoom;
    private boolean hasTickLines, isVisibible, hasErrorBars, hasBaseLine, showNonValidPoints;
    private JCheckBoxMenuItem isVisibibleMenuItem, hasErrorBarsMenuItem, hasBaseLineMenuItem, hasTickLinesMenuItem, showNonValidPointsMenuItem;
    private JMenu plotMenu;
    private JPopupMenu plotPopupMenu;
    private JMenuItem yDataBandNameMenuItem, yDataRangeMenuItem;
    private RangeSlider horizontalRangeSlider, verticalRangeSlider;


    public Plot(String xAxisLabel) {
        this.xAxisLabel = xAxisLabel;
        pointSizeFactor = 1.8F;
        range = new Range();
        selectionAreaStartPoint = new Point2D.Double(0, 0);
        selectionAreaEndPoint = new Point2D.Double(0, 0);
        zoomStack = new Stack<>();
        clickedMouseButton = MouseEvent.BUTTON1;
        selectionOriginatesInOtherPlot = false;
        createPlotMenu();
        addMouseListener(this);
        addMouseMotionListener(this);
    }

    public void setHorizontalRageSlider(RangeSlider horizontalRangeSlider) {
        this.horizontalRangeSlider = horizontalRangeSlider;
    }

    public void setVerticalRageSlider(RangeSlider verticalRangeSlider) {
        this.verticalRangeSlider = verticalRangeSlider;
    }

    public JPopupMenu getPlotPopupMenu() {
        return this.plotPopupMenu;
    }

    private void createPlotMenu() {
        this.plotMenu = new JMenu();

        // Create plot menu
        // Band name
        this.yDataBandNameMenuItem = new JMenuItem();
        this.plotMenu.add(this.yDataBandNameMenuItem);
        this.plotMenu.addSeparator();

        // Max, Min Y axis values
        this.yDataRangeMenuItem = new JMenuItem();
        this.plotMenu.add(this.yDataRangeMenuItem);

        // Restore original range
        this.plotMenu.add(configureMenuItem(
                new JMenuItem(),
                GTk.Icon.PLOT_RESTORE_RANGES,
                "Restore original X-Y Range",
                GTk.NO_KEY_EVENT,
                this::restoreOriginalRanges
        ));
        // Change ranges
        this.plotMenu.add(configureMenuItem(
                new JMenuItem(),
                GTk.Icon.PLOT_CHANGE_RANGES,
                "Change X-Y Range",
                GTk.NO_KEY_EVENT,
                this::changeRangesDialog
        ));

        this.hasBaseLine = true;
        this.hasTickLines = true;
        this.hasErrorBars = false;
        this.isVisibible = true;
        this.showNonValidPoints = true;
        this.hasBaseLineMenuItem = new JCheckBoxMenuItem("Show base line", this.hasBaseLine);
        this.hasTickLinesMenuItem = new JCheckBoxMenuItem("Show tick lines", this.hasTickLines);
        this.hasErrorBarsMenuItem = new JCheckBoxMenuItem("Show error bars", this.hasErrorBars);
        this.showNonValidPointsMenuItem = new JCheckBoxMenuItem("Show non valid points", this.showNonValidPoints);
        this.isVisibibleMenuItem = new JCheckBoxMenuItem("Show plot", this.isVisibible);

        // Plot visibility
        this.isVisibibleMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleVisibilityMenuItem();
            }
        });
        this.plotMenu.add(this.isVisibibleMenuItem);

        // Base line visibility
        this.hasBaseLineMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleBaseLineMenuItem();
            }
        });
        this.plotMenu.add(this.hasBaseLineMenuItem);

        // Tick lines visibility
        this.hasTickLinesMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleTickLinesMenuItem();
            }
        });
        this.plotMenu.add(this.hasTickLinesMenuItem);

        // Error bars visibility
        this.hasErrorBarsMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleErrorBarsMenuItem();
            }
        });
        this.plotMenu.add(this.hasErrorBarsMenuItem);

        // Non valid points visibility
        this.showNonValidPointsMenuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toggleShowNonValidPointsMenuItem();
            }
        });
        this.plotMenu.add(this.showNonValidPointsMenuItem);

        this.plotPopupMenu = this.plotMenu.getPopupMenu();
    }

    private void changeRangesDialog(ActionEvent ignore) {
        String bandName = this.dataSet.id;
        double minx = this.range.min.x;
        double maxx = this.range.max.x;
        double miny = this.range.min.y;
        double maxy = this.range.max.y;
        RangeDialog.askForNewRanges(bandName, minx, maxx, miny, maxy, rangeValues -> {
            if (null != rangeValues) {
                double minx1 = rangeValues.min.x;
                double maxx1 = rangeValues.max.x;
                double miny1 = rangeValues.min.y;
                double maxy1 = rangeValues.max.y;
                changeXYRanges(minx1, maxx1, miny1, maxy1);
                adjustHorizontalRangeSlider(minx1, maxx1, false);
            }
        });
    }

    public void restoreOriginalRanges(ActionEvent ignore) {
        resetPlotRanges();
        repaint();
        if (null != this.horizontalRangeSlider) {
            this.horizontalRangeSlider.reset();
        }
    }

    public void toggleVisibilityMenuItem(boolean value) {
        this.isVisibible = value;
        this.isVisibibleMenuItem.setSelected(value);
    }

    private void toggleVisibilityMenuItem() {
        toggleVisibilityMenuItem(!this.isVisibible);
    }

    public void toggleErrorBarsMenuItem(boolean value) {
        this.hasErrorBars = value;
        this.hasErrorBarsMenuItem.setSelected(value);
        repaint();
    }

    private void toggleErrorBarsMenuItem() {
        toggleErrorBarsMenuItem(!this.hasErrorBars);
    }

    public void toggleShowNonValidPointsMenuItem(boolean value) {
        this.showNonValidPoints = value;
        this.showNonValidPointsMenuItem.setSelected(value);
        repaint();
    }

    private void toggleShowNonValidPointsMenuItem() {
        toggleShowNonValidPointsMenuItem(!this.showNonValidPoints);
    }

    public void toggleTickLinesMenuItem(boolean value) {
        this.hasTickLines = value;
        this.hasTickLinesMenuItem.setSelected(value);
        repaint();
    }

    private void toggleTickLinesMenuItem() {
        toggleTickLinesMenuItem(!this.hasTickLines);
    }

    public void toggleBaseLineMenuItem(boolean value) {
        this.hasBaseLine = value;
        this.hasBaseLineMenuItem.setSelected(value);
        repaint();
    }

    private void toggleBaseLineMenuItem() {
        toggleBaseLineMenuItem(!this.hasBaseLine);
    }

    @Override
    public boolean isVisible() {
        return this.isVisibible;
    }

    public void setXAxisUnits(String xAxisUnits) {
        this.xAxisLabel = xAxisUnits;
        repaint();
    }

    public DataSet getDataSet() {
        return this.dataSet;
    }

    public void setDataSet(DataSet dataSet) {
        this.dataSet = dataSet;
        this.plotMenu.setText(this.dataSet.id);
        this.yDataBandNameMenuItem.setText(String.format("Band name: %s", this.dataSet.id));
        this.yDataRangeMenuItem.setText(String.format(
                "Data range Y: [%s, %s]",
                AxisLabels.formatForYAxis(this.dataSet.minY), AxisLabels.formatForYAxis(this.dataSet.maxY)));

        // Plot ranges
        resetPlotRanges();
    }

    private static double getAxisExtraVisibilityDelta(double min, double max, double factor) {
        return Math.abs(max - min) * factor;
    }

    private double getXAxisExtraVisibilityDelta() {
        return (null != this.dataSet) ? getAxisExtraVisibilityDelta(this.dataSet.minX, this.dataSet.maxX, X_AXIS_EXTRA_VISIBILITY_DELTA) : 0.0F;
    }

    private double getYAxisExtraVisibilityDelta() {
        return (null != this.dataSet) ? getAxisExtraVisibilityDelta(this.dataSet.minY, this.dataSet.maxY, Y_AXIS_EXTRA_VISIBILITY_DELTA) : 0.0F;
    }

    private void resetPlotRanges() {
        if (null != this.dataSet) {
            double xdelta = getXAxisExtraVisibilityDelta();
            double ydelta = getYAxisExtraVisibilityDelta();
            this.range.min.x = this.dataSet.minX - xdelta;
            this.range.max.x = this.dataSet.maxX + xdelta;
            this.range.min.y = this.dataSet.minY - ydelta;
            this.range.max.y = this.dataSet.maxY + ydelta;
        }
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(
                RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON
        );
        g2.setRenderingHint(
                RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_SPEED
        );
        super.paintComponent(g2);
        drawCanvasXYAxisAndTicks(g2);
        drawCurve(g2);
        drawZoomRectangle(g2);
    }

    private void drawZoomRectangle(Graphics2D g2) {

        if (false == this.selectionAreaStartPoint.equals(this.selectionAreaEndPoint)) {
            g2.setColor(Color.GREEN);
            double startx = this.selectionAreaStartPoint.x;
            double endx = this.selectionAreaEndPoint.x;
            double y = this.selectionOriginatesInOtherPlot ? this.range.min.y + (this.yRange / 2.0F) : this.selectionAreaEndPoint.y;
            double len = 2.0F / this.yScale;
            g2.draw(new Line2D.Double(startx, y - len, startx, y + len));
            g2.draw(new Line2D.Double(startx, y, endx, y));
            g2.draw(new Line2D.Double(endx, y - len, endx, y + len));
        }
    }

    private void drawCurve(Graphics2D g2) {
        if (null != dataSet) {
            Points x = dataSet.xValues;
            Points y = dataSet.yValues;
            double xTick = pointSizeFactor / this.xScale;
            double yTick = pointSizeFactor / this.yScale;
            double xPointWidth = xTick * 2.0F;
            double yPointWidth = yTick * 2.0F;
            g2.setColor(dataSet.getColor());

            GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO, dataSet.getSize());
            boolean fistPointFound = false;
            for (int i = 0; i < dataSet.getSize(); i++) {
                if (hasBaseLine) {
                    if (false == fistPointFound) {
                        path.moveTo(x.get(i), y.get(i));
                        fistPointFound = true;
                    } else {
                        path.lineTo(x.get(i), y.get(i));
                    }
                }
                // The point
                g2.fill(new Ellipse2D.Double(x.get(i) - xTick, y.get(i) - yTick, xPointWidth, yPointWidth));
            }
            // Plot the graph
            if (hasBaseLine) {
                g2.draw(path);
            }
        }
    }

    private void drawCanvasXYAxisAndTicks(Graphics2D g2) {
        Dimension windowDimension = getSize();
        this.plotWidth = windowDimension.width - (PLOT_INSETS.left + PLOT_INSETS.right);
        this.plotHeight = windowDimension.height - (PLOT_INSETS.top + PLOT_INSETS.bottom);
        if (this.range.isUndefined()) {
            // When there is no data we don't know the plot range
            this.range.setMin(0.0F, 0.0F);
            this.range.setMax(this.plotWidth, this.plotHeight);
        }
        this.xRange = this.range.max.x - this.range.min.x;
        this.yRange = this.range.max.y - this.range.min.y;
        this.xScale = this.plotWidth / this.xRange;
        this.yScale = this.plotHeight / this.yRange;

        // Fill background and draw border around plot area.
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, windowDimension.width, windowDimension.height);
        g2.setColor(BORDER_COLOR);
        g2.drawRect(PLOT_INSETS.left, PLOT_INSETS.top, this.plotWidth, plotHeight);

        // Shift coordinate centre to bottom-left corner of the internal rectangle.
        g2.translate(PLOT_INSETS.left, windowDimension.height - PLOT_INSETS.bottom);

        // Draw ticks and tick labels
        drawTicksX(g2);
        drawTicksY(g2);
        drawAxisLabelsAndUnits(g2);

        // Scale the coordinate system to match plot coordinates
        this.pointTransformForZoom = g2.getTransform();
        this.pointTransformForZoom.scale(this.xScale, -1.0F * this.yScale);
        this.pointTransformForZoom.translate(-1.0F * this.range.min.x, -1.0F * this.range.min.y);
        try {
            this.pointTransformForZoom = this.pointTransformForZoom.createInverse();
        } catch (NoninvertibleTransformException ex) {
            System.err.println(ex.getMessage());
        }
        g2.scale(xScale, -yScale);
        g2.translate(-1.0F * this.range.min.x, -1.0F * this.range.min.y);

        // Draw only within plotting area
        g2.setClip(new Rectangle2D.Double(this.range.min.x, this.range.min.y, this.xRange, this.yRange));

        // Set stroke for curve and zoom
        g2.setStroke(new BasicStroke((float) Math.abs(1.0F / (100.0F * Math.max(this.xScale, this.yScale)))));
    }

    private void drawAxisLabelsAndUnits(Graphics2D g2) {
        FontMetrics fontMetrics = g2.getFontMetrics();
        char[] xAxisUnitsChars = this.xAxisLabel.toCharArray();
        int xAxisUnitsWidth = fontMetrics.charsWidth(xAxisUnitsChars, 0, xAxisUnitsChars.length);
        g2.setColor(UNITS_COLOR);
        g2.drawString(this.xAxisLabel, this.plotWidth - xAxisUnitsWidth, INSET_BOTTOM * 3 / 4);
        if (null != this.dataSet) {
            g2.drawString(
                    String.format(
                            "Zoom Range x:[%s, %s], y:[%s, %s]",
                            AxisLabels.formatForXAxis(this.range.min.x),
                            AxisLabels.formatForXAxis(this.range.max.x),
                            AxisLabels.formatForYAxis(this.range.min.y),
                            AxisLabels.formatForYAxis(this.range.max.y)),
                    0, Math.round(INSET_BOTTOM * 3 / 4));
        }
        // Draw Zero line
        int yPositionOfZero = yTickLabels != null ? yTickLabels.getYPositionOfZeroLabel() : -1;
        if (-1 != yPositionOfZero) {
            g2.setColor(Color.BLACK);
            g2.drawLine(0, yPositionOfZero, this.plotWidth, yPositionOfZero);
        }
    }

    private void drawTicksX(Graphics2D g2) {
        double xRangeTickInterval = this.xRange / X_RANGE_NUMBER_OF_TICKS;
        int[] xTickPositions = getTickPositions(this.range.min.x, this.xRange, this.xScale, xRangeTickInterval, false);
        if (null != xTickPositions) {
            this.xTickLabels = initLabels(g2, this.range.min.x, this.xRange, this.xScale, xRangeTickInterval, xTickPositions, AxisLabels.X_AXIS_SIGNIFICANT_FIGURES);
            int tickLength = this.xTickLabels.getTickLength();
            int labelVerticalPosition = tickLength + this.xTickLabels.getLabelHeight(0);
            BasicStroke stroke = (BasicStroke) g2.getStroke();
            BasicStroke dashedStroke = createDashedStroke(stroke);
            for (int i = 0; i < this.xTickLabels.getSize(); i++) {
                int pos = this.xTickLabels.getTickPosition(i);
                g2.drawLine(pos, 0, pos, tickLength);
                g2.drawString(this.xTickLabels.getLabel(i), pos - this.xTickLabels.getLabelWidth(i) / 2, labelVerticalPosition);
                g2.setStroke(dashedStroke);
                if (this.hasTickLines) {
                    g2.drawLine(pos, 0, pos, -this.plotHeight);
                }
                g2.setStroke(stroke);
            }
        }
    }

    private void drawTicksY(Graphics2D g2) {
        double yRangeTickInterval = this.yRange / Y_RANGE_NUMBER_OF_TICKS;
        int[] yTickPositions = getTickPositions(this.range.min.y, this.yRange, this.yScale, yRangeTickInterval, true);
        if (null != yTickPositions) {
            this.yTickLabels = initLabels(g2, this.range.min.y, this.yRange, this.yScale, yRangeTickInterval, yTickPositions, AxisLabels.Y_AXIS_SIGNIFICANT_FIGURES);
            int tickLength = this.yTickLabels.getTickLength();
            BasicStroke stroke = (BasicStroke) g2.getStroke();
            BasicStroke dashedStroke = createDashedStroke(stroke);
            for (int i = 0; i < this.yTickLabels.getSize(); i++) {
                int pos = this.yTickLabels.getTickPosition(i);
                g2.drawLine(0, pos, -tickLength, pos);
                g2.drawString(this.yTickLabels.getLabel(i), -(this.yTickLabels.getLabelWidth(i) + tickLength + 2), pos + this.yTickLabels.getLabelHeight(i) / 2 - 2);
                g2.setStroke(dashedStroke);
                if (this.hasTickLines) {
                    g2.drawLine(0, pos, this.plotWidth, pos);
                }
                g2.setStroke(stroke);
            }
        }
    }

    private AxisLabels initLabels(Graphics2D g2,
                                  double minValue,
                                  double range,
                                  double scale,
                                  double tickInterval,
                                  int[] tickPositions,
                                  int significantFigures) {
        double startValue = calculateStartValue(minValue, tickInterval);
        int tickNo = calculateTickNo(range, startValue, tickInterval);
        String[] labels = new String[tickNo];
        int[] labelWidths = new int[tickNo];
        int[] labelHeights = new int[tickNo];
        int tickLength = 10;
        FontMetrics fontMetrics = g2.getFontMetrics();
        for (int i = 0; i < tickNo; i++) {
            String label = AxisLabels.formatToSignificantFigures(startValue + i * tickInterval + minValue, significantFigures);
            Rectangle2D bounds = fontMetrics.getStringBounds(label, g2);
            labels[i] = label;
            labelWidths[i] = (int) bounds.getWidth();
            labelHeights[i] = (int) bounds.getHeight();
        }
        return new AxisLabels(labels, labelWidths, labelHeights, tickPositions, tickLength, significantFigures);
    }

    private int[] getTickPositions(double minPoint, double range, double scale, double tickInterval, boolean invert) {
        double start = calculateStartValue(minPoint, tickInterval);
        int tickNo = calculateTickNo(range, start, tickInterval);
        int[] tickPositions = null;
        if (tickNo > 0) {
            int inversionFactor = invert ? -1 : 1;
            tickPositions = new int[tickNo];
            for (int i = 0; i < tickNo; i++) {
                tickPositions[i] = inversionFactor * (int) ((start + i * tickInterval) * scale);
            }
        }
        return tickPositions;
    }

    private BasicStroke createDashedStroke(BasicStroke srcStroke) {
        return new BasicStroke(
                srcStroke.getLineWidth(),
                srcStroke.getEndCap(),
                srcStroke.getLineJoin(),
                srcStroke.getMiterLimit(),
                DASHED_LINE,
                0
        );
    }

    private double calculateStartValue(double minValue, double interval) {
        return (double) (Math.ceil(minValue / interval) * interval - minValue);
    }

    private int calculateTickNo(double range, double start, double interval) {
        return (int) (Math.abs(range - start) / interval + 1);
    }

    private void startMarkingSelectionArea(Point2D cursorPosition) {
        Point2D startPoint = this.pointTransformForZoom.transform(cursorPosition, null);
        this.selectionAreaStartPoint.setLocation(startPoint);
        this.selectionAreaEndPoint.setLocation(startPoint);
    }

    private void keepMarkingSelectionArea(Point2D cursorPosition) {
        Point2D.Double endPoint = (Point2D.Double) this.pointTransformForZoom.transform(cursorPosition, null);
        this.selectionAreaEndPoint = this.range.getInside((Point2D.Double) endPoint);
        repaint();
    }

    private boolean isInside(int x, int y) {
        return x >= INSET_LEFT && x <= INSET_LEFT + this.plotWidth && y >= INSET_TOP && y <= INSET_TOP + this.plotHeight;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        this.selectionAreaFirstPointIsInsidePlotArea = isInside(e.getX(), e.getY());
        if (this.selectionAreaFirstPointIsInsidePlotArea) {
            mousePressedAction(e, false);
        }
    }

    protected void mousePressedAction(MouseEvent e, boolean eventOriginatesInOtherPlot) {
        this.clickedMouseButton = e.getButton();
        this.selectionOriginatesInOtherPlot = eventOriginatesInOtherPlot;
        if (this.clickedMouseButton == MouseEvent.BUTTON1) {
            startMarkingSelectionArea(e.getPoint());
        }
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        if (this.selectionAreaFirstPointIsInsidePlotArea) {
            mouseDraggedAction(e, false);
        }
    }

    protected void mouseDraggedAction(MouseEvent e, boolean eventOriginatesInOtherPlot) {
        this.selectionOriginatesInOtherPlot = eventOriginatesInOtherPlot;
        if (this.clickedMouseButton == MouseEvent.BUTTON1) {
            keepMarkingSelectionArea(e.getPoint());
        }
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        if (this.selectionAreaFirstPointIsInsidePlotArea) {
            mouseReleasedAction(e, false);
        }
    }

    protected void mouseReleasedAction(MouseEvent e, boolean eventOriginatesInOtherPlot) {
        this.selectionOriginatesInOtherPlot = eventOriginatesInOtherPlot;
        if (this.clickedMouseButton == MouseEvent.BUTTON1) {
            this.selectionAreaEndPoint = this.selectionAreaStartPoint;
            repaint();
        }
    }

    private void zoomIn() {
        this.zoomStack.push((Range) this.range.clone());
        this.range.min.x = Math.min(this.selectionAreaStartPoint.x, this.selectionAreaEndPoint.x);
        this.range.max.x = Math.max(this.selectionAreaStartPoint.x, this.selectionAreaEndPoint.x);
        adjustYRangeToLocalMinMax();
        adjustHorizontalRangeSlider();
    }

    public void changeXYRanges(double minx, double maxx, double miny, double maxy) {
        this.zoomStack.push((Range) this.range.clone());
        this.range.min.x = minx;
        this.range.max.x = maxx;
        this.range.min.y = miny;
        this.range.max.y = maxy;
        repaint();
    }

    public void transformXRange(int minValue, int maxValue, int sliderMin, int sliderMax, boolean comesFromAnotherPlot) {
        double dataScale = (this.dataSet.maxX - this.dataSet.minX) / (sliderMax - sliderMin);
        double min = this.dataSet.minX + (minValue * dataScale);
        double max = this.dataSet.minX + (maxValue * dataScale);
        changeXRange(min, max);
        if (comesFromAnotherPlot) {
            if (null != this.horizontalRangeSlider) {
                this.horizontalRangeSlider.setLowValue(minValue);
                this.horizontalRangeSlider.setHighValue(maxValue);
            }
        }
    }

    public void transformMinXRange(int value, int sliderMin, int sliderMax, boolean comesFromAnotherPlot) {
        double dataScale = (this.dataSet.maxX - this.dataSet.minX) / (sliderMax - sliderMin);
        double min = this.dataSet.minX + (value * dataScale);
        changeXRange(min, this.range.max.x);
        if (comesFromAnotherPlot) {
            if (null != this.horizontalRangeSlider) {
                this.horizontalRangeSlider.setLowValue(value);
            }
        }
    }

    public void transformMaxXRange(int value, int sliderMin, int sliderMax, boolean comesFromAnotherPlot) {
        double dataScale = (this.dataSet.maxX - this.dataSet.minX) / (sliderMax - sliderMin);
        double max = this.dataSet.minX + (value * dataScale);
        changeXRange(this.range.min.x, max);
        if (comesFromAnotherPlot) {
            if (null != this.horizontalRangeSlider) {
                this.horizontalRangeSlider.setHighValue(value);
            }
        }
    }

    public void transformYRange(int minValue, int maxValue, int sliderMin, int sliderMax) {
        double dataScale = (this.dataSet.maxY - this.dataSet.minY) / (sliderMax - sliderMin);
        double min = this.dataSet.minY + (minValue * dataScale);
        double max = this.dataSet.minY + (maxValue * dataScale);
        System.out.println("--> min, max: " + min + ", " + max);
        changeYRange(min, max);
    }

    public void transformMinYRange(int value, int sliderMin, int sliderMax) {
        double dataScale = (this.dataSet.maxY - this.dataSet.minY) / (sliderMax - sliderMin);
        double min = this.dataSet.minY + (value * dataScale);
        System.out.println("--> min: " + min);
        changeYRange(min, this.range.max.y);
    }

    public void transformMaxYRange(int value, int sliderMin, int sliderMax) {
        double dataScale = (this.dataSet.maxY - this.dataSet.minY) / (sliderMax - sliderMin);
        double max = this.dataSet.minY + (value * dataScale);
        System.out.println("--> max: " + max);
        changeYRange(this.range.min.y, max);
    }

    public void adjustHorizontalRangeSlider(double minx, double maxx) {
        adjustHorizontalRangeSlider(minx, maxx, true);
    }

    private void adjustHorizontalRangeSlider(double minx, double maxx, boolean affectPlot) {
        if (null != this.horizontalRangeSlider) {
            int sliderMin = this.horizontalRangeSlider.getMin();
            int sliderMax = this.horizontalRangeSlider.getMax();
            double sliderScale = (sliderMax - sliderMin) / (this.dataSet.maxX - this.dataSet.minX);
            double fmin = sliderMin + ((minx - this.dataSet.minX) * sliderScale);
            double fmax = sliderMin + ((maxx - this.dataSet.minX) * sliderScale);
            this.horizontalRangeSlider.setLowValue((int) Math.round(fmin));
            this.horizontalRangeSlider.setHighValue((int) Math.round(fmax));
            if (affectPlot) {
                changeXRange(minx, maxx);
            }
        }
    }

    private void adjustHorizontalRangeSlider() {
        adjustHorizontalRangeSlider(this.range.min.x, this.range.max.x, false);
    }

    private void adjustYRangeToLocalMinMax() {
        if (null != this.dataSet) {
            double min = -1.0F, max = -1.0F, deltaY = -1.0F;
            System.out.println("this.verticalRangeSlider.isfullyStretched? " + this.verticalRangeSlider.isFullyStretched());
            System.out.println("min: " + verticalRangeSlider.getMin() + ", max: " + verticalRangeSlider.getMax() + ", minVal:" + verticalRangeSlider.getLowValue() + ", max val: " + verticalRangeSlider.getHighValue());

            boolean needsToAdjustToLocalMinMax = (null == this.verticalRangeSlider || (null != this.verticalRangeSlider && this.verticalRangeSlider.isFullyStretched()));
            if (needsToAdjustToLocalMinMax) {
                System.out.println("needsToAdjustToLocalMinMax");
                final double[] minMaxY = this.dataSet.getLocalMinMaxInYAxis(this.range.min.x, this.range.max.x);
                min = minMaxY[0];
                max = minMaxY[1];
            } else if (null == this.verticalRangeSlider) {
                System.out.println(">> adjusting to what the slider in the Y axis has to say");
                final int sliderMax = this.verticalRangeSlider.getMax();
                final int sliderMin = this.verticalRangeSlider.getMin();
                final int sliderMaxValue = sliderMax - this.verticalRangeSlider.getMin();
                final int sliderMinValue = sliderMax - this.verticalRangeSlider.getMax();
                final double dataScale = (this.dataSet.maxY - this.dataSet.minY) / (sliderMax - sliderMin);
                System.out.println("slider min, max: " + sliderMin + ", " + sliderMax);
                System.out.println("slider minValue, maxValue: " + sliderMinValue + ", " + sliderMaxValue);
                min = this.dataSet.minY + (sliderMinValue * dataScale);
                max = this.dataSet.minY + (sliderMaxValue * dataScale);
            }
            deltaY = getAxisExtraVisibilityDelta(min, max, Y_AXIS_EXTRA_VISIBILITY_DELTA);
            this.range.min.y = min - deltaY;
            this.range.max.y = max + deltaY;
        }
    }

    private void changeXRange(double minx, double maxx) {
        this.range.min.x = minx;
        this.range.max.x = maxx;
        adjustYRangeToLocalMinMax();
        repaint();
    }

    public void changeYRange(double miny, double maxy) {
        this.range.min.y = miny;
        this.range.max.y = maxy;
        repaint();
    }

    private void zoomOut() {
        if (false == this.zoomStack.isEmpty()) {
            this.range = zoomStack.pop();
            adjustHorizontalRangeSlider();
        } else if (null != this.horizontalRangeSlider) {
            restoreOriginalRanges(null);
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // Nothing needed to be done
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getButton() == MouseEvent.BUTTON3) {
            this.plotPopupMenu.show(e.getComponent(), e.getX(), e.getY());
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // Nothing needed to be done
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // Nothing needed to be done
    }

    public static void main(String[] args) throws Exception {
        Plot plot = new Plot("Time");
        plot.setXAxisUnits("micro");
        plot.setBackground(Color.WHITE);
        plot.setOpaque(true);

        Points xValues = new Points();
        Points yValues = new Points();

        double angle = Math.PI;
        double step = Math.PI / 120;
        for (int i = 0; i < 4000; i++) {
            xValues.addPoint(angle);
            yValues.addPoint(Math.sin(angle));
            angle += step;
        }
        xValues.done();
        yValues.done();


        DataSet dataSet = new DataSet("example", xValues, yValues);
        plot.setDataSet(dataSet);

        JFrame frame = GTk.createFrame("Plot", null);
        Dimension size = GTk.frameDimension(7.0F);
        frame.add(new RangedPlot(plot), BorderLayout.CENTER);
        Dimension location = GTk.frameLocation(size);
        frame.setLocation(location.width, location.height);
        frame.setVisible(true);
    }
}