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

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

public class MapUtils {

    private MapUtils() {
    }

    public static <K, E> boolean addToMapSet(Map<K, Set<E>> map, K key, E element) {
        return map.computeIfAbsent(key, k -> SetUtils.newHybridSet()).add(element);
    }

    public static <K1, K2, V> void addToMapMap(Map<K1, Map<K2, V>> map,
                                               K1 key1, K2 key2, V value) {
        map.computeIfAbsent(key1, k -> newHybridMap()).put(key2, value);
    }

    @Nullable
    public static <K1, K2, V> V getMapMap(
            Map<K1, Map<K2, V>> map, K1 key1, K2 key2) {
        Map<K2, V> map2 = map.get(key1);
        return map2 == null ? null : map2.get(key2);
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

    /**
     * @return a stream of all values of a map of map.
     */
    public static <K1, K2, V> Stream<V> mapMapValues(Map<K1, Map<K2, V>> map) {
        return map.values()
                .stream()
                .flatMap(m -> m.entrySet().stream())
                .map(Map.Entry::getValue);
    }
}
