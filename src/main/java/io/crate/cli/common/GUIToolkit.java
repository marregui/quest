/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */
package io.crate.cli.common;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;


public final class GUIToolkit {

    static {
        // antialiased fonts
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
    }

    public static final String JDBC_DRIVER_URL_FORMAT = "jdbc:postgresql://%s:%s/";

    public static final String MAIN_FONT_NAME = "Arial";
    public static final Color CRATE_COLOR = new Color(66, 188, 245);

    public static final String ERROR_HEADER = "======= Error =======\n";
    public static final Font ERROR_FONT = new Font(MAIN_FONT_NAME, Font.BOLD, 14);
    public static final Color ERROR_FONT_COLOR = new Color(189, 4, 4); // blood

    public static final Font TABLE_HEADER_FONT = new Font(MAIN_FONT_NAME, Font.BOLD, 18);
    public static final Color TABLE_HEADER_FONT_COLOR = Color.BLACK;
    public static final Font TABLE_CELL_FONT = new Font(MAIN_FONT_NAME, Font.PLAIN, 16);


    public static Dimension frameDimension() {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        int width = (int) (screenSize.getWidth() * 0.9);
        int height = (int) (screenSize.getHeight() * 0.9);
        return new Dimension(width, height);
    }

    public static Dimension frameLocation(Dimension frameDimension) {
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension screenSize = tk.getScreenSize();
        int x = (int) (screenSize.getWidth() - frameDimension.getWidth()) / 2;
        int y = (int) (screenSize.getHeight() - frameDimension.getHeight()) / 2;
        return new Dimension(x, y);
    }

    private GUIToolkit() {
        throw new IllegalStateException("not meant to me instantiated");
    }
}
