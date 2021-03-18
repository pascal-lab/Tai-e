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

import pascal.taie.language.classes.JMethod;
import pascal.taie.pta.core.cs.CSCallSite;
import pascal.taie.pta.core.cs.CSObj;
import pascal.taie.pta.core.heap.Obj;

public class KObjSelector extends KContextSelector<Obj> {

    public KObjSelector(int k, int hk) {
        super(k, hk);
    }

    public KObjSelector(int k) {
        this(k, k - 1);
    }

    @Override
    public Context selectContext(CSCallSite callSite, JMethod callee) {
        return callSite.getContext();
    }

    @Override
    public Context selectContext(CSCallSite callSite, CSObj recv, JMethod callee) {
        return factory.append(recv.getContext(), recv.getObject(), limit);
    }
}
