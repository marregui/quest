package io.quest.frontend.editor.meta;

import io.questdb.cairo.TableUtils;

public enum FileType {
    META(true),
    TXN(true),
    CV(true),
    SB(true),
    D(true),
    C,
    O,
    K,
    V,
    UNKNOWN;

    private final boolean defaultChecked;

    FileType() {
        this(false);
    }

    FileType(boolean checked) {
        defaultChecked = checked;
    }

    public boolean isDefaultChecked() {
        return defaultChecked;
    }


    public static FileType of(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return FileType.UNKNOWN;
        }

        // trim potential transaction version
        int p = fileName.length() - 1;
        while (p > 0) {
            char c = fileName.charAt(p);
            if (c >= '0' && c <= '9') {
                p--;
            } else {
                break;
            }
        }
        if (p <= 0) {
            return FileType.UNKNOWN;
        }
        if (fileName.charAt(p) == '.') {
            fileName = fileName.substring(0, p);
        }

        if (fileName.endsWith(TableUtils.META_FILE_NAME)) {
            return FileType.META;
        }

        if (fileName.endsWith(TableUtils.TXN_SCOREBOARD_FILE_NAME)) {
            return FileType.SB;
        }

        if (fileName.endsWith(TableUtils.TXN_FILE_NAME)) {
            return FileType.TXN;
        }

        if (fileName.endsWith(TableUtils.COLUMN_VERSION_FILE_NAME)) {
            return FileType.CV;
        }

        if (fileName.endsWith(".k")) {
            return FileType.K;
        }

        if (fileName.endsWith(".o")) {
            return FileType.O;
        }

        if (fileName.endsWith(".c")) {
            return FileType.C;
        }

        if (fileName.endsWith(".v")) {
            return FileType.V;
        }

        if (fileName.endsWith(".d")) {
            return FileType.D;
        }

        return FileType.UNKNOWN;
    }
}
