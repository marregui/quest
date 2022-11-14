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
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AxisRangePanel extends JPanel {
    @FunctionalInterface
    public interface RangeHasBeenChanged {
        void newRangeValues(double min, double max, Axis axis);
    }

    private final Axis axis;
    private final ValuePanel minValue, maxValue;
    private final JButton applyButton;
    private final List<RangeHasBeenChanged> rangeChangeObservers;

    public AxisRangePanel(double min, double max, Axis axis) {
        this.axis = axis;
        rangeChangeObservers = new CopyOnWriteArrayList<>();
        JLabel minLabel = new JLabel("min: ");
        minValue = new ValuePanel(min, axis);
        JLabel maxLabel = new JLabel("max: ");
        maxValue = new ValuePanel(max, axis);
        applyButton = new JButton("Apply");
        applyButton.addActionListener(e -> notifyRangeChangeObservers());
        setBorder(BorderFactory.createTitledBorder(String.format("%s Axis", axis)));
        setLayout(new FlowLayout(FlowLayout.LEFT));
        add(minLabel);
        add(minValue);
        add(maxLabel);
        add(maxValue);
        add(applyButton);
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        super.setEnabled(isEnabled);
        minValue.setEnabled(isEnabled);
        maxValue.setEnabled(isEnabled);
        applyButton.setEnabled(isEnabled);
    }

    public void setMin(double min) {
        minValue.setValue(min);
    }

    public double getMin() {
        return minValue.getValue();
    }

    public void setMax(double max) {
        maxValue.setValue(max);
    }

    public double getMax() {
        return maxValue.getValue();
    }

    public void addRangeChangeObserver(RangeHasBeenChanged observer) {
        rangeChangeObservers.add(observer);
    }

    private void notifyRangeChangeObservers() {
        double min = getMin();
        double max = getMax();
        if (min >= max) {
            JOptionPane.showMessageDialog(
                    this,
                    "Max value must be greater than Min value",
                    "Range problem",
                    JOptionPane.ERROR_MESSAGE);
        } else {
            for (RangeHasBeenChanged observer : rangeChangeObservers) {
                observer.newRangeValues(min, max, axis);
            }
        }
    }
}
