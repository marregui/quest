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

public class DataSet {
    public static final Color DEFAULT_COLOR = new Color(0x006699);
    private static final int DEFAULT_HASH_CODE = 0;
    public final String id;
    public final double minX, maxX, minY, maxY;
    public final Points xValues, yValues;
    private int hashCode;
    private Color color;

    public DataSet(String id, Points xValues, Points yValues) {
        this.id = id;
        this.xValues = xValues;
        this.yValues = yValues;
        minX = xValues.min();
        maxX = xValues.max();
        minY = yValues.min();
        maxY = yValues.max();
        hashCode = DEFAULT_HASH_CODE;
        color = DEFAULT_COLOR;
    }

    public double[] getLocalMinMaxInYAxis(double minx, double maxx) {
        boolean valuesFound = false;
        double miny = Float.MAX_VALUE;
        double maxy = Float.MIN_VALUE;
        if (maxx > minx) {
            for (int i = 0; i < getSize(); i++) {
                double x = xValues.get(i);
                if (x >= minx && x <= maxx) {
                    double y = yValues.get(i);
                    miny = Math.min(miny, y);
                    maxy = Math.max(maxy, y);
                    valuesFound = true;
                }
            }
        }
        if (!valuesFound) {
            miny = yValues.min();
            maxy = yValues.max();
        }
        return new double[]{miny, maxy};
    }

    public int getSize() {
        return yValues.getSize();
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof DataSet that) {
            return null != id && null != that.id && id.equals(that.id) &&
                yValues.equals(that.yValues) &&
                minY == that.minY &&
                maxY == that.maxY &&
                Math.abs(minX - that.minX) < 0.0001 &&
                Math.abs(maxX - that.maxX) < 0.0001;
        }
        return false;
    }

    @Override
    public int hashCode() {
        if (hashCode == DEFAULT_HASH_CODE) {
            hashCode = yValues.hashCode();
        }
        return hashCode;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public String toString() {
        return new StringBuilder()
            .append("point count: ")
            .append(yValues.getSize())
            .append(", X <")
            .append(minX)
            .append(", ")
            .append(maxX)
            .append(">, Y <")
            .append(minY)
            .append(", ")
            .append(maxY)
            .append(", \"")
            .append(id)
            .append("\">")
            .toString();
    }
}
