/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.oldpta.core.context;

import pascal.taie.language.classes.JMethod;
import pascal.taie.oldpta.core.cs.CSCallSite;
import pascal.taie.oldpta.core.cs.CSMethod;
import pascal.taie.oldpta.core.cs.CSObj;
import pascal.taie.oldpta.ir.Obj;

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
