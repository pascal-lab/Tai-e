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
import pascal.taie.util.collection.Sets;

import java.util.Set;

public class HybridArrayHashPointsToSet extends DelegatePointsToSet {

    HybridArrayHashPointsToSet() {
        super(Sets.newHybridSet());
    }

    private HybridArrayHashPointsToSet(Set<CSObj> set) {
        super(set);
    }

    @Override
    public PointsToSet copy() {
        return new HybridArrayHashPointsToSet(Sets.newHybridSet(set));
    }
}
