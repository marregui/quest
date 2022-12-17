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

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.PlainDocument;
import java.awt.*;

public class ValuePanel extends JPanel {
    private static final int BUTTON_HEIGHT = 12;
    private static final int BUTTON_WIDTH = 42;

    private final double increment;
    private final Axis axis;
    private final JTextField text;
    private final JButton upButton, downButton;

    public ValuePanel(double value, Axis axis) {
        increment = getIncrement(axis);
        this.axis = axis;
        Dimension textDimension = new Dimension((int) Math.round(1.5F * BUTTON_WIDTH), 2 * BUTTON_HEIGHT);
        text = new JTextField();
        text.setSize(textDimension);
        text.setPreferredSize(textDimension);
        text.setDocument(new PlainDocument() {
            @Override
            public void insertString(int offs, String str, AttributeSet a) throws BadLocationException {
                String currentText = text.getText();
                int len = currentText.length();
                StringBuilder newValue = new StringBuilder(currentText.substring(0, offs));
                newValue.append(str);
                if (offs != len) {
                    newValue.append(currentText.substring(offs + 1));
                }
                try {
                    String newValueStr = newValue.toString();
                    if (null == newValueStr || newValueStr.isEmpty() || newValueStr.equals("-")) {
                        super.insertString(offs, str, a);
                    } else {
                        Float.valueOf(newValueStr); // make sure it is a number
                        super.insertString(offs, str, a);
                    }
                } catch (NumberFormatException nfe) {
                    // not a number
                }
            }
        });
        setValue(value);

        // Buttons
        Font buttonFont = new Font("Arial", Font.BOLD, 9);
        Dimension buttonSize = new Dimension(BUTTON_WIDTH, BUTTON_HEIGHT);
        upButton = new JButton("+");
        upButton.addActionListener(e -> increment());
        upButton.setSize(buttonSize);
        this.upButton.setPreferredSize(buttonSize);
        upButton.setFont(buttonFont);
        downButton = new JButton("-");
        downButton.addActionListener(e -> decrement());
        downButton.setSize(buttonSize);
        downButton.setPreferredSize(buttonSize);
        downButton.setFont(buttonFont);
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 1));
        buttonsPanel.add(upButton);
        buttonsPanel.add(downButton);

        setLayout(new BorderLayout());
        add(this.text, BorderLayout.CENTER);
        add(buttonsPanel, BorderLayout.EAST);
    }

    private static int significantFigures(Axis axis) {
        int significantFigures = -1;
        switch (axis) {
            case X:
                significantFigures = AxisLabels.X_AXIS_SIGNIFICANT_FIGURES;
                break;
            case Y:
                significantFigures = AxisLabels.Y_AXIS_SIGNIFICANT_FIGURES;
                break;
        }
        assert (significantFigures > 1);
        return significantFigures;
    }

    private static double getIncrement(Axis axis) {
        StringBuilder sb = new StringBuilder("0.");
        for (int i = 0; i < significantFigures(axis) - 1; i++) {
            sb.append("0");
        }
        sb.append("1");
        return Float.valueOf(sb.toString());
    }

    @Override
    public void setEnabled(boolean isEnabled) {
        super.setEnabled(isEnabled);
        text.setEnabled(isEnabled);
        upButton.setEnabled(isEnabled);
        downButton.setEnabled(isEnabled);
    }

    public double getValue() {
        String str = text.getText();
        if (null == str || str.isEmpty() || str.equals("-")) {
            setValue(0.0F);
            return 0.0F;
        }
        return Float.valueOf(str);
    }

    public void setValue(double value) {
        String str = null;
        switch (axis) {
            case X:
                str = AxisLabels.formatForXAxis(value);
                break;
            case Y:
                str = AxisLabels.formatForYAxis(value);
                break;
        }
        text.setText(str);
    }

    private void increment() {
        setValue(getValue() + increment);
    }

    private void decrement() {
        setValue(getValue() - increment);
    }
}
