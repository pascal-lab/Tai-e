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

package pascal.taie.analysis.oldpta.core.context;

import pascal.taie.language.classes.JMethod;
import pascal.taie.analysis.oldpta.core.cs.CSCallSite;
import pascal.taie.analysis.oldpta.core.cs.CSMethod;
import pascal.taie.analysis.oldpta.core.cs.CSObj;
import pascal.taie.analysis.oldpta.ir.Obj;

/**
 * 1-type-sensitivity with no heap context.
 */
public class OneTypeSelector extends AbstractContextSelector {

    @Override
    public Context selectContext(CSCallSite callSite, JMethod callee) {
        return callSite.getContext();
    }

    @Override
    public Context selectContext(CSCallSite callSite, CSObj recv, JMethod callee) {
        return new OneContext<>(recv.getObject().getContainerType());
    }

    @Override
    protected Context doSelectHeapContext(CSMethod method, Obj obj) {
        return getDefaultContext();
    }
}
