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

import pascal.taie.ir.exp.ReferenceLiteral;
import pascal.taie.ir.stmt.New;

/**
 * Represents of heap models for heap objects.
 */
public interface HeapModel {

    /**
     * @return the abstract object for given new statement.
     */
    Obj getObj(New allocSite);

    /**
     * @return the constant object for given value.
     */
    Obj getConstantObj(ReferenceLiteral value);
}
