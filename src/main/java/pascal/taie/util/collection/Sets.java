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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Static utility methods for {@link Set}.
 */
public final class Sets {

    private Sets() {
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

    public static <E> Set<E> newSmallSet() {
        return new ArraySet<>();
    }

    public static <E> Set<E> newHybridSet() {
        return new HybridArrayHashSet<>();
    }

    public static <E> Set<E> newHybridSet(Collection<E> c) {
        return new HybridArrayHashSet<>(c);
    }

    public static <E> Set<E> newHybridOrderedSet() {
        return new HybridArrayLinkedHashSet<>();
    }

    public static <E> Set<E> newConcurrentSet() {
        return ConcurrentHashMap.newKeySet();
    }

    public static BitSet newBitSet() {
        return new SimpleBitSet();
    }

    public static BitSet newBitSet(int nbits) {
        return new SimpleBitSet(nbits);
    }
}
