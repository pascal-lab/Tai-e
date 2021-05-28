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

package pascal.taie.analysis.pta.plugin.invokedynamic;

import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.Var;

import java.util.List;

class LambdaCallEdge extends Edge<CSCallSite, CSMethod> {

    private final InvokeDynamic lambdaIndy;

    private final Context lambdaContext;

    LambdaCallEdge(CSCallSite csCallSite, CSMethod callee,
                   InvokeDynamic lambdaIndy, Context lambdaContext) {
        super(CallKind.OTHER, csCallSite, callee);
        this.lambdaIndy = lambdaIndy;
        this.lambdaContext = lambdaContext;
    }

    List<Var> getLambdaArgs() {
        return lambdaIndy.getArgs();
    }

    Context getLambdaContext() {
        return lambdaContext;
    }

    // TODO: override hashCode() and equals()?
}
