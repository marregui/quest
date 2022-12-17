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


public class Axes {
    public static final int X_AXIS_SIGNIFICANT_FIGURES = 1;
    public static final int Y_AXIS_SIGNIFICANT_FIGURES = 3;
    private final String labelFloatFormattingTpt;
    private final String[] labels;
    private final int[] labelWidths;
    private final int[] labelHeights;
    private final int[] tickPositions;
    private final int tickLength;

    public Axes(String[] labels, int[] labelWidths, int[] labelHeights, int[] tickPositions, int tickLength, int significantDecimals) {
        this.labels = labels;
        this.labelWidths = labelWidths;
        this.labelHeights = labelHeights;
        this.tickPositions = tickPositions;
        this.tickLength = tickLength;
        labelFloatFormattingTpt = getSignificantFiguresTpt(significantDecimals);
    }

    public static String formatToSignificantFigures(double value, int significantFigures) {
        return String.format(getSignificantFiguresTpt(significantFigures), value);
    }

    public static String formatX(double value) {
        return String.format(getSignificantFiguresTpt(X_AXIS_SIGNIFICANT_FIGURES), value);
    }

    public static String formatY(double value) {
        return String.format(getSignificantFiguresTpt(Y_AXIS_SIGNIFICANT_FIGURES), value);
    }

    private static String getSignificantFiguresTpt(int significantFigures) {
        return String.format("%%.%df", significantFigures);
    }

    public int getYPositionOfZeroLabel() {
        int position = -1;
        String zero = String.format(labelFloatFormattingTpt, 0.0F);
        for (int i = 0; i < labels.length; i++) {
            if (zero.equals(labels[i])) {
                position = tickPositions[i];
                break;
            }
        }
        return position;
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
