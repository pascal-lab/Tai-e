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

import pascal.taie.util.Indexer;

import java.util.Collection;
import java.util.Set;

public final class HybridArrayBitSet<E> extends AbstractHybridSet<E> {

    /**
     * Default threshold for the number of items necessary for the array set
     * to become a hash set.
     */
    private static final int ARRAY_SET_SIZE = 8;

    private final Indexer<E> indexer;

    private final boolean isSparse;

    public HybridArrayBitSet(Indexer<E> indexer, boolean isSparse) {
        this.indexer = indexer;
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
        return new IndexerBitSet<>(indexer, isSparse);
    }

    @Override
    public HybridArrayBitSet<E> addAllDiff(Collection<? extends E> c) {
        HybridArrayBitSet<E> diff = new HybridArrayBitSet<>(indexer, isSparse);
        if (c instanceof HybridArrayBitSet other && other.isLargeSet) {
            //noinspection unchecked
            EnhancedSet<E> otherSet = (EnhancedSet<E>) other.set;
            Set<E> diffSet;
            if (set == null) {
                set = otherSet.copy();
                diffSet = otherSet.copy();
                if (singleton != null) {
                    set.add(singleton);
                    diffSet.remove(singleton);
                    singleton = null;
                }
            } else if (!isLargeSet) {
                // current set is small set
                Set<E> oldSet = set;
                set = otherSet.copy();
                diffSet = otherSet.copy();
                set.addAll(oldSet);
                diffSet.removeAll(oldSet);
            } else {
                // current set is already a large set
                diffSet = ((EnhancedSet<E>) set).addAllDiff(otherSet);
            }
            diff.set = diffSet;
            diff.isLargeSet = isLargeSet = true;
        } else {
            for (E e : c) {
                if (add(e)) {
                    diff.add(e);
                }
            }
        }
        return diff;
    }

    @Override
    public HybridArrayBitSet<E> copy() {
        HybridArrayBitSet<E> copy = new HybridArrayBitSet<>(indexer, isSparse);
        copy.singleton = singleton;
        copy.isLargeSet = isLargeSet;
        if (set != null) {
            copy.set = ((EnhancedSet<E>) set).copy();
        }
        return copy;
    }
}
