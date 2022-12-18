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
import java.util.concurrent.TimeUnit;


public class SlidingPlotTest extends JPanel {

    public static void main(String[] args) {
        Plot plot = new Plot();

        int windowSize = 728;
        Column xValues = new SlidingColumnImpl(plot, windowSize);
        Column yValues = new SlidingColumnImpl(plot, windowSize);
        plot.setDataSet("Sin(∂) in stepts of π/4", xValues, yValues);
        Thread thread = new Thread(() -> {
            try {
                final double step = Math.PI / 90; // degrees to radians
                double angle = Math.PI;
                synchronized (plot) {
                    for (int i = 0; i < windowSize; i++) {
                        xValues.append(angle);
                        yValues.append(Math.sin(angle));
                        angle += step;
                    }
                }
                while (!Thread.currentThread().isInterrupted()) {
                    synchronized (plot) {
                        xValues.append(angle);
                        yValues.append(Math.sin(angle));
                    }
                    angle += step;
                    GTk.invokeLater(plot::repaint);
                    TimeUnit.MILLISECONDS.sleep(10L);
                }
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        });
        thread.setDaemon(true);
        thread.setName("Sensor");
        thread.start();

        JFrame frame = GTk.frame("Plot");
        Dimension size = GTk.frameDimension(7.0F, 7.0F);
        frame.add(plot, BorderLayout.CENTER);
        Dimension location = GTk.frameLocation(size);
        frame.setLocation(location.width, location.height);
        frame.setVisible(true);
    }
}
