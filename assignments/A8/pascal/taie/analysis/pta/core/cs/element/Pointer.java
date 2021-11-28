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

import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.language.type.Type;

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
     * @return the type of this pointer
     */
    Type getType();
}
