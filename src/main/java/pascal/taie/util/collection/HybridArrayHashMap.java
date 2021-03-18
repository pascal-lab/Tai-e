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

import pascal.taie.util.HashUtils;

import javax.annotation.Nonnull;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Hybrid of array and hash map.
 * Small maps are represented as array map; above a certain threshold
 * a hash map is used instead. Moreover, empty maps and singleton maps
 * are represented with just a reference. Keys cannot be null.
 */
public final class HybridArrayHashMap<K, V> implements Map<K, V> {
    // invariant: at most one of singleton_key, array and hashmap is non-null

    private static final String NULL_KEY = "HybridArrayHashMap does not permit null keys";
    /**
     * Threshold for the number of items necessary for the array to become a hash map.
     */
    private static final int ARRAY_MAP_SIZE = 8;

    /**
     * The key for singletons. Null if not singleton.
     */
    private K singleton_key;

    /**
     * The value, for singletons. Null if not singleton.
     */
    private V singleton_value;

    /**
     * The array map with the items. Null if the hashmap is not used.
     */
    private ArrayMap<K, V> arrayMap;

    /**
     * The hash map with the items. Null if the hashmap is not used.
     */
    private HashMap<K, V> hashMap;

    /**
     * Constructs a new empty hybrid map.
     */
    public HybridArrayHashMap() {
        // do nothing
    }

    /**
     * Constructs a new hybrid map from the given map.
     */
    public HybridArrayHashMap(Map<K, V> m) {
        putAll(m);
    }

    @Override
    public void clear() {
        if (singleton_key != null) {
            singleton_key = null;
            singleton_value = null;
        }
        if (arrayMap != null) {
            arrayMap.clear();
        }
        if (hashMap != null) {
            hashMap.clear();
        }
    }

