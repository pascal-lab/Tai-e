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
import java.util.function.Supplier;

/**
 * Provides static factory methods for {@link PointsToSet}.
 */
public class PointsToSetFactory {

    private static final Supplier<Set<CSObj>> setFactory = Sets::newHybridSet;

    public static PointsToSet make() {
        return new DelegatePointsToSet(setFactory.get());
    }

    /**
     * Convenient method for making one-element points-to set.
     */
    public static PointsToSet make(CSObj obj) {
        PointsToSet set = make();
        set.addObject(obj);
        return set;
    }
}
