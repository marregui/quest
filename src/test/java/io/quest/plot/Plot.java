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

    private static BasicStroke createDashedStroke(BasicStroke srcStroke) {
        return new BasicStroke(srcStroke.getLineWidth(), srcStroke.getEndCap(), srcStroke.getLineJoin(), srcStroke.getMiterLimit(), DASHED_LINE, 0);
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
        int plotWidth = width - (PLOT_INSETS.left + PLOT_INSETS.right);
        int plotHeight = height - (PLOT_INSETS.top + PLOT_INSETS.bottom);

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
            Axis xTickLabels = Axis.forX(g2, minX, rangeX, scaleX);
            if (null != xTickLabels) {
                int labelVerticalPosition = Axis.TICK_LENGTH + xTickLabels.getHeight(0);
                BasicStroke stroke = (BasicStroke) g2.getStroke();
                BasicStroke dashedStroke = createDashedStroke(stroke);
                for (int i = 0; i < xTickLabels.getSize(); i++) {
                    int pos = xTickLabels.getPosition(i);
                    g2.drawLine(pos, 0, pos, Axis.TICK_LENGTH);
                    g2.drawString(xTickLabels.getLabel(i), pos - xTickLabels.getWidth(i) / 2, labelVerticalPosition);
                    g2.setStroke(dashedStroke);
                    g2.drawLine(pos, 0, pos, -plotHeight);
                    g2.setStroke(stroke);
                }
            }

            Axis yTickLabels = Axis.forY(g2, minY, rangeY, scaleY);
            if (null != yTickLabels) {
                BasicStroke stroke = (BasicStroke) g2.getStroke();
                BasicStroke dashedStroke = createDashedStroke(stroke);
                for (int i = 0; i < yTickLabels.getSize(); i++) {
                    int pos = yTickLabels.getPosition(i);
                    g2.drawLine(0, pos, -Axis.TICK_LENGTH, pos);
                    g2.drawString(yTickLabels.getLabel(i), -(yTickLabels.getWidth(i) + Axis.TICK_LENGTH + 2), pos + yTickLabels.getHeight(i) / 2 - 2);
                    g2.setStroke(dashedStroke);
                    g2.drawLine(0, pos, plotWidth, pos);
                    g2.setStroke(stroke);
                }
            }
            g2.drawString(String.format("Ranges x:[%s, %s], y:[%s, %s]", Axis.formatX(minX), Axis.formatX(maxX), Axis.formatY(minY), Axis.formatY(maxY)), 0, Math.round(INSET_BOTTOM * 3 / 4.0F));

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
}
