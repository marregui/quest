package io.quest.common;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;

public class StringTransferable implements Transferable {

    private static final DataFlavor[] supported = {DataFlavor.stringFlavor};

    private String str;

    public StringTransferable(String str) {
        this.str = str;
    }

    public DataFlavor[] getTransferDataFlavors() {
        return supported;
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return DataFlavor.stringFlavor.equals(flavor);
    }

    public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
        if (!isDataFlavorSupported(flavor)) {
            throw new UnsupportedFlavorException(flavor);
        }
        return str;
    }

}