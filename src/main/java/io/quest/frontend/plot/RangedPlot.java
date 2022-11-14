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

import javax.swing.*;
import java.awt.*;

public class RangedPlot extends JPanel {
    private static final int MIN = 0;
    private static final int MAX = 100;


    public final Plot plot;
    public final RangeSlider horizontalSlider, verticalSlider;

    public RangedPlot(Plot plot) {
        this.plot = plot;
        horizontalSlider = RangeSlider.horizontalRangeSlider(MIN, MAX, (minValue, maxValue, sliderSide) -> {
            switch (sliderSide) {
                case NONE:
                    break;

                case TOP_OR_LEFT:
                    plot.transformMinXRange(minValue, MIN, MAX, false);
                    break;

                case BOTTOM_OR_RIGHT:
                    plot.transformMaxXRange(maxValue, MIN, MAX, false);
                    break;

                case THUMB:
                    plot.transformXRange(minValue, maxValue, MIN, MAX, false);
                    break;
            }
        });
        plot.setHorizontalRageSlider(horizontalSlider);
        verticalSlider = RangeSlider.verticalRangeSlider(MIN, MAX, (minValue, maxValue, sliderSide) -> {
            final int max = MAX - minValue;
            final int min = MAX - maxValue;
            switch (sliderSide) {
                case NONE:
                    break;

                case TOP_OR_LEFT:
                    plot.transformMaxYRange(max, MIN, MAX);
                    break;

                case BOTTOM_OR_RIGHT:
                    plot.transformMinYRange(min, MIN, MAX);
                    break;

                case THUMB:
                    plot.transformYRange(min, max, MIN, MAX);
                    break;
            }
        });
        plot.setVerticalRageSlider(verticalSlider);

        JPanel horizontalSliderPanel = new JPanel(new BorderLayout());
        horizontalSliderPanel.add(horizontalSlider, BorderLayout.CENTER);
        horizontalSliderPanel.setPreferredSize(new Dimension(8, 8));
        JPanel verticalSliderPanel = new JPanel(new BorderLayout());
        verticalSliderPanel.add(verticalSlider, BorderLayout.CENTER);
        verticalSliderPanel.setPreferredSize(new Dimension(8, 8));
        setLayout(new BorderLayout());
        add(plot, BorderLayout.CENTER);
        add(verticalSliderPanel, BorderLayout.WEST);
        add(horizontalSliderPanel, BorderLayout.SOUTH);
    }
}