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


import java.awt.*;
import java.awt.event.MouseEvent;


public class VerticalRangeSlider extends RangeSlider {
    public VerticalRangeSlider(int minimum, int maximum, RangeChangedObserver callback) {
        super(minimum, maximum, callback);
    }

    @Override
    protected int getRelevantCoordinate(MouseEvent e) {
        return e.getY();
    }

    @Override
    protected void customRender(Graphics2D g2, int width, int height, int min, int max) {
        // Draw arrow and thumb backgrounds
        g2.setStroke(new BasicStroke(1));
        g2.setColor(getForeground());
        g2.fillRect(0, min - ARROW_SZ, width, ARROW_SZ - 1);
        paint3DRectLighting(g2, 0, min - ARROW_SZ, width, ARROW_SZ - 1);

        g2.setColor(isFullyStretched() ? CENTER_AREA_COLOR : ALT_CENTER_AREA_COLOR);
        g2.fillRect(0, min, width, max - min - 1);
        paint3DRectLighting(g2, 0, min, width, max - min - 1);

        g2.setColor(getForeground());
        g2.fillRect(0, max, width, ARROW_SZ - 1);
        paint3DRectLighting(g2, 0, max, width, ARROW_SZ - 1);

        // Draw arrows
        g2.setColor(Color.black);
        paintArrow(g2, (width - ARROW_WIDTH) / 2.0, min - ARROW_SZ + (ARROW_SZ - ARROW_HEIGHT) / 2.0, ARROW_WIDTH, ARROW_HEIGHT, true);
        paintArrow(g2, (width - ARROW_WIDTH) / 2.0, max + (ARROW_SZ - ARROW_HEIGHT) / 2.0, ARROW_WIDTH, ARROW_HEIGHT, false);
    }

    @Override
    protected void paintArrow(Graphics2D g2, double x, double y, int w, int h, boolean topDown) {
        int intX = (int) (x + 0.5);
        int intY = (int) (y + 0.5);

        if (w % 2 == 0) {
            w = w - 1;
        }

        if (topDown) {
            for (int i = 0; i < (w / 2 + 1); i++) {
                g2.drawLine(intX + i, intY + i, intX + w - i - 1, intY + i);
            }
        } else {
            for (int i = 0; i < (w / 2 + 1); i++) {
                g2.drawLine(intX + w / 2 - i, intY + i, intX + w - w / 2 + i - 1, intY + i);
            }
        }
    }

    @Override
    protected int toLocal(int xOrY) {
        Dimension sz = getSize();
        int min = model.getMinimum();
        double scale = (sz.height - (2 * ARROW_SZ)) / (double) (model.getMaximum() - min);
        return (int) (((xOrY - ARROW_SZ) / scale) + min + 0.5);
    }

    @Override
    protected int toScreen(int xOrY) {
        Dimension sz = getSize();
        int min = model.getMinimum();
        double scale = (sz.height - (2 * ARROW_SZ)) / (double) (model.getMaximum() - min);
        return (int) (ARROW_SZ + ((xOrY - min) * scale) + 0.5);
    }
}
