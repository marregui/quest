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

import java.util.Arrays;

public class Points {
    private static final int SCALE = 100;

    private double[] points;
    private int offset;
    private int size;
    private double min, max;

    public Points() {
        points = new double[SCALE];
        offset = 0;
        size = SCALE;
    }

    public int getSize() {
        return offset;
    }

    public void addPoint(double value) {
        if (offset >= size) {
            double[] tmpPoints = new double[size + SCALE];
            System.arraycopy(points, 0, tmpPoints, 0, size);
            points = tmpPoints;
            size += SCALE;
        }
        points[offset++] = value;
    }

    public void done() {
        min = Float.MAX_VALUE;
        max = Float.MIN_VALUE;
        for (int i = 0; i < offset; i++) {
            min = Math.min(min, points[i]);
            max = Math.max(max, points[i]);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Points) {
            Points that = (Points) o;
            if (that.getSize() == getSize()) {
                for (int i = 0; i < getSize(); i++) {
                    if (that.get(i) != get(i)) {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(points);
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
}