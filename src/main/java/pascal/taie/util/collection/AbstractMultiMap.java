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
