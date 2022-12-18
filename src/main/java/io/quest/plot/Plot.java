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
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;


public class Plot extends JPanel {
    private static final float[] DASHED_LINE = new float[]{1, 8};
    private static final int INSET_TOP = 20;
    private static final int INSET_BOTTOM = 80;
    private static final int INSET_LEFT = 80;
    private static final int INSET_RIGHT = 20;
    private static final Insets PLOT_INSETS = new Insets(INSET_TOP, INSET_LEFT, INSET_BOTTOM, INSET_RIGHT);
    public Column xValues, yValues;
    private BasicStroke dashedStroke;
    private String title;

    public Plot() {
        setOpaque(true);
    }

    public void setDataSet(String title, Column xValues, Column yValues) {
        if (xValues.size() != yValues.size()) {
            throw new IllegalArgumentException("sizes must match and be greated than zero");
        }
        this.title = title;
        this.xValues = xValues;
        this.yValues = yValues;
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
        int plotHeight = height - (PLOT_INSETS.top + PLOT_INSETS.bottom);
        int plotWidth = width - (PLOT_INSETS.left + PLOT_INSETS.right);

        // Fill background and draw border around plot area.
        g2.setColor(GTk.QUEST_APP_BACKGROUND_COLOR);
        g2.fillRect(0, 0, width, height);
        g2.setColor(GTk.PLOT_BORDER_COLOR);
        g2.drawRect(PLOT_INSETS.left, PLOT_INSETS.top, plotWidth, plotHeight);

        // draw curve
        if (null != yValues) {

            double deltaX = xValues.delta(0.005F);
            double deltaY = yValues.delta(0.07F);
            double minX = xValues.min() - deltaX;
            double maxX = xValues.max() + deltaX;
            double minY = yValues.min() - deltaY;
            double maxY = yValues.max() + deltaY;
            double rangeX = maxX - minX;
            double rangeY = maxY - minY;

            // Shift coordinate centre to bottom-left corner of the internal rectangle.
            g2.translate(PLOT_INSETS.left, height - PLOT_INSETS.bottom);

            double scaleX = plotWidth / rangeX;
            double scaleY = plotHeight / rangeY;
            Axis x = Axis.forX(g2, minX, rangeX, scaleX);
            Axis y = Axis.forY(g2, minY, rangeY, scaleY);
            if (x == null || y == null) {
                return;
            }

            // Draw Zero line
            int yPositionOfZero = y.getYPositionOfZeroLabel();
            g2.drawLine(0, yPositionOfZero, plotWidth, yPositionOfZero);

            // Draw ticks and their labels
            int verticalPos = Axis.TICK_LENGTH + x.getHeight(0);
            BasicStroke stroke = (BasicStroke) g2.getStroke();
            if (dashedStroke == null) {
                dashedStroke = new BasicStroke(stroke.getLineWidth(), stroke.getEndCap(), stroke.getLineJoin(), stroke.getMiterLimit(), DASHED_LINE, 0);
            }
            for (int i = 0, n = x.size(); i < n; i++) {
                int pos = x.position(i);
                g2.setColor(GTk.PLOT_BORDER_COLOR);
                g2.drawLine(pos, 0, pos, Axis.TICK_LENGTH);
                g2.drawString(x.label(i), pos - x.width(i) / 2, verticalPos);
                g2.setColor(GTk.EDITOR_LINENO_COLOR);
                g2.setStroke(dashedStroke);
                g2.drawLine(pos, 0, pos, -plotHeight);
                g2.setStroke(stroke);
            }
            for (int i = 0, n = y.size(); i < n; i++) {
                int pos = y.position(i);
                g2.setColor(GTk.PLOT_BORDER_COLOR);
                g2.drawLine(0, pos, -Axis.TICK_LENGTH, pos);
                g2.drawString(y.label(i), -(y.width(i) + Axis.TICK_LENGTH + 2), pos + y.getHeight(i) / 2 - 2);
                if (i == 0 || i == n - 1 || y.isZero(i)) {
                    continue;
                }
                g2.setColor(GTk.EDITOR_LINENO_COLOR);
                g2.setStroke(dashedStroke);
                g2.drawLine(0, pos, plotWidth, pos);
                g2.setStroke(stroke);
            }

            // Draw title and ranges
            g2.setColor(GTk.EDITOR_FONT_COLOR);
            g2.drawString(String.format("%s x:[%s, %s], y:[%s, %s]", title != null ? title : "", Axis.fmtX(minX), Axis.fmtX(maxX), Axis.fmtY(minY), Axis.fmtY(maxY)), 0, Math.round(INSET_BOTTOM * 3 / 4.0F));

            // Scale the coordinate system to match plot coordinates
            g2.scale(scaleX, -scaleY);
            g2.translate(-1.0F * minX, -1.0F * minY);

            // Draw only within plotting area
            g2.setClip(new Rectangle2D.Double(minX, minY, rangeX, rangeY));

            // Set stroke for curve
            g2.setStroke(new BasicStroke((float) Math.abs(1.0F / (100.0F * Math.max(scaleX, scaleY)))));
            double pointSizeFactor = 1.2;
            double xTick = pointSizeFactor / scaleX;
            double yTick = pointSizeFactor / scaleY;
            double xPointWidth = xTick * 2.0F;
            double yPointWidth = yTick * 2.0F;
            g2.setColor(GTk.SELECT_FONT_COLOR);
            int n = yValues.size();
            double px = xValues.get(0);
            double py = yValues.get(0);
            GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO, n);
            path.moveTo(px, py);
            g2.fill(new Ellipse2D.Double(px - xTick, py - yTick, xPointWidth, yPointWidth));
            for (int i = 1; i < n; i++) {
                px = xValues.get(i);
                g2.setColor(GTk.SELECT_FONT_COLOR);
                py = yValues.get(i);
                path.lineTo(px, py);
                g2.fill(new Ellipse2D.Double(px - xTick, py - yTick, xPointWidth, yPointWidth));
            }
            g2.setColor(GTk.QUEST_APP_COLOR);
            g2.draw(path);
        }
    }
}
