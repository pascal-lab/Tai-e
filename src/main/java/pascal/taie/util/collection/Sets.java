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

import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeSet;
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

    public static <E> Set<E> newSet(Collection<? extends E> set) {
        return new HashSet<>(set);
    }

    public static <E> Set<E> newLinkedSet() {
        return new LinkedHashSet<>();
    }

    public static <E> Set<E> newSet(int initialCapacity) {
        if (initialCapacity <= ArraySet.DEFAULT_CAPACITY) {
            return newSmallSet();
        } else {
            return newSet();
        }
    }

    public static <E extends Comparable<E>> TreeSet<E> newOrderedSet() {
        return new TreeSet<>();
    }

    public static <E> TreeSet<E> newOrderedSet(Comparator<? super E> comparator) {
        return new TreeSet<>(comparator);
    }

    public static <E> Set<E> newSmallSet() {
        return new ArraySet<>();
    }

    public static <E> Set<E> newHybridSet() {
        return new HybridHashSet<>();
    }

    public static <E> Set<E> newHybridSet(Collection<E> c) {
        return new HybridHashSet<>(c);
    }

    public static <E> Set<E> newHybridOrderedSet() {
        return new HybridLinkedHashSet<>();
    }

    public static <E> Set<E> newConcurrentSet() {
        return ConcurrentHashMap.newKeySet();
    }
}
