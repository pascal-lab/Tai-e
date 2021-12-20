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

class ImmutableMapEntry<K, V> extends MapEntry<K, V> {

    ImmutableMapEntry(K key, V value) {
        super(key, value);
    }

    @Override
    public V setValue(V value) {
        throw new UnsupportedOperationException();
    }
}
