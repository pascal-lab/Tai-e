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
import pascal.taie.util.collection.EnhancedSet;
import pascal.taie.util.collection.HybridHashSet;

public class HybridHashPointsToSet extends DelegatePointsToSet {

    HybridHashPointsToSet() {
        this(new HybridHashSet<>());
    }

    private HybridHashPointsToSet(EnhancedSet<CSObj> set) {
        super(set);
    }

    @Override
    protected PointsToSet newSet(EnhancedSet<CSObj> set) {
        return new HybridHashPointsToSet(set);
    }
}
