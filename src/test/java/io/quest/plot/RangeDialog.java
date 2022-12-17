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

import io.quest.frontend.GTk;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class RangeDialog extends JDialog {

    private static final RangeDialog DIALOG = new RangeDialog();
    private final AxisRangePanel xRange, yRange;
    private RangeValuesObserver callback;


    private RangeDialog() {
        xRange = new AxisRangePanel(Range.UNDEFINED, Range.UNDEFINED, Axis.X);
        yRange = new AxisRangePanel(Range.UNDEFINED, Range.UNDEFINED, Axis.Y);
        JPanel rangesPanel = new JPanel(new GridLayout(2, 1));
        rangesPanel.add(xRange);
        rangesPanel.add(yRange);
        JButton applyButton = new JButton("Apply");
        applyButton.addActionListener(this::applyButtonAction);
        JButton closeButton = new JButton("Close");
        closeButton.addActionListener(e -> setVisible(false));
        setSize(325, 200);
        setModalityType(ModalityType.DOCUMENT_MODAL);
        setAlwaysOnTop(true);
        setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        setLayout(new BorderLayout());
        add(rangesPanel, BorderLayout.CENTER);
        add(GTk.flowPanel(closeButton, applyButton), BorderLayout.SOUTH);
    }

    public static void askForNewRanges(
        String columnName,
        double minx,
        double maxx,
        double miny,
        double maxy,
        RangeValuesObserver callback
    ) {
        DIALOG.setRange(columnName, minx, maxx, miny, maxy, callback).setVisible(true);
    }

    private void applyButtonAction(ActionEvent ignore) {
        if (callback != null) {
            try {
                callback.rangeValuesChanged(new Range(xRange.getMin(), xRange.getMax(), yRange.getMin(), yRange.getMax()));
            } catch (Throwable t) {
                JOptionPane.showMessageDialog(
                    RangeDialog.this,
                    "Make sure the values are valid",
                    "Problem",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private RangeDialog setRange(String columnName, double minx, double maxx, double miny, double maxy, RangeValuesObserver callback) {
        setTitle(columnName);
        xRange.setMin(minx);
        xRange.setMax(maxx);
        yRange.setMin(miny);
        yRange.setMax(maxy);
        this.callback = callback;
        return this;
    }

    @FunctionalInterface
    public interface RangeValuesObserver {
        void rangeValuesChanged(Range range);
    }
}
