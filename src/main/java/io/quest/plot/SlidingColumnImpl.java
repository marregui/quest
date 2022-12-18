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

import java.util.Objects;

public class SlidingColumnImpl implements Column {

    private final double[] points;
    private final Object lock;
    private int writePtr;
    private int readPtr;
    private long appends;

    public SlidingColumnImpl(Object lock, int size) {
        this.lock = Objects.requireNonNull(lock);
        points = new double[size];
    }

    @Override
    public int size() {
        synchronized (lock) {
            return appends < points.length ? (int) appends : points.length;
        }
    }

    @Override
    public void append(double value) {
        synchronized (lock) {
            points[writePtr] = value;
            writePtr = (writePtr + 1) % points.length;
            if (readPtr > writePtr) {
                readPtr = writePtr;
            }
            appends++;
        }
    }

    @Override
    public double get(int i) {
        synchronized (lock) {
            return points[(readPtr + i) % points.length];
        }
    }

    @Override
    public double min() {
        synchronized (lock) {
            double min = Double.MAX_VALUE;
            for (int i = readPtr, n = size(); i < n; i++) {
                min = Math.min(min, points[i]);
            }
            return min;
        }

    }

    @Override
    public double max() {
        synchronized (lock) {
            double max = Double.MIN_VALUE;
            for (int i = readPtr, n = size(); i < n; i++) {
                max = Math.max(max, points[i]);
            }
            return max;
        }
    }
}
