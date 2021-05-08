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
import pascal.taie.ir.exp.Var;

import java.util.List;

class LambdaCallEdge extends Edge<CSCallSite, CSMethod> {

    private Var invokeResult;

    private List<Var> capturedValues;

    private Context lambdaContext;

    public LambdaCallEdge(CSCallSite csCallSite, CSMethod callee) {
        super(CallKind.OTHER, csCallSite, callee);
    }

    public void setLambdaParams(Var invokeResult, List<Var> capturedValues, Context lambdaContext) {
        this.invokeResult = invokeResult;
        this.capturedValues = capturedValues;
        this.lambdaContext = lambdaContext;
    }

    public Var getInvokeResult() {
        return invokeResult;
    }

    public void setInvokeResult(Var invokeResult) {
        this.invokeResult = invokeResult;
    }

    public List<Var> getCapturedValues() {
        return capturedValues;
    }

    public void setCapturedValues(List<Var> capturedValues) {
        this.capturedValues = capturedValues;
    }

    public Context getLambdaContext() {
        return lambdaContext;
    }

    public void setLambdaContext(Context lambdaContext) {
        this.lambdaContext = lambdaContext;
    }
}
