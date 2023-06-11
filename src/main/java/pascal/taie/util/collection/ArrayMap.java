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

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Map implementation based on ArrayList. This class should only be
 * used for small set. Keys cannot be null.
 * Note that remove(Object) will shift the elements after the removed one.
 * TODO: if necessary, optimize remove(Object) and let add(Object) add
 *  element to empty hole of the array.
 */
public class ArrayMap<K, V> extends AbstractMap<K, V>
        implements Serializable {

    public static final int DEFAULT_CAPACITY = 8;

    private static final String NULL_KEY = "ArrayMap does not permit null keys";
    private static final String EXCEED_CAPACITY = "Capacity of this ArrayMap is fixed";

    private final ArrayList<Entry<K, V>> entries;
    private final int initialCapacity;
    private final boolean fixedCapacity;

    /**
     * The cache of {@link ArrayMap#entrySet()}.
     */
    private transient Set<Entry<K, V>> entrySet;

    public ArrayMap() {
        this(DEFAULT_CAPACITY);
    }

    public ArrayMap(int initialCapacity) {
        this(initialCapacity, true);
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
        Objects.requireNonNull(key, NULL_KEY);
        Entry<K, V> e = getEntry(key);
        if (e == null) {
            ensureCapacity(size() + 1);
            entries.add(new MapEntry<>(key, value));
            return null;
        } else {
            return e.setValue(value);
        }
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
    @Nonnull
    public Set<Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> es;
        return (es = entrySet) == null ? (entrySet = new EntrySet()) : es;
    }

    private Entry<K, V> getEntry(Object key) {
        for (Entry<K, V> e : entries) {
            if (e.getKey().equals(key)) {
                return e;
            }
        }
        return null;
    }

    private void ensureCapacity(int minCapacity) {
        if (fixedCapacity && minCapacity > initialCapacity) {
            throw new TooManyElementsException(EXCEED_CAPACITY);
        }
    }

    private class EntrySet extends AbstractSet<Entry<K, V>> {

        @Override
        @Nonnull
        public Iterator<Entry<K, V>> iterator() {
            return entries.iterator();
        }

        @Override
        public int size() {
            return entries.size();
        }

        @Override
        public void clear() {
            entries.clear();
        }
    }
}
