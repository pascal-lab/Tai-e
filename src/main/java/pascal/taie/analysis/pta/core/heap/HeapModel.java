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

package pascal.taie.analysis.pta.core.heap;

import pascal.taie.ir.exp.NewExp;
import pascal.taie.ir.exp.ReferenceLiteral;

/**
 * Model for heap objects.
 */
public interface HeapModel {

    /**
     * @return the abstract object for given new statement.
     */
    Obj getObj(NewExp newExp);

    /**
     * @return the constant object for given value.
     */
    Obj getConstantObj(ReferenceLiteral value);

    /**
     * @return the abstract object for given mock object.
     */
    Obj getMockObj(MockObj obj);
}
