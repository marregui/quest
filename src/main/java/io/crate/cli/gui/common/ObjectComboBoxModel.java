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
        synchronized (elements) {
            elements.clear();
        }
        fireContentsChanged(this, -1, -1);
    }

    public boolean contains(ItemType item) {
        boolean contains;
        synchronized (elements) {
            contains = elements.contains(item);
        }
        return contains;
    }

    public List<ItemType> getElements() {
        List<ItemType> copy;
        synchronized (elements) {
            copy = new ArrayList<>(elements);
        }
        return copy;
    }

    public void setElements(List<ItemType> newElements) {
        synchronized (elements) {
            elements.clear();
            elements.addAll(newElements);
        }
        fireIntervalAdded(this, 0, elements.size());
    }

    @Override
    public void addElement(ItemType item) {
        int idx;
        synchronized (elements) {
            elements.add(item);
            idx = elements.size() - 1;
        }
        fireIntervalAdded(this, idx, idx);
    }

    @Override
    public void insertElementAt(ItemType item, int idx) {
        synchronized (elements) {
            checkBounds("insertElementAt", idx, elements.size());
            elements.add(idx, item);
        }
        fireIntervalAdded(this, idx, idx);
    }

    @Override
    public void removeElement(Object obj) {
        if (null != obj) {
            ItemType item = (ItemType) obj;
            int idx;
            synchronized (elements) {
                idx = elements.indexOf(item);
                removeElementAt(idx);
            }
        }
    }

    @Override
    public void removeElementAt(int idx) {
        synchronized (elements) {
            checkBounds("removeElementAt", idx, elements.size());
            ItemType item = elements.remove(idx);
            if (null != selectedElement && selectedElement.equals(item)) {
                selectedElement = null;
            }
        }
        fireIntervalRemoved(this, idx, idx);
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
    public ItemType getElementAt(int index) {
        ItemType item;
        synchronized (elements) {
            checkBounds("getElementAt", index, elements.size());
            item = elements.get(index);
        }
        return item;
    }

    @Override
    public void setSelectedItem(Object obj) {
        if (null != obj) {
            int idx;
            synchronized (elements) {
                selectedElement = (ItemType) obj;
                idx = elements.indexOf(selectedElement);
            }
            fireContentsChanged(this, idx, idx);
        }
    }

    @Override
    public Object getSelectedItem() {
        Object selected;
        synchronized (elements) {
            selected = selectedElement;
        }
        return selected;
    }

    @Override
    public int getSize() {
        int size;
        synchronized (elements) {
            size = elements.size();
        }
        return size;
    }
}
