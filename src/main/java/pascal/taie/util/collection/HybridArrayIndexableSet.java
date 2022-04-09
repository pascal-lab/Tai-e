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

public class HybridArrayIndexableSet<E extends Indexable>
        extends AbstractHybridSet<E> {

    /**
     * Default threshold for the number of items necessary for the array set
     * to become a hash set.
     */
    private static final int ARRAY_SET_SIZE = 8;

    private final boolean isSparse;

    public HybridArrayIndexableSet(boolean isSparse) {
        this.isSparse = isSparse;
    }

    @Override
    protected int getThreshold() {
        return ARRAY_SET_SIZE;
    }

    @Override
    protected Set<E> newSmallSet() {
        return new ArraySet<>(getThreshold());
    }

    @Override
    protected Set<E> newLargeSet(int unused) {
        return new IndexableSet<>(isSparse);
    }
}
