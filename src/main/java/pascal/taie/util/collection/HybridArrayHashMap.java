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

import java.util.HashMap;
import java.util.Map;

/**
 * Hybrid of array map (for small map) and hash map (for large map).
 */
public final class HybridArrayHashMap<K, V> extends AbstractHybridMap<K, V> {

    /**
     * Threshold for the number of items necessary for the array map
     * to become a hash map.
     */
    private static final int ARRAY_MAP_SIZE = 8;

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
        super(m);
    }

    @Override
    protected int getThreshold() {
        return ARRAY_MAP_SIZE;
    }

    @Override
    protected Map<K, V> newSmallMap(int initialCapacity) {
        return new ArrayMap<>(initialCapacity);
    }

    @Override
    protected Map<K, V> newLargeMap(int initialCapacity) {
        return new HashMap<>(initialCapacity);
    }
}
