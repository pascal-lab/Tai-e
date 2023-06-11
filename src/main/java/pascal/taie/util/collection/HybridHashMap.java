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

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Hybrid map that uses hash map for large map.
 */
public final class HybridHashMap<K, V> extends AbstractHybridMap<K, V>
        implements Serializable {

    /**
     * Constructs a new empty hybrid map.
     */
    public HybridHashMap() {
    }

    /**
     * Constructs a new hybrid map from the given map.
     */
    public HybridHashMap(Map<K, V> m) {
        super(m);
    }

    @Override
    protected Map<K, V> newLargeMap(int initialCapacity) {
        return new HashMap<>(initialCapacity);
    }
}
