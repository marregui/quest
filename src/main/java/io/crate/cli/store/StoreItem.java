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

package io.crate.cli.store;

import io.crate.cli.common.HasKey;

import java.util.*;
import java.util.function.BiConsumer;


public class StoreItem implements HasKey, Comparable<StoreItem> {

    protected static final Class<?>[] CONSTRUCTOR_SIGNATURE = {StoreItem.class};

    public static final Comparator<String> KEY_COMPARATOR = (k1, k2) -> {
        String [] k1Parts = k1.split("\\.");
        String [] k2Parts = k2.split("\\.");
        if (k1Parts.length != k2Parts.length) {
            return Integer.compare(
                    k1Parts.length,
                    k2Parts.length);
        } else if (2 == k1Parts.length) {
            if (k1Parts[0].equals(k2Parts[0])) {
                return k1Parts[1].compareTo(k2Parts[1]);
            }
        }
        return k1.compareTo(k2);
    };

    protected String name;
    protected Map<String, String> attributes;


    protected StoreItem(StoreItem other) {
        this.name = other.name;
        attributes = other.attributes;
    }

    public StoreItem(String name) {
        if (null == name || name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }
        this.name = name;
        attributes = new TreeMap<>();
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public String getName() {
        return name;
    }

    public String setName(String newName) {
        if (null == newName || newName.isEmpty()) {
            throw new IllegalArgumentException("newName cannot be null or empty");
        }
        if (false == name.equals(newName)) {
            attributes = getRenamedAttributes(attributes, newName);
            name = newName;
        }
        return name;
    }

    @Override
    public String getKey() {
        return String.format(Locale.ENGLISH,"%s.%s", name, attributes);
    }

    public String getStoreItemAttributeKey(String attrName) {
        return getStoreItemAttributeKey(name, attrName);
    }

    public String getStoreItemAttributeKey(HasKey attr) {
        return getStoreItemAttributeKey(name, attr.getKey());
    }

    public static String getStoreItemAttributeKey(String name, String attributeName) {
        return null == name ?
                attributeName
                :
                String.format(Locale.ENGLISH, "%s.%s", name, attributeName);
    }

    public static void splitStoreItemAttributeKey(String key, BiConsumer<String, String>  consumer) {
        if (null == key || key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or empty");
        }
        String [] keyParts = key.split("\\.");
        if (2 == keyParts.length) {
            consumer.accept(keyParts[0], keyParts[1]);
        }
    }

    private Map<String, String> getRenamedAttributes(Map<String, String> attributes, String newName) {
        Map<String, String> renamed = new HashMap<>();
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            splitStoreItemAttributeKey(entry.getKey(), (uniqueName, attributeName) -> {
                if (uniqueName.equals(name)) {
                    String newKey = name.equals(newName) ?
                            entry.getKey()
                            :
                            getStoreItemAttributeKey(newName, attributeName);
                    renamed.put(newKey, entry.getValue());
                }
            });

        }
        return renamed;
    }

    public String getAttribute(String attrName) {
        return attributes.get(getStoreItemAttributeKey(attrName));
    }

    public String getAttribute(HasKey attr) {
        return attributes.get(getStoreItemAttributeKey(attr));
    }

    public String setAttribute(HasKey attr, String value) {
        return setAttribute(attr, value, "");
    }

    public String setAttribute(HasKey attr, String value, String defaultValue) {
        return attributes.put(
                getStoreItemAttributeKey(attr),
                null == value || value.isEmpty() ? defaultValue : value);
    }

    public String setAttribute(String attrName, String value, String defaultValue) {
        return attributes.put(
                getStoreItemAttributeKey(attrName),
                null == value || value.isEmpty() ? defaultValue : value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StoreItem that = (StoreItem) o;
        return name.equals(that.name) && attributes.equals(that.attributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, attributes);
    }

    @Override
    public String toString() {
        return getKey();
    }

    @Override
    public int compareTo(StoreItem that) {
        if (this == that) {
            return 0;
        }
        if (null == that) {
            return 1;
        }
        return KEY_COMPARATOR.compare(getKey(), that.getKey());
    }
}
