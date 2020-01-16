package io.crate.cli.store;

import java.util.*;
import java.util.function.BiConsumer;


public class StoreItemDescriptor implements HasKey, Comparable<StoreItemDescriptor> {

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


    public StoreItemDescriptor(String name) {
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
        StoreItemDescriptor that = (StoreItemDescriptor) o;
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
    public int compareTo(StoreItemDescriptor that) {
        if (this == that) {
            return 0;
        }
        if (null == that) {
            return 1;
        }
        return getKey().compareTo(that.getKey());
    }
}
