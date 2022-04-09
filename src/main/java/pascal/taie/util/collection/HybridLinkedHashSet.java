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

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Hybrid set that uses linked hash set for large set.
 */
public final class HybridLinkedHashSet<E> extends AbstractHybridSet<E> {

    @Override
    protected Set<E> newLargeSet(int initialCapacity) {
        return new LinkedHashSet<>(initialCapacity);
    }

    @Override
    protected EnhancedSet<E> newSet() {
        return new HybridLinkedHashSet<>();
    }
}
