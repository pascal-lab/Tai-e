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
import pascal.taie.util.collection.ArraySet;
import pascal.taie.util.collection.HybridIndexableSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

abstract class AbstractPointer implements Pointer {

    private PointsToSet pointsToSet;

    private final int index;

    private final Set<Pointer> successors = new HybridIndexableSet<>(true);

    private final ArrayList<PointerFlowEdge> outEdges = new ArrayList<>(4);

    protected AbstractPointer(int index) {
        this.index = index;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public PointsToSet getPointsToSet() {
        return pointsToSet;
    }

    @Override
    public void setPointsToSet(PointsToSet pointsToSet) {
        this.pointsToSet = pointsToSet;
    }

    @Override
    public Set<CSObj> getObjects() {
        PointsToSet pts = getPointsToSet();
        return pts == null ? Set.of() : pts.getObjects();
    }

    @Override
    public Stream<CSObj> objects() {
        return getObjects().stream();
    }

    @Override
    public boolean addOutEdge(PointerFlowEdge edge) {
        if (successors.add(edge.getTarget())) {
            return outEdges.add(edge);
        }
        return false;
    }

    @Override
    public Set<PointerFlowEdge> getOutEdges() {
        return Collections.unmodifiableSet(new ArraySet<>(outEdges, true));
    }

    @Override
    public int getOutDegree() {
        return outEdges.size();
    }
}
