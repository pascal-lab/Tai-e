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
import pascal.taie.language.types.TypeManager;

public class AllocationSiteBasedModel extends AbstractHeapModel {

    public AllocationSiteBasedModel(TypeManager typeManager) {
        super(typeManager);
    }

    @Override
    protected Obj doGetObj(NewExp newExp) {
        return getNewObj(newExp);
    }
}
