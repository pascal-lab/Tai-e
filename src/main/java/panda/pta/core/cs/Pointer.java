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

package panda.pta.core.cs;

import panda.pta.element.Type;
import panda.pta.set.PointsToSet;

public interface Pointer {

    PointsToSet getPointsToSet();

    void setPointsToSet(PointsToSet pointsToSet);

    /**
     * @return the type of this pointer
     */
    Type getType();
}
