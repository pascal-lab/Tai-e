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

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Static utility methods for various maps, including {@link Map},
 * {@link MultiMap}, and {@link TwoKeyMap}.
 */
public final class Maps {

    private Maps() {
    }

    public static <K, V> Map<K, V> ofLinkedHashMap(K k1, V v1, K k2, V v2) {
        Map<K, V> map = new LinkedHashMap<>(2);
        map.put(k1, v1);
        map.put(k2, v2);
        return Collections.unmodifiableMap(map);
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

    public static <K, V> Map<K, V> newLinkedHashMap() {
        return new LinkedHashMap<>();
    }

    public static <K extends Comparable<K>, V> Map<K, V> newOrderedMap() {
        return new TreeMap<>();
    }

    public static <K, V> Map<K, V> newOrderedMap(Comparator<? super K> comparator) {
        return new TreeMap<>(comparator);
    }

    public static <K, V> Map<K, V> newSmallMap() {
        return new ArrayMap<>();
    }

    public static <K, V> Map<K, V> newHybridMap() {
        return new HybridHashMap<>();
    }

    public static <K, V> Map<K, V> newHybridMap(Map<K, V> map) {
        return new HybridHashMap<>(map);
    }

    public static <K, V> ConcurrentMap<K, V> newConcurrentMap() {
        return new ConcurrentHashMap<>();
    }

    public static <K, V> ConcurrentMap<K, V> newConcurrentMap(int initialCapacity) {
        return new ConcurrentHashMap<>(initialCapacity);
    }

    public static <K, V> MultiMap<K, V> newMultiMap(Map<K, Set<V>> map,
                                                    SSupplier<Set<V>> setFactory) {
        return new MapSetMultiMap<>(map, setFactory);
    }

    public static <K, V> MultiMap<K, V> newMultiMap(SSupplier<Set<V>> setFactory) {
        return newMultiMap(newMap(), setFactory);
    }

    public static <K, V> MultiMap<K, V> newMultiMap(Map<K, Set<V>> map) {
        return newMultiMap(map, Sets::newHybridSet);
    }

    public static <K, V> MultiMap<K, V> newMultiMap() {
        return newMultiMap(newMap(), Sets::newHybridSet);
    }

    public static <K, V> MultiMap<K, V> newMultiMap(int initialCapacity) {
        return new MapSetMultiMap<>(
                newMap(initialCapacity), Sets::newHybridSet);
    }

    public static <K, V> MultiMap<K, V> unmodifiableMultiMap(MultiMap<K, V> map) {
        if (map instanceof UnmodifiableMultiMap<K, V>) {
            return map;
        }
        return new UnmodifiableMultiMap<>(map);
    }

    @SuppressWarnings("rawtypes")
    private static final MultiMap EMPTY_MULTIMAP = unmodifiableMultiMap(newMultiMap());

    @SuppressWarnings("unchecked")
    public static <K, V> MultiMap<K, V> emptyMultiMap() {
        return (MultiMap<K, V>) EMPTY_MULTIMAP;
    }

    public static <K1, K2, V> TwoKeyMap<K1, K2, V> newTwoKeyMap() {
        return newTwoKeyMap(newMap(), Maps::newHybridMap);
    }

    public static <K1, K2, V> TwoKeyMap<K1, K2, V> newTwoKeyMap(
            Map<K1, Map<K2, V>> map1,
            SSupplier<Map<K2, V>> map2Factory) {
        return new MapMapTwoKeyMap<>(map1, map2Factory);
    }

    public static <K1, K2, V> TwoKeyMultiMap<K1, K2, V> newTwoKeyMultiMap() {
        return new MapMultiMapTwoKeyMultiMap<>(newMap(), Maps::newMultiMap);
    }

    public static <K1, K2, V> TwoKeyMultiMap<K1, K2, V> newTwoKeyMultiMap(
            Map<K1, MultiMap<K2, V>> map,
            SSupplier<MultiMap<K2, V>> multimapFactory) {
        return new MapMultiMapTwoKeyMultiMap<>(map, multimapFactory);
    }

    public static <K1, K2, V> TwoKeyMultiMap<K1, K2, V> unmodifiableTwoKeyMultiMap(
            TwoKeyMultiMap<K1, K2, V> map) {
        if (map instanceof UnmodifiableTwoKeyMultiMap<K1, K2, V>) {
            return map;
        }
        return new UnmodifiableTwoKeyMultiMap<>(map);
    }

    @SuppressWarnings("rawtypes")
    private static final TwoKeyMultiMap EMPTY_TWO_KEY_MULTIMAP =
            unmodifiableTwoKeyMultiMap(newTwoKeyMultiMap());

    @SuppressWarnings("unchecked")
    public static <K1, K2, V> TwoKeyMultiMap<K1, K2, V> emptyTwoKeyMultiMap() {
        return (TwoKeyMultiMap<K1, K2, V>) EMPTY_TWO_KEY_MULTIMAP;
    }
}
