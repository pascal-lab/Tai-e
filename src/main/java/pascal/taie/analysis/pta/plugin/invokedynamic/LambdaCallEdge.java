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
import pascal.taie.util.HashUtils;

import java.util.List;

/**
 * Represents call edge on lambda functional object.
 * The edge carries the information about invokedynamic invocation site
 * where the lambda functional object was created.
 */
class LambdaCallEdge extends Edge<CSCallSite, CSMethod> {

    private final InvokeDynamic lambdaIndy;

    private final Context lambdaContext;

    LambdaCallEdge(CSCallSite csCallSite, CSMethod callee,
                   InvokeDynamic lambdaIndy, Context lambdaContext) {
        super(CallKind.OTHER, csCallSite, callee);
        this.lambdaIndy = lambdaIndy;
        this.lambdaContext = lambdaContext;
    }

    List<Var> getCapturedArgs() {
        return lambdaIndy.getArgs();
    }

    Context getLambdaContext() {
        return lambdaContext;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) return false;
        LambdaCallEdge that = (LambdaCallEdge) o;
        return lambdaIndy.equals(that.lambdaIndy) &&
                lambdaContext.equals(that.lambdaContext);
    }

    @Override
    public int hashCode() {
        return HashUtils.hash(super.hashCode(), lambdaIndy, lambdaContext);
    }
}
