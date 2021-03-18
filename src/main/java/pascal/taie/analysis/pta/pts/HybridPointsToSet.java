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

import static pascal.taie.util.collection.CollectionUtils.newHybridSet;

public class HybridPointsToSet extends DelegatePointsToSet {

    @Override
    protected void initializePointsToSet() {
        set = newHybridSet();
    }

    public static class Factory extends PointsToSetFactory {

        @Override
        protected PointsToSet makePointsToSet() {
            return new HybridPointsToSet();
        }
    }
}
