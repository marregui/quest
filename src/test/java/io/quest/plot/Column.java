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

public class Column {
    private static final int SCALE = 5000;

    private double[] points;
    private int offset;
    private int size;
    private double min, max;

    public Column() {
        points = new double[SCALE];
        offset = 0;
        size = SCALE;
        min = Double.MAX_VALUE;
        max = Double.MIN_VALUE;
    }

    public int size() {
        return offset;
    }

    public void append(double value) {
        if (offset >= size) {
            double[] tmpPoints = new double[size + SCALE];
            System.arraycopy(points, 0, tmpPoints, 0, size);
            points = tmpPoints;
            size += SCALE;
        }
        points[offset++] = value;
        min = Math.min(min, value);
        max = Math.max(max, value);
    }

    public double get(int i) {
        return points[i];
    }

    public double min() {
        return min;
    }

    public double max() {
        return max;
    }

    public double delta(double factor) {
        return Math.abs(max - min) * factor;
    }
}
