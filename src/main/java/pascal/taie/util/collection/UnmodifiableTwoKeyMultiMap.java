/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.util.collection;

import pascal.taie.util.TriConsumer;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

class UnmodifiableTwoKeyMultiMap<K1, K2, V> implements
        TwoKeyMultiMap<K1, K2, V>, Serializable {

    private final TwoKeyMultiMap<K1, K2, V> m;

    UnmodifiableTwoKeyMultiMap(@Nonnull TwoKeyMultiMap<K1, K2, V> m) {
        this.m = Objects.requireNonNull(m);
    }

    @Override
    public boolean contains(K1 key1, K2 key2, V value) {
        return m.contains(key1, key2, value);
    }

    @Override
    public boolean containsKey(K1 key1, K2 key2) {
        return m.containsKey(key1, key2);
    }

    @Override
    public boolean containsKey(K1 key1) {
        return m.containsKey(key1);
    }

    @Override
    public boolean containsValue(V value) {
        return m.containsValue(value);
    }

    @Override
    public Set<V> get(K1 key1, K2 key2) {
        return m.get(key1, key2);
    }

    @Override
    public MultiMap<K2, V> get(K1 key1) {
        return m.get(key1);
    }

    @Override
    public boolean put(@Nonnull K1 key1, @Nonnull K2 key2, @Nonnull V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(K1 key1, K2 key2, V value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(K1 key1, K2 key2) {
        throw new UnsupportedOperationException();
    }

    private transient Set<Pair<K1, K2>> twoKeySet;

    @Override
    public Set<Pair<K1, K2>> twoKeySet() {
        if (twoKeySet == null) {
            twoKeySet = Collections.unmodifiableSet(m.twoKeySet());
        }
        return twoKeySet;
    }

    private transient Set<TwoKeyMap.Entry<K1, K2, V>> entrySet;

    @Override
    public Set<TwoKeyMap.Entry<K1, K2, V>> entrySet() {
        if (entrySet == null) {
            entrySet = Collections.unmodifiableSet(m.entrySet());
        }
        return entrySet;
    }

    @Override
    public void forEach(@Nonnull TriConsumer<K1, K2, V> action) {
        m.forEach(action);
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isEmpty() {
        return m.isEmpty();
    }

    @Override
    public int size() {
        return m.size();
    }

    @Override
    public boolean equals(Object obj) {
        return m.equals(obj);
    }

    @Override
    public int hashCode() {
        return m.hashCode();
    }

    @Override
    public String toString() {
        return m.toString();
    }
}
