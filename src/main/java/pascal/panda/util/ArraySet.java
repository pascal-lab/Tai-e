/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.panda.util;

import javax.annotation.Nonnull;
import java.util.AbstractSet;
import java.util.Iterator;

/**
 * Set implementation based on ArrayMap. This class should only be
 * used for small set. Elements cannot be null.
 */
public class ArraySet<E> extends AbstractSet<E> {

    private static final int DEFAULT_CAPACITY = 8;
    // Dummy value to associate with an Object in the backing Map
    private static final Object PRESENT = new Object();

    private final ArrayMap<E, Object> map;

    public ArraySet() {
        this(DEFAULT_CAPACITY, true);
    }

    public ArraySet(int initialCapacity) {
        this(initialCapacity, true);
    }

    public ArraySet(int initialCapacity, boolean fixedCapacity) {
        map = new ArrayMap<>(initialCapacity, fixedCapacity);
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        //noinspection SuspiciousMethodCalls
        return map.containsKey(o);
    }

    @Override
    public boolean add(E e) {
        return map.put(e, PRESENT) == null;
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o) == PRESENT;
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Nonnull
    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public int size() {
        return map.size();
    }
}
