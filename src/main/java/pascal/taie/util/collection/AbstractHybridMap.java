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

import pascal.taie.util.Hashes;

import javax.annotation.Nonnull;
import java.io.Serializable;
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
 * <p>
 * By default, this set use {@link ArrayMap} for small map.
 *
 * @param <K> type of keys
 * @param <V> type of values
 */
public abstract class AbstractHybridMap<K, V> extends AbstractMap<K, V>
        implements Serializable {

    // invariant: at most one of singleton_key and map is non-null

    private static final String NULL_KEY = "HybridMap does not permit null keys";

    /**
     * Default size of small map, which acts as the threshold for
     * the number of items necessary for the small map to become a large map.
     */
    private static final int SMALL_SIZE = 8;

    /**
     * The key for singletons. Null if not singleton.
     */
    private K singletonKey;

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
    protected int getThreshold() {
        return SMALL_SIZE;
    }

    /**
     * Creates a small map.
     */
    protected Map<K, V> newSmallMap() {
        return new ArrayMap<>(getThreshold());
    }

    /**
     * Creates a large map.
     *
     * @param initialCapacity initial capacity of the resulting map.
     */
    protected abstract Map<K, V> newLargeMap(int initialCapacity);

    @Override
    public void clear() {
        if (singletonKey != null) {
            singletonKey = null;
            singleton_value = null;
        }
        if (map != null) {
            map.clear();
        }
    }

    @Override
    public boolean containsKey(Object key) {
        if (singletonKey != null) {
            return singletonKey.equals(key);
        }
        if (map != null) {
            return map.containsKey(key);
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        if (singletonKey != null) {
            return singleton_value.equals(value);
        }
        if (map != null) {
            return map.containsValue(value);
        }
        return false;
    }

    @Override
    public V get(Object key) {
        if (singletonKey != null) {
            if (singletonKey.equals(key)) {
                return singleton_value;
            }
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
        if (singletonKey != null) {
            if (singletonKey.equals(key)) {
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
        singletonKey = key;
        singleton_value = value;
        return null;
    }

    private void upgradeToSmallMap() {
        map = newSmallMap();
        if (singletonKey != null) {
            map.put(singletonKey, singleton_value);
            singletonKey = null;
            singleton_value = null;
        }
    }

    private void upgradeToLargeMap(int initialCapacity) {
        assert !isLargeMap;
        Map<K, V> origin = map;
        map = newLargeMap(initialCapacity);
        if (singletonKey != null) {
            map.put(singletonKey, singleton_value);
            singletonKey = null;
            singleton_value = null;
        }
        if (origin != null) {
            map.putAll(origin);
        }
        isLargeMap = true;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        int mSize = m.size();
        if (mSize == 0) {
            return;
        }
        int maxNewSize = mSize + size();
        int threshold = getThreshold();
        if (map == null) {
            if (maxNewSize == 1) {
                assert singletonKey == null;
                var entry = CollectionUtils.getOne(m.entrySet());
                singletonKey = Objects.requireNonNull(entry.getKey(), NULL_KEY);
                singleton_value = entry.getValue();
                return;
            } else if (maxNewSize <= threshold) {
                upgradeToSmallMap();
            } else {
                upgradeToLargeMap(maxNewSize + threshold);
            }
        } else if (!isLargeMap && maxNewSize > threshold) {
            upgradeToLargeMap(maxNewSize + threshold);
        }
        if (!(m instanceof AbstractHybridMap)) {
            m.keySet().forEach(k -> Objects.requireNonNull(k, NULL_KEY));
        }
        map.putAll(m);
    }

    @Override
    public V remove(Object key) {
        if (singletonKey != null) {
            if (singletonKey.equals(key)) {
                V oldValue = singleton_value;
                singletonKey = null;
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
        if (singletonKey != null) {
            return 1;
        }
        if (map != null) {
            return map.size();
        }
        return 0;
    }

    @Override
    public boolean isEmpty() {
        if (singletonKey != null) {
            return false;
        }
        if (map != null) {
            return map.isEmpty();
        }
        return true;
    }

    @Override
    @Nonnull
    public Collection<V> values() {
        return new Values();
    }

    @Override
    @Nonnull
    public Set<Entry<K, V>> entrySet() {
        return new EntrySet();
    }

    @Override
    @Nonnull
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
        int thisSize = size();
        if (thisSize != m.size()) {
            return false;
        }
        if (thisSize == 1 && obj instanceof AbstractHybridMap<?, ?> h) {
            if (singletonKey != null && h.singletonKey != null) {
                return singletonKey.equals(h.singletonKey)
                        && (Objects.equals(singleton_value, h.singleton_value));
            }
        }
        return entrySet().equals(m.entrySet());
    }

    @Override
    public int hashCode() { // see contract for Map.hashCode
        if (singletonKey != null) {
            return Hashes.safeHash(singletonKey, singleton_value);
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

        @Override
        @Nonnull
        public Iterator<K> iterator() {
            if (singletonKey != null) {
                return new SingletonIterator<K>() {
                    @Override
                    @Nonnull
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

        @Override
        @Nonnull
        public Iterator<V> iterator() {
            if (singletonKey != null) {
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
        @Override
        @Nonnull
        public Iterator<Entry<K, V>> iterator() {
            if (singletonKey != null) {
                return new SingletonIterator<Entry<K, V>>() {
                    @Override
                    @Nonnull
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
            if (singletonKey != null) {
                return singletonKey.equals(e.getKey())
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
            Entry<K, V> e = new MapEntry<>(singletonKey, singleton_value);
            done = true;
            return e;
        }

        public K nextKey() {
            if (done) {
                throw new NoSuchElementException();
            }
            done = true;
            return singletonKey;
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
            if (done && singletonKey != null) {
                singletonKey = null;
                singleton_value = null;
            } else {
                throw new IllegalStateException();
            }
        }
    }
}
