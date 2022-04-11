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

import pascal.taie.util.Copyable;

import java.util.Collection;
import java.util.Set;

/**
 * This interface extends {@link Set} to provide more useful APIs.
 *
 * @param <E> type of elements in this set
 */
public interface EnhancedSet<E> extends Set<E>, Copyable<EnhancedSet<E>> {

    /**
     * Adds all elements in collection {@code c}, and returns the difference set
     * between {@code c} and this set (before the call).
     *
     * @return a set of elements that are contained in {@code c} but
     * not in this set before the call.
     */
    EnhancedSet<E> addAllDiff(Collection<? extends E> c);
}
