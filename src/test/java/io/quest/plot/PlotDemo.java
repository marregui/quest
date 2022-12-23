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
 * Copyright (c) 2019 - 2023, Miguel Arregui a.k.a. marregui
 */

package io.quest.plot;

import io.quest.GTk;

import javax.swing.*;
import java.awt.*;


public class PlotDemo extends JPanel {

    public static void main(String[] args) {
        Plot plot = new Plot();

        Column xValues = new ColumnImpl("x");
        Column yValues = new ColumnImpl("y", Color.BLUE);
        double angle = Math.PI;
        double step = Math.PI / 90; // degrees to radians
        int n = (int) ((1.0 + Math.sqrt(5.0)) * 314);
        for (int i = 0; i < n; i++) {
            xValues.append(angle);
            double sin = Math.sin(angle);
            double cos = Math.cos(angle);
            yValues.append(sin * sin + cos * cos);
            angle += step;
        }
        plot.setDataSet("Sin(∂), Tan(∂) in steps of π/4", xValues, yValues);

        JFrame frame = GTk.frame("Plot");
        Dimension size = GTk.frameDimension(7.0F, 7.0F);
        frame.add(plot, BorderLayout.CENTER);
        Dimension location = GTk.frameLocation(size);
        frame.setLocation(location.width, location.height);
        frame.setVisible(true);
    }

}
