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

package io.quest.plot;

import io.quest.GTk;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.*;


public class Plot extends JPanel {
    private static final Color BORDER_COLOR = new Color(153, 153, 153);
    private static final float[] DASHED_LINE = new float[]{1, 8};
    private static final int X_RANGE_NUMBER_OF_TICKS = 15;
    private static final int Y_RANGE_NUMBER_OF_TICKS = 10;
    private static final int INSET_TOP = 20;
    private static final int INSET_BOTTOM = 80;
    private static final int INSET_LEFT = 80;
    private static final int INSET_RIGHT = 20;
    private static final Insets PLOT_INSETS = new Insets(INSET_TOP, INSET_LEFT, INSET_BOTTOM, INSET_RIGHT);
    private final double POINT_SIZE_FACTOR = (1.0 + Math.sqrt(5.0)) / 2;
    public Column xValues, yValues;
    private double minX, minY, maxX, maxY, rangeX, rangeY;
    private Rectangle2D.Double clip;
    private GeneralPath path;

    private Axes yTickLabels;
    private int plotHeight, plotWidth;

    public Plot() {
        setOpaque(true);
    }

    public static void main(String[] args) {
        Plot plot = new Plot();

        Column xValues = new Column();
        Column yValues = new Column();
        double angle = Math.PI;
        double step = Math.PI / 90; // degrees to radians
        int n = (int) ((1.0 + Math.sqrt(5.0)) * 314);
        for (int i = 0; i < n; i++) {
            xValues.append(angle);
            yValues.append(Math.sin(angle));
            angle += step;
        }
        plot.setDataSet(xValues, yValues);

        JFrame frame = GTk.frame("Plot");
        Dimension size = GTk.frameDimension(7.0F, 7.0F);
        frame.add(plot, BorderLayout.CENTER);
        Dimension location = GTk.frameLocation(size);
        frame.setLocation(location.width, location.height);
        frame.setVisible(true);
    }

