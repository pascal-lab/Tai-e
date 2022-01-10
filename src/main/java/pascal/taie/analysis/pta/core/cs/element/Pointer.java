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
import pascal.taie.language.type.Type;

import java.util.Set;

/**
 * Represents all pointers (nodes) in context-sensitive
 * pointer analysis (pointer flow graph).
 */
public interface Pointer {

    /**
     * @return the points-to set associated with the pointer.
     */
    PointsToSet getPointsToSet();

    /**
     * Sets the associated points-to set of the pointer.
     */
    void setPointsToSet(PointsToSet pointsToSet);

    /**
     * @param edge an out edge of this pointer
     * @return true if new out edge was added to this pointer as a result
     * of the call, otherwise false.
     */
    boolean addOutEdge(PointerFlowEdge edge);

    /**
     * @return out edges of this pointer in pointer flow graph.
     */
    Set<PointerFlowEdge> getOutEdges();

    /**
     * @return out degree of this pointer in pointer flow graph.
     */
    int getOutDegree();

    /**
     * @return the type of this pointer
     */
    Type getType();
}
