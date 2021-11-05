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

package io.quest.frontend.commands;

import io.quest.backend.StoreEntry;


/**
 * Persistent {@link CommandBoard} content.
 */
public class Content extends StoreEntry {

    private static final String ATTR_NAME = "content";

    public Content() {
        this("default");
    }

    public Content(String name) {
        super(name);
        setAttr(ATTR_NAME,
                "\n" +
                        "\n" +
                        "\n" +
                        ".                              _\n" +
                        "   __ _   _   _    ___   ___  | |_\n" +
                        "  / _` | | | | |  / _ \\ / __| | __|\n" +
                        " | (_| | | |_| | |  __/ \\__ \\ | |_\n" +
                        "  \\__, |  \\__,_|  \\___| |___/  \\__|\n" +
                        "     |_|\n" +
                        "\n" +
                        "  Copyright (c) 2019 - 2022\n" +
                        "  Miguel Arregui a.k.a. marregui\n"
        );
    }

    /**
     * Shallow copy constructor, used by the store, attributes are a reference to
     * the attributes of 'other'.
     *
     * @param other original store item
     */
    public Content(StoreEntry other) {
        super(other);
    }

    @Override
    public final String getKey() {
        return getName();
    }

    public String getContent() {
        return getAttr(ATTR_NAME);
    }

    public void setContent(String content) {
        setAttr(ATTR_NAME, content);
    }
}
