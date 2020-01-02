package io.crate.cli.connections;

import java.util.Comparator;
import java.util.Locale;
import java.util.function.BiConsumer;


public enum AttributeName {

    host("localhost"),
    port("5432"),
    username("crate"),
    password("");


    private final String defaultValue;

    AttributeName(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    String getDefaultValue() {
        return defaultValue;
    }

    public static String key(String uniqueName, AttributeName attributeName) {
        return null == uniqueName ?
                attributeName.name()
                :
                String.format(Locale.ENGLISH, "%s.%s", uniqueName, attributeName.name());
    }

    public static String [] splitKey(String key, BiConsumer<String, AttributeName> operation) {
        if (null == key || key.isEmpty()) {
            throw new IllegalArgumentException("key cannot be null or empty");
        }
        String [] keyParts = key.split("\\.");
        if (2 == keyParts.length) {
            operation.accept(keyParts[0], valueOf(keyParts[1]));
        }
        return keyParts;
    }

    public static final Comparator<String> KEY_COMPARATOR = (k1, k2) -> {
        String [] k1Parts = k1.split("\\.");
        String [] k2Parts = k2.split("\\.");
        if (k1Parts.length != k2Parts.length) {
            return Integer.compare(
                    k1Parts.length,
                    k2Parts.length);
        } else if (2 == k1Parts.length) {
            if (k1Parts[0].equals(k2Parts[0])) {
                return Integer.compare(
                        valueOf(k1Parts[1]).ordinal(),
                        valueOf(k2Parts[1]).ordinal());
            }
        }
        return k1.compareTo(k2);
    };
}
