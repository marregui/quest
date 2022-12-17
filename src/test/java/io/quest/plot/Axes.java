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


import java.awt.*;
import java.awt.geom.Rectangle2D;

public class Axes {
    public static final int X_AXIS_SIGNIFICANT_FIGURES = 3;
    public static final int Y_AXIS_SIGNIFICANT_FIGURES = 3;
    private static final String X_TPT = String.format("%%.%df", X_AXIS_SIGNIFICANT_FIGURES);
    private static final String Y_TPT = String.format("%%.%df", Y_AXIS_SIGNIFICANT_FIGURES);
    private final String labelZero;
    private final String[] labels;
    private final int[] labelWidths;
    private final int[] labelHeights;
    private final int[] tickPositions;
    private final int tickLength;


    private Axes(String[] labels, int[] labelWidths, int[] labelHeights, int[] tickPositions, int tickLength, String labelZero) {
        this.labels = labels;
        this.labelWidths = labelWidths;
        this.labelHeights = labelHeights;
        this.tickPositions = tickPositions;
        this.tickLength = tickLength;
        this.labelZero = labelZero;

    }

    public static Axes initXLabels(Graphics2D g2, double min, double range, double tickInterval, int[] tickPositions) {
        return initLabels(g2, min, range, tickInterval, tickPositions, X_TPT);
    }

    public static Axes initYLabels(Graphics2D g2, double min, double range, double tickInterval, int[] tickPositions) {
        return initLabels(g2, min, range, tickInterval, tickPositions, Y_TPT);
    }

    private static Axes initLabels(Graphics2D g2, double min, double range, double tickInterval, int[] tickPositions, String labelTpt) {
        double startValue = startValue(min, tickInterval);
        int tickNo = tickNo(range, startValue, tickInterval);
        String[] labels = new String[tickNo];
        int[] labelWidths = new int[tickNo];
        int[] labelHeights = new int[tickNo];
        int tickLength = 10;
        FontMetrics fm = g2.getFontMetrics();
        for (int i = 0; i < tickNo; i++) {
            String label = String.format(labelTpt, (startValue + i * tickInterval + min));
            Rectangle2D bounds = fm.getStringBounds(label, g2);
            labels[i] = label;
            labelWidths[i] = (int) bounds.getWidth();
            labelHeights[i] = (int) bounds.getHeight();
        }

        return new Axes(labels, labelWidths, labelHeights, tickPositions, tickLength, String.format(labelTpt, 0.0));
    }

    public static double startValue(double minValue, double interval) {
        return Math.ceil(minValue / interval) * interval - minValue;
    }

    public static int tickNo(double range, double start, double interval) {
        return (int) (Math.abs(range - start) / interval + 1);
    }

    public static String formatToSignificantFigures(double value, int significantFigures) {
        return String.format(significantFiguresTpt(significantFigures), value);
    }

    public static String formatX(double value) {
        return String.format(X_TPT, value);
    }

    public static String formatY(double value) {
        return String.format(Y_TPT, value);
    }

    private static String significantFiguresTpt(int significantFigures) {
        return String.format("%%.%df", significantFigures);
    }

    public int getYPositionOfZeroLabel() {
        for (int i = 0; i < labels.length; i++) {
            if (labelZero.equals(labels[i])) {
                return tickPositions[i];
            }
        }
        return -1;
    }

    public int getSize() {
        return labels.length;
    }

    public String getLabel(int n) {
        return labels[n];
    }

    public int getLabelWidth(int n) {
        return labelWidths[n];
    }

    public int getLabelHeight(int n) {
        return labelHeights[n];
    }

    public int getTickPosition(int n) {
        return tickPositions[n];
    }

    public int getTickLength() {
        return tickLength;
    }
}
