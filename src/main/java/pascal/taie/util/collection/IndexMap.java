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

import javax.annotation.Nonnull;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Array-based implementation of the {@code Map} interface.
 * This implementation requires a {@link Indexer}, which computes a unique
 * index for each key object that could be put into this map, and the entries
 * of the map are placed in an array according to the indexes.
 * The capacity of each map is fixed, and only the numbers in [0, capacity)
 * are considered as valid index for keys.
 * This map is designed for maps that close to the size of the universe.
 *
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 * @see Indexer
 */
public class IndexMap<K, V> extends AbstractMap<K, V> {

    private static final String NULL_KEY = "IndexMap does not permit null keys";

    private final Indexer indexer;

    private final Entry<K, V>[] entries;

    private int size = 0;

    private Set<Entry<K, V>> entrySet;

    @SuppressWarnings("unchecked")
    public IndexMap(Indexer indexer, int capacity) {
        this.indexer = indexer;
        this.entries = (Entry<K, V>[]) new Entry[capacity];
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
        int index = indexer.getIndex(key);
        return isValidIndex(index) && entries[index] != null;
    }

    @Override
    public V get(Object key) {
        int index = indexer.getIndex(key);
        if (isValidIndex(index)) {
            Entry<K, V> e = entries[index];
            return e == null ? null : e.getValue();
        }
        return null;
    }

    private boolean isValidIndex(int index) {
        return 0 <= index && index < entries.length;
    }

    /**
     * @throws IllegalIndexException if the index returned from the indexer is illegal.
     */
    @Override
    public V put(K key, V value) {
        Objects.requireNonNull(key, NULL_KEY);
        int index = indexer.getIndex(key);
        if (!isValidIndex(index)) {
            throw new IllegalIndexException(String.format(
                    "Index returned from indexer is illegal, expected: [0, %d), given: %d",
                    entries.length, index));
        }
        Entry<K, V> e = entries[index];
        if (e == null) {
            entries[index] = new MapEntry<>(key, value);
            ++size;
            return null;
        } else {
            return e.setValue(value);
        }
    }

    @Override
    public V remove(Object key) {
        int index = indexer.getIndex(key);
        if (isValidIndex(index)) {
            Entry<K, V> e = entries[index];
            if (e != null) {
                entries[index] = null;
                --size;
                return e.getValue();
            }
        }
        return null;
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        Set<Map.Entry<K, V>> es;
        return (es = entrySet) == null ? (entrySet = new EntrySet()) : es;
    }

    private class EntrySet extends AbstractSet<Entry<K, V>> {

        @Nonnull
        @Override
        public Iterator<Entry<K, V>> iterator() {
            return new EntryIterator();
        }

        @Override
        public int size() {
            return size;
        }

        @Override
        public void clear() {
            if (size > 0) {
                size = 0;
                Arrays.fill(entries, null);
            }
        }
    }

    private class EntryIterator implements Iterator<Entry<K, V>> {

        private final int BOUND = entries.length;

        private int current, next;

        private EntryIterator() {
            Entry<K, V>[] es = entries;
            current = BOUND;
            next = 0;
            if (size > 0) {
                // advance to first entry
                for (next = 0; next < BOUND && es[next] == null; ++next) {
                }
            }
        }

        @Override
        public boolean hasNext() {
            return next < BOUND;
        }

        @Override
        public Entry<K, V> next() {
            Entry<K, V>[] es = entries;
            if (next >= BOUND) {
                throw new NoSuchElementException();
            }
            current = next;
            if (++next < BOUND) {
                while (next < BOUND && es[next] == null) {
                    ++next;
                }
            }
            return es[current];
        }

        @Override
        public void remove() {
            int i = current;
            if (i == BOUND || entries[i] == null) {
                throw new IllegalStateException();
            }
            entries[i] = null;
            --size;
        }
    }
}
