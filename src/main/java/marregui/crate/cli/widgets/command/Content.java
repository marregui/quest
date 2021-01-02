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
 * Copyright 2020, Miguel Arregui a.k.a. marregui
 */

package marregui.crate.cli.widgets.command;

import marregui.crate.cli.backend.StoreEntry;


/**
 * Persistent {@link CommandBoard} content.
 */
public class Content extends StoreEntry {

    private static final String ATTR_NAME = "content";

    public Content() {
        super(CommandBoard.class.getSimpleName());
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
        return getAttribute(ATTR_NAME);
    }

    public void setContent(String content) {
        setAttribute(ATTR_NAME, content);
    }
}
