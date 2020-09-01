/*
 * Copyright 2009-2019 Aarhus University
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package panda.util;

import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Map entry.
 * Pair of a key and a value.
 */
class MapEntry<K, V> implements Entry<K, V>, Serializable {

    private final K key;

    private V value;

    /**
     * Constructs a new map entry.
     */
    public MapEntry(K key, V value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public K getKey() {
        return key;
    }

    @Override
    public V getValue() {
        return value;
    }

    @Override
    public V setValue(V value) {
        V old = this.value;
        this.value = value;
        return old;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this)
            return true;
        if (!(obj instanceof Map.Entry<?, ?>))
            return false;
        Entry<?, ?> e = (Entry<?, ?>) obj;
        return (key == null ? e.getKey() == null : key.equals(e.getKey()))
                && (value == null ? e.getValue() == null : value.equals(e.getValue()));
    }

    @Override
    public int hashCode() {
        return (key == null ? 0 : key.hashCode()) ^ (value == null ? 0 : value.hashCode());
    }
}
