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

package pascal.taie.analysis.oldpta.core.cs;

import pascal.taie.analysis.oldpta.core.context.Context;
import pascal.taie.language.classes.JMethod;

import java.util.Set;

import static pascal.taie.util.collection.SetUtils.newHybridSet;

public class CSMethod extends AbstractCSElement {

    private final JMethod method;
    /**
     * Callers of this CS method.
     */
    private final Set<CSCallSite> callers = newHybridSet();

    CSMethod(JMethod method, Context context) {
        super(context);
        this.method = method;
    }

    public JMethod getMethod() {
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
