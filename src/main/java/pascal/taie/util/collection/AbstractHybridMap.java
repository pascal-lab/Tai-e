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

import javax.annotation.Nonnull;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * This map supports hybrid of two map implementations, where one is
 * efficient for small map and another one is efficient for large map.
 * <p>
 * When the number of mappings contained in this map succeeds a threshold,
 * it will automatically upgrade the map implementation to the one that is
 * efficient for large map. Moreover, empty maps and singleton maps are
 * represented with just a reference.
 * <p>
 * Keys added to this map cannot be null.
 *
 * @param <K> type of keys
 * @param <V> type of values
 */
public abstract class AbstractHybridMap<K, V> extends AbstractMap<K, V> {

    // invariant: at most one of singleton_key and map is non-null

    private static final String NULL_KEY = "HybridMap does not permit null keys";

    /**
     * The key for singletons. Null if not singleton.
     */
    private K singleton_key;

    /**
     * The value, for singletons. Null if not singleton.
     */
    private V singleton_value;

    /**
     * The map containing the key-value mappings. Null if the map is not used.
     */
    private Map<K, V> map;

    /**
     * Whether the map implementation is the one for large map.
     */
    private boolean isLargeMap = false;

    /**
     * Constructs a new empty hybrid map.
     */
    protected AbstractHybridMap() {
        // do nothing
    }

    /**
     * Constructs a new hybrid map from the given map.
     */
    public AbstractHybridMap(Map<K, V> m) {
        putAll(m);
    }

    /**
     * @return the threshold between sizes of small map and large map.
     * When number of mappings exceeds the threshold, map should be upgraded
     * to large map.
     */
    protected abstract int getThreshold();

    /**
     * Creates a small map.
     *
     * @param initialCapacity initial capacity of the resulting map.
     *                        Usually this is the same as the threshold.
     */
    protected abstract Map<K, V> newSmallMap(int initialCapacity);

    /**
     * Creates a large map.
     *
     * @param initialCapacity initial capacity of the resulting map.
     */
    protected abstract Map<K, V> newLargeMap(int initialCapacity);

    @Override
    public void clear() {
        if (singleton_key != null) {
            singleton_key = null;
            singleton_value = null;
        }
        if (map != null) {
            map.clear();
        }
    }

