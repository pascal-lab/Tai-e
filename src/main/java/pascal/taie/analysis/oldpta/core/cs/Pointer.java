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

package pascal.taie.analysis.oldpta.core.cs;

import pascal.taie.analysis.oldpta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.oldpta.set.PointsToSet;
import pascal.taie.language.types.Type;

import java.util.Set;

/**
 * Represent pointers/nodes in pointer analysis/pointer flow graph.
 */
public interface Pointer {

    PointsToSet getPointsToSet();

    void setPointsToSet(PointsToSet pointsToSet);

    /**
     * @param edge an out edge of this pointer
     * @return if the given edge is an new edge for this pointer.
     */
    boolean addOutEdge(PointerFlowEdge edge);

    /**
     * @return out edges of this pointer in pointer flow graph.
     */
    Set<PointerFlowEdge> getOutEdges();

    /**
     * @return the type of this pointer
     */
    Type getType();
}
