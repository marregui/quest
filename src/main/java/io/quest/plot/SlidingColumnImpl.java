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

public class SlidingColumnImpl implements Column {

    private final double[] points;
    private int writePtr;
    private int readPtr;

    public SlidingColumnImpl(int size) {
        points = new double[size];
    }

    @Override
    public int size() {
        return points.length;
    }

    @Override
    public void append(double value) {
        synchronized (points) {
            points[writePtr] = value;
            writePtr = (writePtr + 1) % points.length;
        }
    }

    @Override
    public double get(int i) {
        synchronized (points) {
            if (readPtr > writePtr) {
                readPtr = (writePtr + 1) % points.length;
            }
            return points[readPtr + i];
        }
    }

    @Override
    public double min() {
        synchronized (points) {
            double min = Double.MAX_VALUE;
            for (int i = readPtr, n = size(); i < n; i++) {
                min = Math.min(min, points[i]);
            }
            return min;
        }

    }

    @Override
    public double max() {
        synchronized (points) {
            double max = Double.MIN_VALUE;
            for (int i = readPtr, n = size(); i < n; i++) {
                max = Math.max(max, points[i]);
            }
            return max;
        }
    }
}
