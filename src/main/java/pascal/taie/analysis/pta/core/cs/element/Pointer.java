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
import pascal.taie.util.Indexable;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Represents all pointers (nodes) in context-sensitive
 * pointer analysis (pointer flow graph).
 */
public interface Pointer extends Indexable {

    /**
     * @return the points-to set associated with the pointer.
     */
    @Nullable
    PointsToSet getPointsToSet();

    /**
     * Sets the associated points-to set of the pointer.
     */
    void setPointsToSet(PointsToSet pointsToSet);

    /**
     * Safely retrieves context-sensitive objects pointed to by this pointer.
     *
     * @return an empty set if {@code pointer} has not been associated
     * a {@code PointsToSet}; otherwise, returns set of objects in the
     * {@code PointsToSet}.
     */
    Set<CSObj> getObjects();

    /**
     * Safely retrieves context-sensitive objects pointed to by this pointer.
     *
     * @return an empty stream if {@code pointer} has not been associated
     * a {@code PointsToSet}; otherwise, returns stream of objects in the
     * {@code PointsToSet}.
     */
    Stream<CSObj> objects();

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
