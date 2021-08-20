/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.util.collection;

import pascal.taie.util.Hashes;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

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
        return Objects.equals(key, e.getKey()) &&
                Objects.equals(value, e.getValue());
    }

    @Override
    public int hashCode() {
        return Hashes.safeHash(key, value);
    }
}
