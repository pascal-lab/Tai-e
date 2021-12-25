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
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Implements {@link TwoKeyMap} as map of maps.
 */
public class MapMapTwoKeyMap<K1, K2, V> extends
        AbstractTwoKeyMap<K1, K2, V> {

    /**
     * The backing map.
     */
    private final Map<K1, Map<K2, V>> map;

    /**
     * Factory function for creating new maps.
     */
    private final Supplier<Map<K2, V>> mapFactory;

    private int size = 0;

    public MapMapTwoKeyMap(Map<K1, Map<K2, V>> map, Supplier<Map<K2, V>> mapFactory) {
        this.map = map;
        this.mapFactory = mapFactory;
    }

    @Nullable
    @Override
    public Map<K2, V> get(K1 key1) {
        var m = map.get(key1);
        return m == null ? null : Collections.unmodifiableMap(m);
    }

    @Nullable
    @Override
    public V put(@Nonnull K1 key1, @Nonnull K2 key2, @Nonnull V value) {
        Objects.requireNonNull(key1, NULL_KEY);
        Objects.requireNonNull(key2, NULL_KEY);
        Objects.requireNonNull(value, NULL_VALUE);
        V oldV = getOrCreateMap(key1).put(key2, value);
        if (oldV == null) {
            ++size;
        }
        return oldV;
    }

    @Override
    public void putAll(@Nonnull K1 key1, @Nonnull Map<K2, V> map) {
        Objects.requireNonNull(key1, NULL_KEY);
        Objects.requireNonNull(map);
        map.forEach((k2, v) -> put(key1, k2, v));
    }

    @Override
    public void putAll(@Nonnull TwoKeyMap<K1, K2, V> twoKeyMap) {
        Objects.requireNonNull(twoKeyMap);
        twoKeyMap.forEach(this::put);
    }

    private Map<K2, V> getOrCreateMap(@Nonnull K1 key1) {
        return map.computeIfAbsent(key1, unused -> mapFactory.get());
    }

    @Nullable
    @Override
    public V remove(K1 key1, K2 key2) {
        Map<K2, V> mappings = map.get(key1);
        V oldV = null;
        if (mappings != null) {
            oldV = mappings.remove(key2);
            if (oldV != null) {
                --size;
            }
            if (mappings.isEmpty()) {
                map.remove(key1);
            }
        }
        return oldV;
    }

    @Override
    public boolean removeAll(K1 key1) {
        Map<K2, V> oldMappings = map.remove(key1);
        if (oldMappings != null) {
            size -= oldMappings.size();
            return true;
        }
        return false;
    }

    @Override
    public Set<K1> keySet() {
        return Collections.unmodifiableSet(map.keySet());
    }

    @Override
    protected Iterator<TwoKeyMap.Entry<K1, K2, V>> entryIterator() {
        return new EntryIterator();
    }

    private final class EntryIterator implements Iterator<TwoKeyMap.Entry<K1, K2, V>> {

        private final Iterator<Map.Entry<K1, Map<K2, V>>> mapIt;

        private K1 currKey1;

        private Iterator<Map.Entry<K2, V>> key2ValIt;

        private EntryIterator() {
            this.mapIt = map.entrySet().iterator();
            if (mapIt.hasNext()) {
                advanceKey1();
            } else {
                key2ValIt = Collections.emptyIterator();
            }
        }

        @Override
        public boolean hasNext() {
            return key2ValIt.hasNext() || mapIt.hasNext();
        }

        @Override
        public TwoKeyMap.Entry<K1, K2, V> next() {
            if (key2ValIt.hasNext()) {
                var next = key2ValIt.next();
                return new Entry<>(currKey1, next.getKey(), next.getValue());
            } else if (mapIt.hasNext()) {
                advanceKey1();
                var next = key2ValIt.next();
                return new Entry<>(currKey1, next.getKey(), next.getValue());
            } else {
                throw new NoSuchElementException();
            }
        }

        private void advanceKey1() {
            var entry = mapIt.next();
            currKey1 = entry.getKey();
            key2ValIt = entry.getValue().entrySet().iterator();
        }
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
}
