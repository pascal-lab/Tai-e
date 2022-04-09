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

/**
 * Hybrid set that uses hash set for large set.
 */
public final class HybridHashSet<E> extends AbstractHybridSet<E> {

    /**
     * Constructs a new hybrid set.
     */
    public HybridHashSet() {
    }

    /**
     * Constructs a new hybrid set from the given collection.
     */
    public HybridHashSet(Collection<E> c) {
        super(c);
    }

    @Override
    protected Set<E> newLargeSet(int initialCapacity) {
        return new HashSet<>(initialCapacity);
    }

    @Override
    protected EnhancedSet<E> newSet() {
        return new HybridHashSet<>();
    }
}
