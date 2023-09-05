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

import pascal.taie.util.function.SSupplier;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;

/**
 * An implementation of {@link MultiMap} that stores key-value pairs
 * as a map from key to the sets of its corresponding values.
 *
 * @param <K> type of the keys in this map
 * @param <V> type of the values in this map
 */
public class MapSetMultiMap<K, V> extends AbstractMultiMap<K, V>
        implements Serializable {

    /**
     * The backing map.
     */
    private final Map<K, Set<V>> map;

    /**
     * Factory function for creating new sets.
     */
    private final SSupplier<Set<V>> setFactory;

    private int size = 0;

    public MapSetMultiMap(Map<K, Set<V>> map, SSupplier<Set<V>> setFactory) {
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
        return values == null ? Set.of() :
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
        return map.computeIfAbsent(key, __ -> setFactory.get());
    }

    @Override
    public boolean putAll(@Nonnull MultiMap<? extends K, ? extends V> multiMap) {
        Objects.requireNonNull(multiMap);
        boolean[] changed = {false};
        multiMap.forEachSet((k, vs) -> changed[0] |= putAll(k, vs));
        return changed[0];
    }

    @Override
    public boolean remove(K key, V value) {
        Collection<V> values = map.get(key);
        if (values != null && values.remove(value)) {
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

        private Iterator<V> valueIt;

        private K currKey;

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
