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

package panda.pta.core.context;

import panda.pta.core.cs.CSCallSite;
import panda.pta.core.cs.CSMethod;
import panda.pta.core.cs.CSObj;
import panda.pta.element.Method;
import panda.pta.element.Obj;

public interface ContextSelector {

    default Context getDefaultContext() {
        return DefaultContext.INSTANCE;
    }

    /**
     * Selects contexts for static methods.
     */
    Context selectContext(CSCallSite callSite, Method callee);

    /**
     * Selects contexts for instance methods.
     */
    Context selectContext(CSCallSite callSite, CSObj recv, Method callee);

    /**
     * Selects heap contexts for new-created abstract objects.
     */
    Context selectHeapContext(CSMethod method, Obj obj);
}
