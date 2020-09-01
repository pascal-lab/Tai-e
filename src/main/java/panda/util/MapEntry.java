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

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Map entry.
 * Pair of a key and a value.
 */
class MapEntry<K, V> implements Entry<K, V>, Serializable {

    private final K key;

    private V value;

    /**
     * Constructs a new map entry.
     */
    public MapEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public V setValue(V value) {
        V old = this.value;
        this.value = value;
        return old;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Map.Entry<?, ?>))
            return false;
        Entry<?, ?> e = (Entry<?, ?>) obj;
        return (key == null ? e.getKey() == null : key.equals(e.getKey()))
                && (value == null ? e.getValue() == null : value.equals(e.getValue()));
    }

    @Override
    public int hashCode() {
        return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
    }
}
