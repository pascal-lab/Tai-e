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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Static utility methods for various maps, including {@link Map},
 * {@link MultiMap}, and {@link TwoKeyMap}.
 */
public final class Maps {

    private Maps() {
    }

    public static <K, V> Map<K, V> newMap() {
        return new HashMap<>();
    }

    public static <K, V> Map<K, V> newMap(int initialCapacity) {
        if (initialCapacity <= ArrayMap.DEFAULT_CAPACITY) {
            return newSmallMap();
        } else {
            return newMap();
        }
    }

    public static <K, V> Map<K, V> newSmallMap() {
        return new ArrayMap<>();
    }

    public static <K, V> Map<K, V> newHybridMap() {
        return new HybridArrayHashMap<>();
    }

    public static <K, V> Map<K, V> newHybridMap(Map<K, V> map) {
        return new HybridArrayHashMap<>(map);
    }

    public static <K, V> ConcurrentMap<K, V> newConcurrentMap() {
        return new ConcurrentHashMap<>();
    }

    public static <K, V> ConcurrentMap<K, V> newConcurrentMap(int initialCapacity) {
        return new ConcurrentHashMap<>(initialCapacity);
    }

    public static <K, V> MultiMap<K, V> newMultiMap() {
        return new MapSetMultiMap<>(newMap(), HybridArrayHashSet::new);
    }

    public static <K, V> MultiMap<K, V> newMultiMap(Map<K, Set<V>> map) {
        return new MapSetMultiMap<>(map, HybridArrayHashSet::new);
    }

    public static <K, V> MultiMap<K, V> newMultiMap(int initialCapacity) {
        return new MapSetMultiMap<>(
                newMap(initialCapacity), HybridArrayHashSet::new);
    }

    public static <K1, K2, V> TwoKeyMap<K1, K2, V> newTwoKeyMap() {
        return new MapMapTwoKeyMap<>(newMap(), HybridArrayHashMap::new);
    }
}
