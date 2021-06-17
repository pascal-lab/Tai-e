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

package pascal.taie.analysis.pta.plugin.reflection;

import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.ir.exp.Var;

import javax.annotation.Nullable;

/**
 * Represents reflective call edges.
 */
class ReflectiveCallEdge extends Edge<CSCallSite, CSMethod> {

    /**
     * Variable pointing to the array argument of reflective call,
     * which contains the arguments for the reflective target method.
     */
    @Nullable
    private final Var args;

    ReflectiveCallEdge(CSCallSite csCallSite, CSMethod callee, @Nullable Var arg) {
        super(CallKind.OTHER, csCallSite, callee);
        this.args = arg;
    }

    @Nullable
    Var getArgs() {
        return args;
    }
}
