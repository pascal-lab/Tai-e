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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Provides convenient utility operations for collections.
 */
public class CollectionUtils {

    // Suppresses default constructor, ensuring non-instantiability.
    private CollectionUtils() {
    }

    public static <K, E> boolean addToMapSet(Map<K, Set<E>> map, K key, E element) {
        return map.computeIfAbsent(key, k -> newSet()).add(element);
    }

    public static <K1, K2, V> void addToMapMap(Map<K1, Map<K2, V>> map,
                                               K1 key1, K2 key2, V value) {
        map.computeIfAbsent(key1, k -> newMap()).put(key2, value);
    }

    // Factory methods for sets and maps
    public static <E> Set<E> newSet() {
        return new HashSet<>();
    }

    public static <E> Set<E> newSet(int initialCapacity) {
        if (initialCapacity <= ArraySet.DEFAULT_CAPACITY) {
            return newSmallSet();
        } else {
            return newSet();
        }
    }

    // TODO: deprecate this and use Sets.of() after upgrading to Java 9.
    public static <E> Set<E> newSet(E... elems) {
        return Arrays.stream(elems).collect(Collectors.toSet());
    }

    public static <E> Set<E> newSmallSet() {
        return new ArraySet<>();
    }

    public static <E> Set<E> newHybridSet() {
        return new HybridArrayHashSet<>();
    }

    public static <E> Set<E> newConcurrentSet() {
        return ConcurrentHashMap.newKeySet();
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

    public static <K, V> ConcurrentMap<K, V> newConcurrentMap() {
        return new ConcurrentHashMap<>();
    }

    public static <K, V> ConcurrentMap<K, V> newConcurrentMap(int initialCapacity) {
        return new ConcurrentHashMap<>(initialCapacity);
    }

    /**
     * @return a stream of all values of a map of map.
     */
    public static <K1, K2, V> Stream<V> getAllValues(Map<K1, Map<K2, V>> map) {
        return map.values()
                .stream()
                .flatMap(m -> m.entrySet().stream())
                .map(Map.Entry::getValue);
    }

    /**
     * @return an arbitrary element of the given collection.
     */
    public static <T> T getOne(Collection<T> collection) {
        return collection.iterator().next();
    }

    /**
     * @return an unmodifiable list of the given elements.
     */
    public static <T> List<T> freeze(Collection<T> elements) {
        switch (elements.size()) {
            case 0:
                return Collections.emptyList();
            case 1:
                return Collections.singletonList(getOne(elements));
            default:
                return Collections.unmodifiableList(new ArrayList<>(elements));
        }
    }
}
