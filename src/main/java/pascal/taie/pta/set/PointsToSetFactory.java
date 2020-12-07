/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.pta.set;

import pascal.taie.pta.core.cs.CSObj;

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
