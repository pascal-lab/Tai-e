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

package pascal.taie.pta.core.context;

import pascal.taie.pta.core.cs.CSCallSite;
import pascal.taie.pta.core.cs.CSMethod;
import pascal.taie.pta.core.cs.CSObj;
import pascal.taie.java.classes.JMethod;
import pascal.taie.pta.ir.Obj;

public interface ContextSelector {

    default Context getDefaultContext() {
        return DefaultContext.INSTANCE;
    }

    /**
     * Selects contexts for static methods.
     */
    Context selectContext(CSCallSite callSite, JMethod callee);

    /**
     * Selects contexts for instance methods.
     */
    Context selectContext(CSCallSite callSite, CSObj recv, JMethod callee);

    /**
     * Selects heap contexts for new-created abstract objects.
     */
    Context selectHeapContext(CSMethod method, Obj obj);
}
