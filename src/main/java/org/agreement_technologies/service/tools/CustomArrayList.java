package org.agreement_technologies.service.tools;

import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;

/**
 * @author Alejandro Torreño
 */
public class CustomArrayList<E> extends AbstractList<E>
        implements List<E>, RandomAccess, Cloneable, java.io.Serializable {
    private int size;
    private transient E v[];

    @SuppressWarnings("unchecked")
    public CustomArrayList(int capacity) {
        size = 0;
        v = (E[]) new Object[capacity];
    }

    public CustomArrayList() {
        this(10);
    }

    public void trimToSize(int s) {
        this.size = s;
    }

    public void addNotRepeated(E value) {
        for (int i = 0; i < this.size; i++)
            if (value.equals(v[i]))
                return;
        this.add(value);
    }

    public void clear() {
        size = 0;
    }

    public void insert(E x) {
        if (size == v.length)
            v = java.util.Arrays.copyOf(v, v.length + (v.length >> 1));
        v[size++] = x;
    }

    public boolean add(E x) {
        if (size == v.length)
            v = java.util.Arrays.copyOf(v, v.length + (v.length >> 1));
        v[size++] = x;
        return true;
    }

    public void removePosition(int x) {
        v[x] = v[--size];
    }

    public E get(int index) {
        return v[index];
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return this.size == 0;
    }

    //Gets and erases an element of the array
    public E retrieve() {
        this.size--;
        return this.get(this.size);
    }

    //Appends an array to the arraylist
    public void append(CustomArrayList<E> array) {
        for (E v : array)
            this.add(v);
    }

    //Checks if the array includes a certain element
    public boolean includes(E elem) {
        for (E val : this.v)
            if (val == elem) return true;
        return false;
    }
}
