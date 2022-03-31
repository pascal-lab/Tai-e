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

package pascal.taie.analysis.pta.pts;

import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.util.Indexer;
import pascal.taie.util.collection.HybridArrayBitSet;

import java.util.Set;

class HybridArrayBitPointsToSet extends DelegatePointsToSet {

    public HybridArrayBitPointsToSet(Indexer<CSObj> indexer) {
        this(new HybridArrayBitSet<>(indexer));
    }

    private HybridArrayBitPointsToSet(Set<CSObj> set) {
        super(set);
    }

    @Override
    public HybridArrayBitPointsToSet copy() {
        return new HybridArrayBitPointsToSet(
                ((HybridArrayBitSet<CSObj>) set).copy());
    }
}
