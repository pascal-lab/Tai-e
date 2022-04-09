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

import pascal.taie.util.Indexable;

import java.util.Set;

/**
 * Hybrid set that uses indexable set for large set.
 */
public class HybridIndexableSet<E extends Indexable>
        extends AbstractHybridSet<E> {

    private final boolean isSparse;

    public HybridIndexableSet(boolean isSparse) {
        this.isSparse = isSparse;
    }

    @Override
    protected Set<E> newLargeSet(int unused) {
        return new IndexableSet<>(isSparse);
    }
}
