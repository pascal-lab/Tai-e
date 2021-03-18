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

package pascal.taie.analysis.oldpta.set;

import pascal.taie.analysis.oldpta.core.cs.CSObj;

public abstract class PointsToSetFactory {

    private static PointsToSetFactory factory;

    public static void setFactory(PointsToSetFactory factory) {
        PointsToSetFactory.factory = factory;
    }

    public static PointsToSet make() {
        return factory.makePointsToSet();
    }

    /**
     * Convenient method for making one-element points-to set.
     */
    public static PointsToSet make(CSObj obj) {
        PointsToSet set = make();
        set.addObject(obj);
        return set;
    }

    protected abstract PointsToSet makePointsToSet();
}