    private static Axes initLabels(Graphics2D g2, double minValue, double range, double tickInterval, int[] tickPositions, int significantFigures) {
        double startValue = startValue(minValue, tickInterval);
        int tickNo = tickNo(range, startValue, tickInterval);
        String[] labels = new String[tickNo];
        int[] labelWidths = new int[tickNo];
        int[] labelHeights = new int[tickNo];
        int tickLength = 10;
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < tickNo; i++) {
            String label = Axes.formatToSignificantFigures(startValue + i * tickInterval + minValue, significantFigures);
            Rectangle2D bounds = fm.getStringBounds(label, g2);
            labels[i] = label;
            labelWidths[i] = (int) bounds.getWidth();
            labelHeights[i] = (int) bounds.getHeight();
        }
        return new Axes(labels, labelWidths, labelHeights, tickPositions, tickLength, significantFigures);
    }

    private static int[] getTickPositions(double min, double range, double scale, double tickInterval, boolean invert) {
        double start = startValue(min, tickInterval);
        int tickNo = tickNo(range, start, tickInterval);
        if (tickNo > 0) {
            int sign = invert ? -1 : 1;
            int[] tickPositions = new int[tickNo];
            for (int i = 0; i < tickNo; i++) {
                tickPositions[i] = sign * (int) ((start + i * tickInterval) * scale);
            }
            return tickPositions;
        }
        return null;
    }

    private static BasicStroke createDashedStroke(BasicStroke srcStroke) {
        return new BasicStroke(srcStroke.getLineWidth(), srcStroke.getEndCap(), srcStroke.getLineJoin(), srcStroke.getMiterLimit(), DASHED_LINE, 0);
    }

    private static double startValue(double minValue, double interval) {
        return Math.ceil(minValue / interval) * interval - minValue;
    }

    private static int tickNo(double range, double start, double interval) {
        return (int) (Math.abs(range - start) / interval + 1);
    }

    public void setDataSet(Column xValues, Column yValues) {
        this.xValues = xValues;
        this.yValues = yValues;
        double deltaX = xValues.delta(0.005F);
        double deltaY = yValues.delta(0.07F);
        minX = xValues.min() - deltaX;
        maxX = xValues.max() + deltaX;
        minY = yValues.min() - deltaY;
        maxY = yValues.max() + deltaY;
        rangeX = maxX - minX;
        rangeY = maxY - minY;
        clip = new Rectangle2D.Double(minX, minY, rangeX, rangeY);
        int n = yValues.getSize();
        path = new GeneralPath(GeneralPath.WIND_NON_ZERO, n);
        path.moveTo(xValues.get(0), yValues.get(0));
        for (int i = 1; i < n; i++) {
            path.lineTo(xValues.get(i), yValues.get(i));
        }
        repaint();
    }

    @Override
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        super.paintComponent(g2);

        int height = getHeight();
        int width = getWidth();
        plotWidth = width - (PLOT_INSETS.left + PLOT_INSETS.right);
        plotHeight = height - (PLOT_INSETS.top + PLOT_INSETS.bottom);

        // Fill background and draw border around plot area.
        g2.setColor(GTk.QUEST_APP_BACKGROUND_COLOR);
        g2.fillRect(0, 0, width, height);
        g2.setColor(BORDER_COLOR);
        g2.drawRect(PLOT_INSETS.left, PLOT_INSETS.top, plotWidth, plotHeight);

        // draw curve
        if (null != yValues) {
            double scaleX = plotWidth / rangeX;
            double scaleY = plotHeight / rangeY;

            // Shift coordinate centre to bottom-left corner of the internal rectangle.
            g2.translate(PLOT_INSETS.left, height - PLOT_INSETS.bottom);

            // Draw ticks and tick labels
            drawTicksX(g2, scaleX);
            drawTicksY(g2, scaleY);
            g2.drawString(String.format("Ranges x:[%s, %s], y:[%s, %s]", Axes.formatX(minX), Axes.formatX(maxX), Axes.formatY(minY), Axes.formatY(maxY)), 0, Math.round(INSET_BOTTOM * 3 / 4.0F));

            // Draw Zero line
            int yPositionOfZero = yTickLabels.getYPositionOfZeroLabel();
            g2.drawLine(0, yPositionOfZero, plotWidth, yPositionOfZero);

            // Scale the coordinate system to match plot coordinates
            g2.scale(scaleX, -scaleY);
            g2.translate(-1.0F * minX, -1.0F * minY);

            // Draw only within plotting area
            g2.setClip(clip);

            // Set stroke for curve
            g2.setStroke(new BasicStroke((float) Math.abs(1.0F / (100.0F * Math.max(scaleX, scaleY)))));
            double xTick = POINT_SIZE_FACTOR / scaleX;
            double yTick = POINT_SIZE_FACTOR / scaleY;
            double xPointWidth = xTick * 2.0F;
            double yPointWidth = yTick * 2.0F;
            g2.setColor(GTk.QUEST_APP_COLOR);
            int n = yValues.getSize();
            g2.fill(new Ellipse2D.Double(xValues.get(0) - xTick, yValues.get(0) - yTick, xPointWidth, yPointWidth));
            for (int i = 1; i < n; i++) {
                g2.fill(new Ellipse2D.Double(xValues.get(i) - xTick, yValues.get(i) - yTick, xPointWidth, yPointWidth));
            }
            g2.draw(path);
        }
    }

    private void drawTicksX(Graphics2D g2, double scaleX) {
        double xRangeTickInterval = rangeX / X_RANGE_NUMBER_OF_TICKS;
        int[] xTickPositions = getTickPositions(minX, rangeX, scaleX, xRangeTickInterval, false);
        if (null != xTickPositions) {
            Axes xTickLabels = initLabels(g2, minX, rangeX, xRangeTickInterval, xTickPositions, Axes.X_AXIS_SIGNIFICANT_FIGURES);
            int tickLength = xTickLabels.getTickLength();
            int labelVerticalPosition = tickLength + xTickLabels.getLabelHeight(0);
            BasicStroke stroke = (BasicStroke) g2.getStroke();
            BasicStroke dashedStroke = createDashedStroke(stroke);
            for (int i = 0; i < xTickLabels.getSize(); i++) {
                int pos = xTickLabels.getTickPosition(i);
                g2.drawLine(pos, 0, pos, tickLength);
                g2.drawString(xTickLabels.getLabel(i), pos - xTickLabels.getLabelWidth(i) / 2, labelVerticalPosition);
                g2.setStroke(dashedStroke);
                g2.drawLine(pos, 0, pos, -plotHeight);
                g2.setStroke(stroke);
            }
        }
    }

    private void drawTicksY(Graphics2D g2, double scaleY) {
        double yRangeTickInterval = rangeY / Y_RANGE_NUMBER_OF_TICKS;
        int[] yTickPositions = getTickPositions(minY, rangeY, scaleY, yRangeTickInterval, true);
        if (null != yTickPositions) {
            yTickLabels = initLabels(g2, minY, rangeY, yRangeTickInterval, yTickPositions, Axes.Y_AXIS_SIGNIFICANT_FIGURES);
            int tickLength = yTickLabels.getTickLength();
            BasicStroke stroke = (BasicStroke) g2.getStroke();
            BasicStroke dashedStroke = createDashedStroke(stroke);
            for (int i = 0; i < yTickLabels.getSize(); i++) {
                int pos = yTickLabels.getTickPosition(i);
                g2.drawLine(0, pos, -tickLength, pos);
                g2.drawString(yTickLabels.getLabel(i), -(yTickLabels.getLabelWidth(i) + tickLength + 2), pos + yTickLabels.getLabelHeight(i) / 2 - 2);
                g2.setStroke(dashedStroke);
                g2.drawLine(0, pos, plotWidth, pos);
                g2.setStroke(stroke);
            }
        }
    }
}
