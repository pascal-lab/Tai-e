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

package panda.util;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * Map implementation based on two ArrayLists (one for keys and one for
 * values). This class should only be used for small set.
 * Elements cannot be null.
 * Note that remove(Object) will shift the rest elements to the end.
 * TODO: if necessary, optimize remove(Object) and let add(Object) add
 *  element to empty hole of the array.
 */
public class ArrayMap<K, V> extends AbstractMap<K, V> {

    private static final String NULL_MESSAGE = "ArraySet does not permit null element";
    private static final int DEFAULT_CAPACITY = 8;

    private final ArrayList<Entry<K, V>> entries;
    private final int initialCapacity;
    private final boolean fixedCapacity;

    public ArrayMap() {
        this(DEFAULT_CAPACITY, true);
    }

    public ArrayMap(int initialCapacity, boolean fixedCapacity) {
        this.initialCapacity = initialCapacity;
        this.fixedCapacity = fixedCapacity;
        entries = new ArrayList<>(initialCapacity);
    }

    @Override
    public int size() {
        return entries.size();
    }

    @Override
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    @Override
    public boolean containsValue(Object value) {
        for (Entry<K, V> entry : entries) {
            if (entry.getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean containsKey(Object key) {
        return getEntry(key) != null;
    }

    @Override
    public V get(Object key) {
        Entry<K, V> e = getEntry(key);
        return e == null ? null : e.getValue();
    }

    @Override
    public V put(K key, V value) {
        return super.put(key, value);
    }

    @Override
    public V remove(Object key) {
        for (int i = 0; i < entries.size(); ++i) {
            Entry<K, V> e = entries.get(i);
            if (e.getKey().equals(key)) {
                return entries.remove(i).getValue();
            }
        }
        return null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        super.putAll(m);
    }

    @Override
    public void clear() {
        entries.clear();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return null;
    }

    private Entry<K, V> getEntry(Object key) {
        for (Entry<K, V> e : entries) {
            if (e.getKey().equals(key)) {
                return e;
            }
        }
        return null;
    }
}
