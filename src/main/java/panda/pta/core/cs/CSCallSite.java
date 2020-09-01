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

package panda.pta.core.cs;

import panda.pta.core.context.Context;
import panda.pta.element.CallSite;

public class CSCallSite extends AbstractCSElement {

    private final CallSite callSite;

    CSCallSite(CallSite callSite, Context context) {
        super(context);
        this.callSite = callSite;
    }

    public CallSite getCallSite() {
        return callSite;
    }

    @Override
    public String toString() {
        return context + ":" + callSite;
    }
}
