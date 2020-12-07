/*
 * Tai'e - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai'e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.util;

import javax.annotation.Nonnull;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Objects;

/**
 * Set implementation based on ArrayList. This class should only be
 * used for small set. Elements cannot be null.
 * Note that remove(Object) will shift the rest elements to the end.
 * TODO: if necessary, optimize remove(Object) and let add(Object) add
 *  element to empty hole of the array.
 */
public class ArraySet<E> extends AbstractSet<E> {

    private static final String NULL_MESSAGE = "ArraySet does not permit null element";
    private static final int DEFAULT_CAPACITY = 8;

    private final ArrayList<E> elements;
    private final int initialCapacity;
    private final boolean fixedCapacity;

    public ArraySet() {
        this(DEFAULT_CAPACITY, true);
    }

    public ArraySet(int initialCapacity) {
        this(initialCapacity, true);
    }

    public ArraySet(int initialCapacity, boolean fixedCapacity) {
        this.initialCapacity = initialCapacity;
        this.fixedCapacity = fixedCapacity;
        elements = new ArrayList<>(initialCapacity);
    }

    @Override
    public boolean isEmpty() {
        return elements.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return elements.contains(o);
    }

    @Nonnull
    @Override
    public Object[] toArray() {
        return elements.toArray();
    }

    @Nonnull
    @Override
    public <T> T[] toArray(@Nonnull T[] a) {
        //noinspection SuspiciousToArrayCall
        return elements.toArray(a);
    }

    @Override
    public boolean add(E e) {
        Objects.requireNonNull(e, NULL_MESSAGE);
        ensureCapacity(size() + 1);
        return !elements.contains(e) && elements.add(e);
    }

    @Override
    public boolean remove(Object o) {
        return o != null && elements.remove(o);
    }

    @Override
    public boolean containsAll(@Nonnull Collection<?> c) {
        return elements.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean changed = false;
        for (E e : c) {
            changed |= add(e);
        }
        return changed;
    }

    @Override
    public boolean retainAll(@Nonnull Collection<?> c) {
        return elements.retainAll(c);
    }

    @Override
    public void clear() {
        elements.clear();
    }

    @Nonnull
    @Override
    public Iterator<E> iterator() {
        return elements.iterator();
    }

    @Override
    public int size() {
        return elements.size();
    }

    private void ensureCapacity(int minCapacity) {
        if (fixedCapacity && minCapacity > initialCapacity) {
            throw new TooManyElementsException("Capacity of this ArraySet is fixed");
        }
    }
}
