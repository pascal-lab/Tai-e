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

import javax.annotation.Nonnull;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

public abstract class AbstractMultiMap<K, V> implements MultiMap<K, V> {

    protected static final String NULL_KEY = "MultiMap does not permit null keys";

    protected static final String NULL_VALUE = "MultiMap does not permit null values";

    @Override
    public boolean containsValue(V value) {
        for (K key : keySet()) {
            if (get(key).contains(value)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The cache of {@link AbstractMultiMap#entrySet()}.
     */
    private transient Set<Map.Entry<K, V>> entrySet;

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        var es = entrySet;
        if (es == null) {
            es = Collections.unmodifiableSet(new EntrySet());
            entrySet = es;
        }
        return es;
    }

    private final class EntrySet extends AbstractSet<Map.Entry<K, V>> {

        @Override
        public boolean contains(Object o) {
            if (o instanceof Map.Entry<?, ?> entry) {
                //noinspection unchecked
                return AbstractMultiMap.this.contains(
                        (K) entry.getKey(), (V) entry.getValue());
            }
            return false;
        }

        @Override
        @Nonnull
        public Iterator<Map.Entry<K, V>> iterator() {
            return entryIterator();
        }

        @Override
        public int size() {
            return AbstractMultiMap.this.size();
        }
    }

    protected abstract Iterator<Map.Entry<K, V>> entryIterator();

    /**
     * The cache of {@link AbstractMultiMap#values()}.
     */
    private transient Collection<V> values;

    @Override
    public Collection<V> values() {
        Collection<V> vals = values;
        if (vals == null) {
            vals = Views.toMappedCollection(entrySet(), Map.Entry::getValue,
                    o -> containsValue((V) o));
            values = vals;
        }
        return vals;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MultiMap)) {
            return false;
        }
        @SuppressWarnings("unchecked")
        MultiMap<K, V> that = (MultiMap<K, V>) o;
        if (size() != that.size()) {
            return false;
        }
        try {
            for (K key : keySet()) {
                if (!get(key).equals(that.get(key))) {
                    return false;
                }
            }
        } catch (ClassCastException | NullPointerException ignored) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringJoiner joiner = new StringJoiner(", ", "{", "}");
        for (K key : keySet()) {
            joiner.add(key + "=" + get(key));
        }
        return joiner.toString();
    }
}
