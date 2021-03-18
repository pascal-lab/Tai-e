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

package pascal.taie.analysis.pta.core.cs.element;

import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.pts.PointsToSet;

import java.util.Set;

import static pascal.taie.util.collection.CollectionUtils.newHybridSet;

abstract class AbstractPointer implements Pointer {

    private PointsToSet pointsToSet;
    private final Set<PointerFlowEdge> outEdges = newHybridSet();

    @Override
    public PointsToSet getPointsToSet() {
        return pointsToSet;
    }

    @Override
    public void setPointsToSet(PointsToSet pointsToSet) {
        this.pointsToSet = pointsToSet;
    }

    @Override
    public boolean addOutEdge(PointerFlowEdge edge) {
        return outEdges.add(edge);
    }

    @Override
    public Set<PointerFlowEdge> getOutEdges() {
        return outEdges;
    }
}
