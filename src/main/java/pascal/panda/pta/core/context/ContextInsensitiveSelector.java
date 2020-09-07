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

package pascal.panda.pta.core.context;

import pascal.panda.pta.core.cs.CSCallSite;
import pascal.panda.pta.core.cs.CSMethod;
import pascal.panda.pta.core.cs.CSObj;
import pascal.panda.pta.element.Method;
import pascal.panda.pta.element.Obj;

public class ContextInsensitiveSelector implements ContextSelector {

    @Override
    public Context selectContext(CSCallSite callSite, Method callee) {
        return DefaultContext.INSTANCE;
    }

    @Override
    public Context selectContext(CSCallSite callSite, CSObj recv, Method callee) {
        return DefaultContext.INSTANCE;
    }

    @Override
    public Context selectHeapContext(CSMethod method, Obj allocation) {
        return DefaultContext.INSTANCE;
    }
}
