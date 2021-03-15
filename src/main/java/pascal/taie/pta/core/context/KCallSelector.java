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

import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.java.classes.JMethod;
import pascal.taie.pta.core.cs.CSCallSite;
import pascal.taie.pta.core.cs.CSObj;

public class KCallSelector extends KContextSelector<InvokeExp> {

    public KCallSelector(int k, int hk) {
        super(k, hk);
    }

    public KCallSelector(int k) {
        this(k, k - 1);
    }

    @Override
    public Context selectContext(CSCallSite callSite, JMethod callee) {
        return factory.append(
                callSite.getContext(), callSite.getCallSite(), limit);
    }

    @Override
    public Context selectContext(CSCallSite callSite, CSObj recv, JMethod callee) {
        Context parent = callSite.getContext();
        return factory.append(parent, callSite.getCallSite(), limit);
    }
}