    @Override
    public boolean containsKey(Object key) {
        if (singleton_key != null) {
            return singleton_key.equals(key);
        }
        if (map != null) {
            return map.containsKey(key);
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        if (singleton_key != null) {
            return singleton_value.equals(value);
        }
        if (map != null) {
            return map.containsValue(value);
        }
        return false;
    }

    @Override
    public V get(Object key) {
        if (singleton_key != null) {
            if (singleton_key.equals(key))
                return singleton_value;
            return null;
        }
        if (map != null) {
            return map.get(key);
        }
        return null;
    }

    @Override
    public V put(K key, V value) {
        Objects.requireNonNull(key, NULL_KEY);
        if (singleton_key != null) {
            if (singleton_key.equals(key)) {
                V old = singleton_value;
                singleton_value = value;
                return old;
            }
            upgradeToSmallMap();
        }
        if (map != null) {
            if (!isLargeMap && map.size() + 1 > getThreshold()) {
                upgradeToLargeMap(getThreshold() * 2);
            }
            return map.put(key, value);
        }
        singleton_key = key;
        singleton_value = value;
        return null;
    }

    private void upgradeToSmallMap() {
        map = newSmallMap(getThreshold());
        if (singleton_key != null) {
            map.put(singleton_key, singleton_value);
            singleton_key = null;
            singleton_value = null;
        }
    }

    private void upgradeToLargeMap(int initialCapacity) {
        assert !isLargeMap;
        Map<K, V> origin = map;
        map = newLargeMap(initialCapacity);
        if (singleton_key != null) {
            map.put(singleton_key, singleton_value);
            singleton_key = null;
            singleton_value = null;
        }
        if (origin != null) {
            map.putAll(origin);
        }
        isLargeMap = true;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        int m_size = m.size();
        if (m_size == 0) {
            return;
        }
        int max_new_size = m_size + size();
        int threshold = getThreshold();
        if (map == null) {
            if (max_new_size == 1) {
                assert singleton_key == null;
                var entry = CollectionUtils.getOne(m.entrySet());
                singleton_key = Objects.requireNonNull(entry.getKey(), NULL_KEY);
                singleton_value = entry.getValue();
                return;
            } else if (max_new_size <= threshold) {
                upgradeToSmallMap();
            } else {
                upgradeToLargeMap(max_new_size + threshold);
            }
        } else if (!isLargeMap && max_new_size > threshold) {
            upgradeToLargeMap(max_new_size + threshold);
        }
        if (!(m instanceof AbstractHybridMap)) {
            m.keySet().forEach(k -> Objects.requireNonNull(k, NULL_KEY));
        }
        map.putAll(m);
    }

    @Override
    public V remove(Object key) {
        if (singleton_key != null) {
            if (singleton_key.equals(key)) {
                V oldValue = singleton_value;
                singleton_key = null;
                singleton_value = null;
                return oldValue;
            }
            return null;
        }
        if (map != null) {
            return map.remove(key);
        }
        return null;
    }

    @Override
    public int size() {
        if (singleton_key != null) {
            return 1;
        }
        if (map != null) {
            return map.size();
        }
        return 0;
    }

    @Override
    public boolean isEmpty() {
        if (singleton_key != null) {
            return false;
        }
        if (map != null) {
            return map.isEmpty();
        }
        return true;
    }

    @Nonnull
    @Override
    public Collection<V> values() {
        return new Values();
    }

    @Nonnull
    @Override
    public Set<Entry<K, V>> entrySet() {
        return new EntrySet();
    }

    @Nonnull
    @Override
    public Set<K> keySet() {
        return new KeySet();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Map<?, ?> m)) {
            return false;
        }
        int this_size = size();
        if (this_size != m.size()) {
            return false;
        }
        if (this_size == 1 && obj instanceof AbstractHybridMap<?, ?> h) {
            if (singleton_key != null && h.singleton_key != null)
                return singleton_key.equals(h.singleton_key)
                        && (Objects.equals(singleton_value, h.singleton_value));
        }
        return entrySet().equals(m.entrySet());
    }

    @Override
    public int hashCode() { // see contract for Map.hashCode
        if (singleton_key != null) {
            return Hashes.safeHash(singleton_key, singleton_value);
        }
        if (map != null) {
            return map.hashCode();
        }
        return 0;
    }

    private final class KeySet extends AbstractSet<K> {
        @Override
        public boolean contains(Object o) {
            return containsKey(o);
        }

        @Override
        public void clear() {
            AbstractHybridMap.this.clear();
        }

        @Nonnull
        @Override
        public Iterator<K> iterator() {
            if (singleton_key != null) {
                return new SingletonIterator<K>() {
                    @Nonnull
                    @Override
                    public K next() {
                        return nextKey();
                    }
                };
            }
            if (map != null) {
                return map.keySet().iterator();
            }
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return AbstractHybridMap.this.size();
        }
    }

    private final class Values extends AbstractCollection<V> {
        @Override
        public boolean contains(Object o) {
            return containsValue(o);
        }

        @Override
        public void clear() {
            AbstractHybridMap.this.clear();
        }

        @Nonnull
        @Override
        public Iterator<V> iterator() {
            if (singleton_key != null) {
                return new SingletonIterator<V>() {
                    @Override
                    public V next() {
                        return nextValue();
                    }
                };
            }
            if (map != null) {
                return map.values().iterator();
            }
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return AbstractHybridMap.this.size();
        }
    }

    private final class EntrySet extends AbstractSet<Entry<K, V>> {
        @Nonnull
        @Override
        public Iterator<Entry<K, V>> iterator() {
            if (singleton_key != null) {
                return new SingletonIterator<Entry<K, V>>() {
                    @Nonnull
                    @Override
                    public Entry<K, V> next() {
                        return nextEntry();
                    }
                };
            }
            if (map != null) {
                return map.entrySet().iterator();
            }
            return Collections.emptyIterator();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Entry<?, ?> e)) {
                return false;
            }
            if (singleton_key != null) {
                return singleton_key.equals(e.getKey())
                        && Objects.equals(singleton_value, e.getValue());
            }
            if (map != null) {
                return map.entrySet().contains(o);
            }
            return false;
        }

        @Override
        public void clear() {
            AbstractHybridMap.this.clear();
        }

        @Override
        public int size() {
            return AbstractHybridMap.this.size();
        }
    }

    private abstract class SingletonIterator<E> implements Iterator<E> {

        boolean done;

        @Override
        public boolean hasNext() {
            return !done;
        }

        public Entry<K, V> nextEntry() {
            if (done) {
                throw new NoSuchElementException();
            }
            Entry<K, V> e = new MapEntry<>(singleton_key, singleton_value);
            done = true;
            return e;
        }

        public K nextKey() {
            if (done) {
                throw new NoSuchElementException();
            }
            done = true;
            return singleton_key;
        }

        public V nextValue() {
            if (done) {
                throw new NoSuchElementException();
            }
            done = true;
            return singleton_value;
        }

        @Override
        public void remove() {
            if (done && singleton_key != null) {
                singleton_key = null;
                singleton_value = null;
            } else {
                throw new IllegalStateException();
            }
        }
    }
}
