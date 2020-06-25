/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C)  2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C)  2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.util;

import java.io.Serializable;
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Hybrid of array and hash map.
 * Small maps are represented as arrays; above a certain threshold a hash map is used instead.
 * Moreover, empty maps and singleton maps are represented with just a reference.
 * Keys cannot be null.
 */
public final class HybridArrayHashMap<K, V> implements Map<K, V>, Serializable {
    // invariant: at most one of singleton_key, array and hashmap is non-null

    private static final String NULL_KEY = "HybridArrayHashMap does not permit null keys";

    /**
     * Threshold for the number of items necessary for the array to become a hash map.
     */
    private static final int ARRAY_SIZE = 8;

    /**
     * The key for singletons. Null if not singleton.
     */
    private K singleton_key;

    /**
     * The value, for singletons. Null if not singleton.
     */
    private V singleton_value;

    /**
     * The array with the item keys. Null if the array is not used.
     */
    private Object[] array_keys;

    /**
     * The array with the item values. Null if the array is not used.
     */
    private Object[] array_values;

    /**
     * Counter for the number of items in the container.
     */
    private int number_of_used_array_entries; // = number of non-null entries in array_keys. if non-null

    /**
     * The hash map with the items. Null if the hashmap is not used.
     */
    private HashMap<K, V> hashmap;

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
        int m_size = m.size();
        if (m_size == 0)
            return;
        if (m_size == 1) {
            Entry<K, V> e = getFirstEntry(m);
            singleton_key = e.getKey();
            singleton_value = e.getValue();
        } else if (m_size <= ARRAY_SIZE) {
            array_keys = new Object[ARRAY_SIZE];
            array_values = new Object[ARRAY_SIZE];
            number_of_used_array_entries = 0;
            for (Entry<K, V> e : m.entrySet()) {
                K key = e.getKey();
                if (key == null)
                    throw new NullPointerException(NULL_KEY);
                V value = e.getValue();
                array_keys[number_of_used_array_entries] = key;
                array_values[number_of_used_array_entries++] = value;
            }
        } else {
            for (K k : m.keySet())
                if (k == null)
                    throw new NullPointerException(NULL_KEY);
            hashmap = new HashMap<>(m_size + ARRAY_SIZE);
            hashmap.putAll(m);
        }
    }

    @SuppressWarnings("unchecked")
    private static <K, V> Entry<K, V> getFirstEntry(Map<K, V> m) {
        if (!(m instanceof HybridArrayHashMap<?, ?>))
            return m.entrySet().iterator().next();
        HybridArrayHashMap<?, ?> map = (HybridArrayHashMap<?, ?>) m;
        if (map.singleton_key != null)
            return new MapEntry<>((K) map.singleton_key, (V) map.singleton_value);
        if (map.array_keys != null) {
            for (int i = 0; i < ARRAY_SIZE; i++)
                if (map.array_keys[i] != null)
                    return new MapEntry<>((K) map.array_keys[i], (V) map.array_values[i]);
            return null;
        }
        if (map.hashmap != null)
            return (Entry<K, V>) map.hashmap.entrySet().iterator().next();
        return null;
    }

    @Override
    public void clear() {
        if (singleton_key != null) {
            singleton_key = null;
            singleton_value = null;
        } else if (array_keys != null) {
            Arrays.fill(array_keys, null);
            Arrays.fill(array_values, null);
            number_of_used_array_entries = 0;
        } else if (hashmap != null)
            hashmap.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        if (singleton_key != null)
            return singleton_key.equals(key);
        if (array_keys != null) {
            for (int i = 0; i < ARRAY_SIZE; i++)
                if (array_keys[i] != null && array_keys[i].equals(key))
                    return true;
            return false;
        }
        if (hashmap != null)
            return hashmap.containsKey(key);
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        if (singleton_key != null)
            return singleton_value.equals(value);
        if (array_keys != null) {
            for (int i = 0; i < ARRAY_SIZE; i++)
                if (array_keys[i] != null && array_values[i].equals(value))
                    return true;
            return false;
        }
        if (hashmap != null)
            return hashmap.containsValue(value);
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public V get(Object key) {
        if (singleton_key != null) {
            if (singleton_key.equals(key))
                return singleton_value;
            return null;
        }
        if (array_keys != null) {
            for (int i = 0; i < ARRAY_SIZE; i++)
                if (array_keys[i] != null && array_keys[i].equals(key))
                    return (V) array_values[i];
            return null;
        }
        if (hashmap != null)
            return hashmap.get(key);
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public V put(K key, V value) {
        if (key == null)
            throw new NullPointerException(NULL_KEY);
        if (singleton_key != null) {
            if (singleton_key.equals(key)) {
                V old = singleton_value;
                singleton_value = value;
                return old;
            }
            convertSingletonToArray();
        }
        if (array_keys != null) {
            for (int i = 0; i < ARRAY_SIZE; i++)
                if (array_keys[i] != null && array_keys[i].equals(key)) {
                    V old = (V) array_values[i];
                    array_values[i] = value;
                    return old;
                }
            for (int i = 0; i < ARRAY_SIZE; i++) {
                if (array_keys[i] == null) {
                    array_keys[i] = key;
                    array_values[i] = value;
                    number_of_used_array_entries++;
                    return null;
                }
            }
            convertArrayToHashMap();
        }
        if (hashmap != null)
            return hashmap.put(key, value);
        singleton_key = key;
        singleton_value = value;
        return null;
    }

    private void convertSingletonToArray() {
        array_keys = new Object[ARRAY_SIZE];
        array_values = new Object[ARRAY_SIZE];
        array_keys[0] = singleton_key;
        array_values[0] = singleton_value;
        number_of_used_array_entries = 1;
        singleton_key = null;
        singleton_value = null;
    }

    @SuppressWarnings("unchecked")
    private void convertArrayToHashMap() {
        hashmap = new HashMap<>(ARRAY_SIZE * 2);
        for (int i = 0; i < ARRAY_SIZE; i++) {
            K key = (K) array_keys[i];
            if (key != null) {
                V value = (V) array_values[i];
                hashmap.put(key, value);
            }
        }
        array_keys = null;
        array_values = null;
        number_of_used_array_entries = 0;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        int m_size = m.size();
        if (m_size == 0)
            return;
        int max_new_size = m_size + size();
        if (array_keys == null && hashmap == null && max_new_size == 1) {
            Entry<? extends K, ? extends V> e = getFirstEntry(m);
            K key = e.getKey();
            if (key == null)
                throw new NullPointerException(NULL_KEY);
            singleton_key = key;
            singleton_value = e.getValue();
            return;
        }
        if (array_keys == null && hashmap == null && max_new_size <= ARRAY_SIZE) {
            if (singleton_key != null)
                convertSingletonToArray();
            else {
                array_keys = new Object[ARRAY_SIZE];
                array_values = new Object[ARRAY_SIZE];
                number_of_used_array_entries = 0;
            }
        }
        if (array_keys != null && max_new_size <= ARRAY_SIZE) {
            int next = 0;
            loop:
            for (Entry<? extends K, ? extends V> f : m.entrySet()) {
                K key = f.getKey();
                if (key == null)
                    throw new NullPointerException(NULL_KEY);
                V value = f.getValue();
                for (int i = 0; i < ARRAY_SIZE; i++)
                    if (array_keys[i] != null && array_keys[i].equals(key)) {
                        array_values[i] = value;
                        continue loop;
                    }
                while (array_keys[next] != null)
                    next++;
                array_keys[next] = key;
                array_values[next++] = value;
                number_of_used_array_entries++;
            }
            return;
        }
        for (K k : m.keySet())
            if (k == null)
                throw new NullPointerException(NULL_KEY);
        if (array_keys != null) {
            convertArrayToHashMap();
        }
        if (hashmap == null) {
            hashmap = new HashMap<>(ARRAY_SIZE + max_new_size);
        }
        if (singleton_key != null) {
            hashmap.put(singleton_key, singleton_value);
            singleton_key = null;
            singleton_value = null;
        }
        hashmap.putAll(m);
    }

    @SuppressWarnings("unchecked")
    @Override
    public V remove(Object key) {
        if (singleton_key != null) {
            if (singleton_key.equals(key)) {
                V old = singleton_value;
                singleton_key = null;
                singleton_value = null;
                return old;
            }
            return null;
        }
        if (array_keys != null) {
            for (int i = 0; i < ARRAY_SIZE; i++) {
                if (array_keys[i] != null && array_keys[i].equals(key)) {
                    V v = (V) array_values[i];
                    array_keys[i] = null;
                    array_values[i] = null;
                    number_of_used_array_entries--;
                    return v;
                }
            }
            return null;
        }
        if (hashmap != null)
            return hashmap.remove(key);
        return null;
    }

    @Override
    public int size() {
        if (singleton_key != null)
            return 1;
        else if (array_keys != null)
            return number_of_used_array_entries;
        else if (hashmap != null)
            return hashmap.size();
        return 0;
    }

    @Override
    public boolean isEmpty() {
        if (singleton_key != null)
            return false;
        if (array_keys != null)
            return number_of_used_array_entries == 0;
        if (hashmap != null)
            return hashmap.isEmpty();
        return true;
    }

    @Override
    public Collection<V> values() {
        if (hashmap != null)
            return hashmap.values();
        return new AbstractCollection<V>() {

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
                if (array_keys != null) {
                    return new ArrayIterator<V>() {
                        @Override
                        public V next() {
                            return nextValue();
                        }
                    };
                }
                return new EmptyIterator<>();
            }

            @Override
            public int size() {
                return HybridArrayHashMap.this.size();
            }

            @Override
            public void clear() {
                HybridArrayHashMap.this.clear();
            }

            @Override
            public boolean contains(Object o) {
                if (singleton_key != null)
                    return singleton_value.equals(o);
                if (array_keys != null) {
                    for (int i = 0; i < ARRAY_SIZE; i++) {
                        if (array_keys[i] != null && array_values[i].equals(o))
                            return true;
                    }
                }
                return false;
            }
        };
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        if (hashmap != null)
            return hashmap.entrySet();
        return new AbstractSet<Entry<K, V>>() {

            @Override
            public Iterator<Entry<K, V>> iterator() {
                if (singleton_key != null) {
                    return new SingletonIterator<Entry<K, V>>() {
                        @Override
                        public Entry<K, V> next() {
                            return nextEntry();
                        }
                    };
                }
                if (array_keys != null) {
                    return new ArrayIterator<Entry<K, V>>() {
                        @Override
                        public Entry<K, V> next() {
                            return nextEntry();
                        }
                    };
                }
                return new EmptyIterator<>();
            }

            @Override
            public int size() {
                return HybridArrayHashMap.this.size();
            }

            @Override
            public void clear() {
                HybridArrayHashMap.this.clear();
            }

            @Override
            public boolean contains(Object o) {
                if (!(o instanceof Entry<?, ?>))
                    return false;
                Entry<?, ?> e = (Entry<?, ?>) o;
                if (singleton_key != null)
                    return singleton_key.equals(e.getKey()) && singleton_value.equals(e.getValue());
                if (array_keys != null) {
                    for (int i = 0; i < ARRAY_SIZE; i++) {
                        if (array_keys[i] != null && array_keys[i].equals(e.getKey()) && array_values[i].equals(e.getValue())) {
                            return true;
                        }
                    }
                }
                return false;
            }
        };
    }

    @Override
    public Set<K> keySet() {
        if (hashmap != null)
            return hashmap.keySet();
        return new AbstractSet<K>() {

            @Override
            public Iterator<K> iterator() {
                if (singleton_key != null) {
                    return new SingletonIterator<K>() {
                        @Override
                        public K next() {
                            return nextKey();
                        }
                    };
                }
                if (array_keys != null) {
                    return new ArrayIterator<K>() {
                        @Override
                        public K next() {
                            return nextKey();
                        }
                    };
                }
                return new EmptyIterator<>();
            }

            @Override
            public int size() {
                return HybridArrayHashMap.this.size();
            }

            @Override
            public void clear() {
                HybridArrayHashMap.this.clear();
            }

            @Override
            public boolean contains(Object o) {
                if (singleton_key != null)
                    return singleton_key.equals(o);
                if (array_keys != null) {
                    for (int i = 0; i < ARRAY_SIZE; i++) {
                        if (array_keys[i] != null && array_keys[i].equals(o))
                            return true;
                    }
                }
                return false;
            }
        };
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Map<?, ?>))
            return false;
        Map<?, ?> m = (Map<?, ?>) obj;
        int this_size = size();
        if (this_size != m.size())
            return false;
        if (this_size == 1 && obj instanceof HybridArrayHashMap<?, ?>) {
            HybridArrayHashMap<?, ?> h = (HybridArrayHashMap<?, ?>) obj;
            if (singleton_key != null && h.singleton_key != null)
                return singleton_key.equals(h.singleton_key) && (Objects.equals(singleton_value, h.singleton_value));
        }
        return entrySet().equals(m.entrySet());
    }

    @Override
    public int hashCode() { // see contract for Map.hashCode
        if (singleton_key != null)
            return singleton_key.hashCode() ^ (singleton_value == null ? 0 : singleton_value.hashCode());
        if (array_keys != null) {
            int h = 0;
            for (int i = 0; i < ARRAY_SIZE; i++) {
                if (array_keys[i] != null)
                    h += array_keys[i].hashCode() ^ (array_values[i] == null ? 0 : array_values[i].hashCode());
            }
            return h;
        }
        if (hashmap != null)
            return hashmap.hashCode();
        return 0;
    }

    @Override
    public String toString() {
        if (singleton_key != null)
            return "[" + singleton_key + '=' + singleton_value + ']';
        if (array_keys != null) {
            StringBuilder b = new StringBuilder();
            b.append('{');
            boolean first = true;
            for (int i = 0; i < ARRAY_SIZE; i++) {
                if (array_keys[i] != null) {
                    if (first)
                        first = false;
                    else
                        b.append(", ");
                    b.append(array_keys[i]).append('=').append(array_values[i]);
                }
            }
            b.append('}');
            return b.toString();
        }
        if (hashmap != null)
            return hashmap.toString();
        return "[]";
    }

    private abstract class ArrayIterator<E> implements Iterator<E> {

        int next, last;

        ArrayIterator() {
            findNext();
            last = -1;
        }

        private void findNext() {
            while (next < ARRAY_SIZE && array_keys[next] == null)
                next++;
        }

        @Override
        public boolean hasNext() {
            return next < ARRAY_SIZE;
        }

        public Entry<K, V> nextEntry() {
            if (next == ARRAY_SIZE)
                throw new NoSuchElementException();
            last = next;
            @SuppressWarnings("unchecked")
            Entry<K, V> e = new MapEntry<>((K) array_keys[next], (V) array_values[next++]);
            findNext();
            return e;
        }

        public K nextKey() {
            if (next == ARRAY_SIZE)
                throw new NoSuchElementException();
            last = next;
            @SuppressWarnings("unchecked")
            K k = (K) array_keys[next++];
            findNext();
            return k;
        }

        public V nextValue() {
            if (next == ARRAY_SIZE)
                throw new NoSuchElementException();
            last = next;
            @SuppressWarnings("unchecked")
            V v = (V) array_values[next++];
            findNext();
            return v;
        }

        @Override
        public void remove() {
            if (last == -1 || array_keys[last] == null)
                throw new IllegalStateException();
            array_keys[last] = null;
            array_values[last] = null;
            number_of_used_array_entries--;
            findNext();
        }
    }

    private abstract class SingletonIterator<E> implements Iterator<E> {

        boolean done;

        @Override
        public boolean hasNext() {
            return !done;
        }

        public Entry<K, V> nextEntry() {
            if (done)
                throw new NoSuchElementException();
            Entry<K, V> e = new MapEntry<>(singleton_key, singleton_value);
            done = true;
            return e;
        }

        public K nextKey() {
            if (done)
                throw new NoSuchElementException();
            done = true;
            return singleton_key;
        }

        public V nextValue() {
            if (done)
                throw new NoSuchElementException();
            done = true;
            return singleton_value;
        }

        @Override
        public void remove() {
            if (done && singleton_key != null) {
                singleton_key = null;
                singleton_value = null;
            } else
                throw new IllegalStateException();
        }
    }

    private static final class EmptyIterator<E> implements Iterator<E> {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public E next() {
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new IllegalStateException();
        }
    }
}
