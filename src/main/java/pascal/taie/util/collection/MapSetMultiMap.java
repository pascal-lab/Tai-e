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
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * An implementation of {@link MultiMap} that stores key-value pairs
 * as a map from key to the sets of its corresponding values.
 *
 * @param <K> type of the keys in this map
 * @param <V> type of the values in this map
 */
public class MapSetMultiMap<K, V> extends AbstractMultiMap<K, V> {

    /**
     * The backing map.
     */
    private final Map<K, Set<V>> map;

    /**
     * Factory function for creating new sets.
     */
    private final Supplier<Set<V>> setFactory;

    private int size = 0;

    public MapSetMultiMap(Map<K, Set<V>> map, Supplier<Set<V>> setFactory) {
        this.map = map;
        this.setFactory = setFactory;
    }

    @Override
    public boolean contains(K key, V value) {
        return get(key).contains(value);
    }

    @Override
    public boolean containsKey(K key) {
        return map.containsKey(key);
    }

    @Override
    public Set<V> get(@Nonnull K key) {
        Objects.requireNonNull(key, NULL_KEY);
        Set<V> values = map.get(key);
        return values == null ? Collections.emptySet() :
                Collections.unmodifiableSet(values);
    }

    @Override
    public boolean put(@Nonnull K key, @Nonnull V value) {
        Objects.requireNonNull(key, NULL_KEY);
        Objects.requireNonNull(value, NULL_VALUE);
        if (getOrCreateSet(key).add(value)) {
            ++size;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean putAll(@Nonnull K key, @Nonnull Collection<? extends V> values) {
        Objects.requireNonNull(key, NULL_KEY);
        Objects.requireNonNull(values);
        if (!values.isEmpty()) {
            Set<V> set = getOrCreateSet(key);
            int diff = 0;
            for (V v : values) {
                if (set.add(Objects.requireNonNull(v, NULL_VALUE))) {
                    ++diff;
                }
            }
            if (diff > 0) {
                size += diff;
                return true;
            }
        }
        return false;
    }

    private Set<V> getOrCreateSet(@Nonnull K key) {
        return map.computeIfAbsent(key, unused -> setFactory.get());
    }

    @Override
    public boolean putAll(@Nonnull MultiMap<K, V> multiMap) {
        Objects.requireNonNull(multiMap);
        boolean[] changed = { false };
        multiMap.forEachSet((k, vs) -> changed[0] |= putAll(k, vs));
        return changed[0];
    }

    @Override
    public boolean remove(K key, V value) {
        Collection<V> values = map.get(key);
        if (values != null && values.remove(value)){
            if (values.isEmpty()) {
                map.remove(key);
            }
            --size;
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean removeAll(K key) {
        Set<V> pre = map.remove(key);
        if (pre != null) {
            size -= pre.size();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean removeAll(K key, Collection<? extends V> values) {
        Collection<V> currValues = map.get(key);
        if (currValues != null) {
            int beforeSize = currValues.size();
            currValues.removeAll(values);
            int diff = beforeSize - currValues.size();
            if (diff > 0) {
                size -= diff;
                if (currValues.isEmpty()) {
                    map.remove(key);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<K> keySet() {
        return Collections.unmodifiableSet(map.keySet());
    }

    @Override
    protected Iterator<Map.Entry<K, V>> entryIterator() {
        return new EntryIterator();
    }

    private final class EntryIterator implements Iterator<Map.Entry<K, V>> {

        private final Iterator<Map.Entry<K, Set<V>>> mapIt;

        private K currKey;

        private Iterator<V> valueIt;

        private EntryIterator() {
            mapIt = map.entrySet().iterator();
            if (mapIt.hasNext()) {
                advanceKey();
            } else {
                valueIt = Collections.emptyIterator();
            }
        }

        @Override
        public boolean hasNext() {
            return valueIt.hasNext() || mapIt.hasNext();
        }

        @Override
        public Map.Entry<K, V> next() {
            if (valueIt.hasNext()) {
                return new ImmutableMapEntry<>(currKey, valueIt.next());
            } else if (mapIt.hasNext()) {
                advanceKey();
                return new ImmutableMapEntry<>(currKey, valueIt.next());
            } else {
                throw new NoSuchElementException();
            }
        }

        private void advanceKey() {
            var entry = mapIt.next();
            currKey = entry.getKey();
            valueIt = entry.getValue().iterator();
        }
    }

    @Override
    public void forEachSet(@Nonnull BiConsumer<K, Set<V>> action) {
        map.forEach(action);
    }

    @Override
    public void clear() {
        map.clear();
        size = 0;
    }

    @Override
    public int size() {
        return size;
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }
}
