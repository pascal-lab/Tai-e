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
import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public abstract class AbstractMultiMap<K, V> implements MultiMap<K, V> {

    @Override
    public boolean containsValue(V value) {
        for (K key : keySet()) {
            if (get(key).contains(value)) {
                return true;
            }
        }
        return false;
    }

    private Set<Map.Entry<K, V>> entrySet;

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
            if (o instanceof Map.Entry) {
                Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
                //noinspection unchecked
                return AbstractMultiMap.this.contains(
                        (K) entry.getKey(), (V) entry.getValue());
            }
            return false;
        }

        @Nonnull
        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            return entryIterator();
        }

        @Override
        public int size() {
            return AbstractMultiMap.this.size();
        }
    }

    protected abstract Iterator<Map.Entry<K, V>> entryIterator();

    private Collection<V> values;

    @Override
    public Collection<V> values() {
        Collection<V> vals = values;
        if (vals == null) {
            vals = Collections.unmodifiableCollection(new Values());
            values = vals;
        }
        return vals;
    }

    private final class Values extends AbstractCollection<V> {

        @Override
        public boolean contains(Object o) {
            return AbstractMultiMap.this.containsValue((V) o);
        }

        @Nonnull
        @Override
        public Iterator<V> iterator() {
            return new Iterator<V>() {
                private final Iterator<Map.Entry<K, V>> i = entrySet().iterator();

                @Override
                public boolean hasNext() {
                    return i.hasNext();
                }

                @Override
                public V next() {
                    return i.next().getValue();
                }
            };
        }

        @Override
        public int size() {
            return AbstractMultiMap.this.size();
        }
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
        if (!(o instanceof MultiMap<?, ?>)) {
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
        } catch (ClassCastException | NullPointerException unused) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        if (!isEmpty()) {
            sb.append('\n');
        }
        for (K key : keySet()) {
            sb.append("  ")
                    .append(key).append('=')
                    .append(get(key)).append('\n');
        }
        sb.append('}');
        return sb.toString();
    }
}
