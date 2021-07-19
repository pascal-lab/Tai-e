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

package pascal.taie.analysis.pta.plugin.util;

import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;

/**
 * Dummy model which does nothing.
 */
public enum DummyModel implements Model {

    INSTANCE;

    public static Model get() {
        return INSTANCE;
    }

    @Override
    public void handleNewInvoke(Invoke invoke) {
    }

    @Override
    public boolean isRelevantVar(Var var) {
        return false;
    }

    @Override
    public void handleNewPointsToSet(CSVar csVar, PointsToSet pts) {
    }
}
