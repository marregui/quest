package io.crate.cli.gui.common;

import javax.swing.*;
import java.io.Serializable;
import java.util.*;


public class ObjectComboBoxModel<ItemType extends HasKey> extends AbstractListModel<ItemType> implements MutableComboBoxModel<ItemType>, Serializable {

    private final List<ItemType> elements;
    private ItemType selectedElement;


    public ObjectComboBoxModel() {
        elements = new ArrayList<>(10);
    }

    public void clear() {
        elements.clear();
        fireContentsChanged(this, -1, -1);
    }

    public int size() {
        return elements.size();
    }

    public List<ItemType> getElements() {
        return elements;
    }

    public void setElements(List<ItemType> newElements) {
        elements.clear();
        elements.addAll(newElements);
        fireIntervalAdded(this, 0, elements.size());
    }

    @Override
    public void addElement(ItemType item) {
        elements.add(item);
        fireIntervalAdded(this, elements.size() - 1, elements.size() - 1);
    }

    @Override
    public void removeElement(Object obj) {
        if (null != obj) {
            ItemType item = (ItemType) obj;
            int idx = elements.indexOf(item);
            if (-1 != idx) {
                elements.remove(idx);
                fireIntervalRemoved(this, idx, idx);
            }
        }
    }

    private static void checkBounds(String descriptor, int idx, int maxExcluded) {
        if (idx < 0 || idx >= maxExcluded) {
            throw new IndexOutOfBoundsException(String.format(
                    Locale.ENGLISH,
                    "idx %d not in [0..%d]: %s",
                    idx,
                    maxExcluded - 1,
                    descriptor));
        }
    }

    @Override
    public void insertElementAt(ItemType item, int index) {
        checkBounds("insertElementAt", index, elements.size());
        elements.add(index, item);
        fireIntervalAdded(this, index, index);
    }

    @Override
    public void removeElementAt(int index) {
        checkBounds("removeElementAt", index, elements.size());
        elements.remove(index);
        fireIntervalRemoved(this, index, index);
    }

    @Override
    public void setSelectedItem(Object obj) {
        if (null != obj) {
            selectedElement = (ItemType) obj;
            int idx = elements.indexOf(selectedElement);
            fireContentsChanged(this, idx, idx);
        }
    }

    @Override
    public Object getSelectedItem() {
        return selectedElement;
    }

    @Override
    public int getSize() {
        return elements.size();
    }

    @Override
    public ItemType getElementAt(int index) {
        checkBounds("getElementAt", index, elements.size());
        return elements.get(index);
    }
}
