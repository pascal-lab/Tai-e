/*
 * Tai'e - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai'e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.pta.core.cs;

import pascal.taie.pta.core.context.Context;
import pascal.taie.pta.element.Method;
import pascal.taie.util.HybridArrayHashSet;

import java.util.Set;

public class CSMethod extends AbstractCSElement {

    private final Method method;
    /**
     * Callers of this CS method.
     */
    private final Set<CSCallSite> callers = new HybridArrayHashSet<>();

    CSMethod(Method method, Context context) {
        super(context);
        this.method = method;
    }

    public Method getMethod() {
        return method;
    }

    public void addCaller(CSCallSite caller) {
        callers.add(caller);
    }

    public Set<CSCallSite> getCallers() {
        return callers;
    }

    @Override
    public String toString() {
        return context + ":" + method;
    }
}
