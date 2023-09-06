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
import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

/**
 * Implements {@link TwoKeyMultiMap} as map of multimaps.
 */
public class MapMultiMapTwoKeyMultiMap<K1, K2, V> extends
        AbstractTwoKeyMultiMap<K1, K2, V> {

    /**
     * The backing map.
     */
    private final Map<K1, MultiMap<K2, V>> map;

    /**
     * Factory function for creating multimap.
     */
    private final SSupplier<MultiMap<K2, V>> multimapFactory;

    private int size = 0;

    public MapMultiMapTwoKeyMultiMap(Map<K1, MultiMap<K2, V>> map,
                                     SSupplier<MultiMap<K2, V>> multimapFactory) {
        this.map = map;
        this.multimapFactory = multimapFactory;
    }

    @Override
    public boolean contains(K1 key1, K2 key2, V value) {
        MultiMap<K2, V> mm = map.get(key1);
        return mm != null && mm.contains(key2, value);
    }

    @Override
    public boolean containsKey(K1 key1, K2 key2) {
        MultiMap<K2, V> mm = map.get(key1);
        return mm != null && mm.containsKey(key2);
    }

    @Override
    public boolean containsKey(K1 key1) {
        return map.containsKey(key1);
    }

    @Override
    public boolean containsValue(V value) {
        for (MultiMap<K2, V> mm : map.values()) {
            if (mm.containsValue(value)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Set<V> get(K1 key1, K2 key2) {
        MultiMap<K2, V> mm = map.get(key1);
        return mm == null ? Set.of() : mm.get(key2);
    }

    @Override
    public MultiMap<K2, V> get(K1 key1) {
        MultiMap<K2, V> mm = map.get(key1);
        return mm == null ? Maps.newMultiMap() : Maps.unmodifiableMultiMap(mm);
    }

    @Override
    public boolean put(@Nonnull K1 key1, @Nonnull K2 key2, @Nonnull V value) {
        Objects.requireNonNull(key1, NULL_KEY);
        Objects.requireNonNull(key2, NULL_KEY);
        Objects.requireNonNull(value, NULL_VALUE);
        if (getOrCreateMultiMap(key1).put(key2, value)) {
            ++size;
            return true;
        } else {
            return false;
        }
    }

    private MultiMap<K2, V> getOrCreateMultiMap(K1 key1) {
        return map.computeIfAbsent(key1, __ -> multimapFactory.get());
    }

    @Override
    public boolean remove(K1 key1, K2 key2, V value) {
        MultiMap<K2, V> mm = map.get(key1);
        if (mm != null && mm.remove(key2, value)) {
            --size;
            if (mm.isEmpty()) {
                map.remove(key1);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean removeAll(K1 key1, K2 key2) {
        MultiMap<K2, V> mm = map.get(key1);
        if (mm != null) {
            int preSize = mm.size();
            if (mm.removeAll(key2)) {
                size -= (preSize - mm.size());
                if (mm.isEmpty()) {
                    map.remove(key1);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    protected Iterator<TwoKeyMap.Entry<K1, K2, V>> entryIterator() {
        return new EntryIterator();
    }

    private final class EntryIterator implements Iterator<TwoKeyMap.Entry<K1, K2, V>> {

        private final Iterator<Map.Entry<K1, MultiMap<K2, V>>> mapIt;

        private Iterator<Map.Entry<K2, V>> mmIt;

        private K1 currKey1;

        private EntryIterator() {
            this.mapIt = map.entrySet().iterator();
            if (mapIt.hasNext()) {
                advanceKey1();
            } else {
                mmIt = Collections.emptyIterator();
            }
        }

        @Override
        public boolean hasNext() {
            return mmIt.hasNext() || mapIt.hasNext();
        }

        @Override
        public TwoKeyMap.Entry<K1, K2, V> next() {
            if (mmIt.hasNext()) {
                var mmEntry = mmIt.next();
                return new TwoKeyMap.Entry<>(currKey1, mmEntry.getKey(), mmEntry.getValue());
            } else if (mapIt.hasNext()) {
                advanceKey1();
                var mmEntry = mmIt.next();
                return new TwoKeyMap.Entry<>(currKey1, mmEntry.getKey(), mmEntry.getValue());
            } else {
                throw new NoSuchElementException();
            }
        }

        private void advanceKey1() {
            var entry = mapIt.next();
            currKey1 = entry.getKey();
            mmIt = entry.getValue().entrySet().iterator();
        }
    }

    private transient Set<Pair<K1, K2>> twoKeySet;

    @Override
    public Set<Pair<K1, K2>> twoKeySet() {
        var tks = twoKeySet;
        if (tks == null) {
            tks = Collections.unmodifiableSet(new TwoKeySet());
            twoKeySet = tks;
        }
        return tks;
    }

    private final class TwoKeySet extends AbstractSet<Pair<K1, K2>> {

        @Override
        public boolean contains(Object o) {
            if (o instanceof Pair<?, ?> pair) {
                //noinspection unchecked
                return MapMultiMapTwoKeyMultiMap.this.containsKey(
                        (K1) pair.first(), (K2) pair.second());
            }
            return false;
        }

        @Nonnull
        @Override
        public Iterator<Pair<K1, K2>> iterator() {
            return new TwoKeyIterator();
        }

        @Override
        public int size() {
            int size = 0;
            for (var entry : map.entrySet()) {
                size += entry.getValue().keySet().size();
            }
            return size;
        }
    }

    private final class TwoKeyIterator implements Iterator<Pair<K1, K2>> {

        private final Iterator<Map.Entry<K1, MultiMap<K2, V>>> mapIt;

        private Iterator<K2> key2It;

        private K1 currKey1;

        private TwoKeyIterator() {
            this.mapIt = map.entrySet().iterator();
            if (mapIt.hasNext()) {
                advanceKey1();
            } else {
                key2It = Collections.emptyIterator();
            }
        }

        @Override
        public boolean hasNext() {
            return key2It.hasNext() || mapIt.hasNext();
        }

        @Override
        public Pair<K1, K2> next() {
            if (key2It.hasNext()) {
                return new Pair<>(currKey1, key2It.next());
            } else if (mapIt.hasNext()) {
                advanceKey1();
                return new Pair<>(currKey1, key2It.next());
            } else {
                throw new NoSuchElementException();
            }
        }

        private void advanceKey1() {
            var entry = mapIt.next();
            currKey1 = entry.getKey();
            key2It = entry.getValue().keySet().iterator();
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

    @Override
    public int hashCode() {
        return map.hashCode();
    }
}
