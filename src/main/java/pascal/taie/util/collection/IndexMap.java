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

import pascal.taie.util.Indexer;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Array-based implementation of the {@link Map} interface.
 * <p>
 * This implementation requires a {@link Indexer}, which provides
 * a unique index for each key object that could be put into this map.
 * Since the indexer maintains the mappings between objects and indexes,
 * this map does not need to store the keys, and it only keeps an array
 * ({@code values}) of the corresponding values.
 * A key object would be indexed to {@code index} by the indexer.
 * If {@code values[index]} is {@code null}, it means that the corresponding
 * key is absent in this map. Hence, it does not permit {@code null} values.
 * <p>
 * This implementation is designed for maps whose size is close to the universe.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @see Indexer
 * <p>
 * TODO: add mod count
 */
public class IndexMap<K, V> extends AbstractMap<K, V>
        implements Serializable {

    private static final String NULL_VALUE_MSG = "IndexMap does not permit null values";

    private final Indexer<K> indexer;

    private V[] values;

    private int size = 0;

    /**
     * The cache of {@link IndexMap#entrySet()}.
     */
    private transient Set<Entry<K, V>> entrySet;

    /**
     * The cache of {@link IndexMap#keySet()}.
     */
    private transient Set<K> keySet;

    public IndexMap(Indexer<K> indexer, int initialCapacity) {
        this.indexer = indexer;
        this.values = (V[]) new Object[initialCapacity];
    }

    public int capacity() {
        return values.length;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        return size == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        int index = indexer.getIndex((K) key);
        return isValidIndex(index) && values[index] != null;
    }

    private boolean isValidIndex(int index) {
        return 0 <= index && index < values.length;
    }

    @Override
    public V get(Object key) {
        int index = indexer.getIndex((K) key);
        return isValidIndex(index) ? values[index] : null;
    }

    @Override
    public V put(K key, V value) {
        Objects.requireNonNull(value, NULL_VALUE_MSG);
        int index = indexer.getIndex(key);
        if (index >= 0) {
            ensureCapacity(index);
            V oldV = values[index];
            values[index] = value;
            if (oldV == null) {
                ++size;
            }
            return oldV;
        } else {
            throw new IllegalArgumentException("index of " + key +
                    " is negative: " + index);
        }
    }

    private void ensureCapacity(int index) {
        int oldCapacity = values.length;
        if (index >= oldCapacity) {
            int newCapacity = Math.max(index + 1, (int) (oldCapacity * 1.5));
            values = Arrays.copyOf(values, newCapacity);
        }
    }

    @Override
    public V remove(Object key) {
        int index = indexer.getIndex((K) key);
        if (isValidIndex(index)) {
            V oldV = values[index];
            if (oldV != null) {
                values[index] = null;
                --size;
                return oldV;
            }
        }
        return null;
    }

    @Override
    public void clear() {
        if (size > 0) {
            size = 0;
            Arrays.fill(values, null);
        }
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> es;
        return (es = entrySet) == null ? (entrySet = new EntrySet()) : es;
    }

    private class EntrySet extends AbstractSet<Entry<K, V>> {

        @Override
        public boolean contains(Object o) {
            if (o instanceof Map.Entry<?, ?> e) {
                V value = get(e.getKey());
                return value != null && value.equals(e.getValue());
            }
            return false;
        }

        @Override
        public boolean remove(Object o) {
            if (o instanceof Map.Entry<?, ?> e) {
                Object key = e.getKey();
                int index = indexer.getIndex((K) key);
                if (isValidIndex(index)) {
                    V v = values[index];
                    if (v != null && v.equals(e.getValue())) {
                        values[index] = null;
                        --size;
                        return true;
                    }
                }
            }
            return false;
        }

        @Override
        public void clear() {
            IndexMap.this.clear();
        }

        @Override
        @Nonnull
        public Iterator<Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public int size() {
            return size;
        }
    }

    private class EntryIterator implements Iterator<Entry<K, V>> {

        /**
         * Current index.
         */
        private int index;

        /**
         * Index of last iterated index; -1 if no such.
         */
        private int lastRet;

        private EntryIterator() {
            index = nextNonNull(0);
            lastRet = -1;
        }

        /**
         * Returns the index of the element that is not {@code null} that
         * occurs on or after the specified starting index. If no such element
         * exists then {@code -1} is returned.
         */
        private int nextNonNull(int fromIndex) {
            if (fromIndex < 0) {
                throw new IndexOutOfBoundsException("fromIndex < 0: " + fromIndex);
            }
            while (true) {
                if (fromIndex == values.length) {
                    return -1;
                }
                if (values[fromIndex] != null) {
                    return fromIndex;
                }
                ++fromIndex;
            }
        }

        @Override
        public boolean hasNext() {
            return index != -1;
        }

        @Override
        public Entry<K, V> next() {
            int i = index;
            if (i == -1) {
                throw new NoSuchElementException();
            }
            index = nextNonNull(i + 1);
            lastRet = i;
            return new MapEntry(i);
        }

        @Override
        public void remove() {
            if (lastRet == -1) {
                throw new IllegalStateException();
            }
            values[lastRet] = null;
            --size;
            lastRet = -1;
        }
    }

    private class MapEntry implements Map.Entry<K, V> {

        private final int index;

        private final K key;

        private MapEntry(int index) {
            this.index = index;
            this.key = indexer.getObject(index);
        }

        @Override
        public K getKey() {
            return key;
        }

        @Override
        public V getValue() {
            return values[index];
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Set<K> keySet() {
        Set<K> ks = keySet;
        if (ks == null) {
            ks = new AbstractSet<>() {
                @Override
                public Iterator<K> iterator() {
                    return new Iterator<>() {

                        private final Iterator<Entry<K, V>> i = entrySet().iterator();

                        @Override
                        public boolean hasNext() {
                            return i.hasNext();
                        }

                        @Override
                        public K next() {
                            return i.next().getKey();
                        }

                        @Override
                        public void remove() {
                            i.remove();
                        }
                    };
                }

                @Override
                public int size() {
                    return IndexMap.this.size();
                }

                @Override
                public boolean isEmpty() {
                    return IndexMap.this.isEmpty();
                }

                @Override
                public void clear() {
                    IndexMap.this.clear();
                }

                @Override
                public boolean contains(Object k) {
                    return IndexMap.this.containsKey(k);
                }

                @Override
                public boolean remove(Object k) {
                    return IndexMap.this.remove(k) != null;
                }
            };
            keySet = ks;
        }
        return ks;
    }
}