    @Override
    public boolean containsKey(Object key) {
        if (singleton_key != null) {
            return singleton_key.equals(key);
        }
        if (arrayMap != null) {
            return arrayMap.containsKey(key);
        }
        if (hashMap != null) {
            return hashMap.containsKey(key);
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        if (singleton_key != null) {
            return singleton_value.equals(value);
        }
        if (arrayMap != null) {
            return arrayMap.containsValue(value);
        }
        if (hashMap != null) {
            return hashMap.containsValue(value);
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
        if (arrayMap != null) {
            return arrayMap.get(key);
        }
        if (hashMap != null) {
            return hashMap.get(key);
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
            convertSingletonToArrayMap();
        }
        if (arrayMap != null) {
            if (arrayMap.size() + 1 <= ARRAY_MAP_SIZE) {
                return arrayMap.put(key, value);
            }
            convertArrayMapToHashMap();
        }
        if (hashMap != null) {
            return hashMap.put(key, value);
        }
        singleton_key = key;
        singleton_value = value;
        return null;
    }

    private void convertSingletonToArrayMap() {
        arrayMap = new ArrayMap<>(ARRAY_MAP_SIZE);
        arrayMap.put(singleton_key, singleton_value);
        singleton_key = null;
        singleton_value = null;
    }

    private void convertArrayMapToHashMap() {
        hashMap = new HashMap<>(ARRAY_MAP_SIZE * 2);
        hashMap.putAll(arrayMap);
        arrayMap = null;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        int m_size = m.size();
        if (m_size == 0) {
            return;
        }
        int max_new_size = m_size + size();
        if (arrayMap == null && hashMap == null && max_new_size == 1) {
            Entry<? extends K, ? extends V> e = CollectionUtils.getOne(m.entrySet());
            singleton_key = Objects.requireNonNull(e.getKey(), NULL_KEY);
            singleton_value = e.getValue();
            return;
        }
        if (!(m instanceof HybridArrayHashMap)) {
            m.keySet().forEach(k -> Objects.requireNonNull(k, NULL_KEY));
        }
        if (arrayMap == null && hashMap == null
                && max_new_size <= ARRAY_MAP_SIZE) {
            if (singleton_key != null)
                convertSingletonToArrayMap();
            else {
                arrayMap = new ArrayMap<>(ARRAY_MAP_SIZE);
            }
        }
        if (arrayMap != null) {
            if (max_new_size <= ARRAY_MAP_SIZE) {
                arrayMap.putAll(m);
                return;
            } else {
                convertArrayMapToHashMap();
            }
        }
        if (hashMap == null) {
            hashMap = new HashMap<>(ARRAY_MAP_SIZE + max_new_size);
        }
        if (singleton_key != null) {
            hashMap.put(singleton_key, singleton_value);
            singleton_key = null;
            singleton_value = null;
        }
        hashMap.putAll(m);
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
        if (arrayMap != null) {
            return arrayMap.remove(key);
        }
        if (hashMap != null) {
            return hashMap.remove(key);
        }
        return null;
    }

    @Override
    public int size() {
        if (singleton_key != null) {
            return 1;
        }
        if (arrayMap != null) {
            return arrayMap.size();
        }
        if (hashMap != null) {
            return hashMap.size();
        }
        return 0;
    }

    @Override
    public boolean isEmpty() {
        if (singleton_key != null) {
            return false;
        }
        if (arrayMap != null) {
            return arrayMap.isEmpty();
        }
        if (hashMap != null) {
            return hashMap.isEmpty();
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

    @Override
    public Set<K> keySet() {
        return new KeySet();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Map<?, ?>)) {
            return false;
        }
        Map<?, ?> m = (Map<?, ?>) obj;
        int this_size = size();
        if (this_size != m.size()) {
            return false;
        }
        if (this_size == 1 && obj instanceof HybridArrayHashMap<?, ?>) {
            HybridArrayHashMap<?, ?> h = (HybridArrayHashMap<?, ?>) obj;
            if (singleton_key != null && h.singleton_key != null)
                return singleton_key.equals(h.singleton_key)
                        && (Objects.equals(singleton_value, h.singleton_value));
        }
        return entrySet().equals(m.entrySet());
    }

    @Override
    public int hashCode() { // see contract for Map.hashCode
        if (singleton_key != null) {
            return HashUtils.safeHash(singleton_key, singleton_value);
        }
        if (arrayMap != null) {
            return arrayMap.hashCode();
        }
        if (hashMap != null) {
            return hashMap.hashCode();
        }
        return 0;
    }

    @Override
    public String toString() {
        if (singleton_key != null) {
            return "[" + singleton_key + '=' + singleton_value + ']';
        }
        if (arrayMap != null) {
            return arrayMap.toString();
        }
        if (hashMap != null) {
            return hashMap.toString();
        }
        return "[]";
    }

    private final class KeySet extends AbstractSet<K> {
        @Override
        public boolean contains(Object o) {
            return containsKey(o);
        }

        @Override
        public void clear() {
            HybridArrayHashMap.this.clear();
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
            if (arrayMap != null) {
                return arrayMap.keySet().iterator();
            }
            if (hashMap != null) {
                return hashMap.keySet().iterator();
            }
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return HybridArrayHashMap.this.size();
        }
    }

    private final class Values extends AbstractCollection<V> {
        @Override
        public boolean contains(Object o) {
            return containsValue(o);
        }

        @Override
        public void clear() {
            HybridArrayHashMap.this.clear();
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
            if (arrayMap != null) {
                return arrayMap.values().iterator();
            }
            if (hashMap != null) {
                return hashMap.values().iterator();
            }
            return Collections.emptyIterator();
        }

        @Override
        public int size() {
            return HybridArrayHashMap.this.size();
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
            if (arrayMap != null) {
                return arrayMap.entrySet().iterator();
            }
            if (hashMap != null) {
                return hashMap.entrySet().iterator();
            }
            return Collections.emptyIterator();
        }

        @Override
        public boolean contains(Object o) {
            if (!(o instanceof Entry<?, ?>)) {
                return false;
            }
            Entry<?, ?> e = (Entry<?, ?>) o;
            if (singleton_key != null) {
                return singleton_key.equals(e.getKey())
                        && Objects.equals(singleton_value, e.getValue());
            }
            if (arrayMap != null) {
                return arrayMap.entrySet().contains(o);
            }
            if (hashMap != null) {
                return hashMap.entrySet().contains(o);
            }
            return false;
        }

        @Override
        public void clear() {
            HybridArrayHashMap.this.clear();
        }

        @Override
        public int size() {
            return HybridArrayHashMap.this.size();
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
