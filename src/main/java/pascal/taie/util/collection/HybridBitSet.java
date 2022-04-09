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

/**
 * Hybrid set that uses bit set for large set.
 */
public final class HybridBitSet<E> extends AbstractHybridSet<E> {

    private final Indexer<E> indexer;

    private final boolean isSparse;

    public HybridBitSet(Indexer<E> indexer, boolean isSparse) {
        this.indexer = indexer;
        this.isSparse = isSparse;
    }

    @Override
    protected Set<E> newLargeSet(int unused) {
        return new IndexerBitSet<>(indexer, isSparse);
    }

    @Override
    public HybridBitSet<E> addAllDiff(Collection<? extends E> c) {
        HybridBitSet<E> diff = new HybridBitSet<>(indexer, isSparse);
        if (c instanceof HybridBitSet other && other.isLargeSet) {
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
    public HybridBitSet<E> copy() {
        HybridBitSet<E> copy = new HybridBitSet<>(indexer, isSparse);
        copy.singleton = singleton;
        copy.isLargeSet = isLargeSet;
        if (set != null) {
            copy.set = ((EnhancedSet<E>) set).copy();
        }
        return copy;
    }
}
