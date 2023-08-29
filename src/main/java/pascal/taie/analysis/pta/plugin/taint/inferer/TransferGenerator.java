package pascal.taie.analysis.pta.plugin.taint.inferer;

import pascal.taie.analysis.graph.callgraph.CallGraphs;
import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.taint.TransferPoint;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.plugin.util.StrategyUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Sets;

import javax.annotation.Nullable;
import java.util.Set;
import java.util.stream.Collectors;

public class TransferGenerator {

    private static final int DEFAULT_WEIGHT = 1;

    private final Solver solver;

    private final CSManager csManager;

    public TransferGenerator(Solver solver) {
        this.solver = solver;
        this.csManager = solver.getCSManager();
    }

    public Set<InferredTransfer> getTransfers(CSCallSite csCallSite, int from, int to) {
        CSVar fromVar = getCSVar(csCallSite, from);
        CSVar toVar = getCSVar(csCallSite, to);
        Set<InferredTransfer> result = Sets.newSet();
        if (fromVar != null && fromVar.getType() instanceof ReferenceType
                && toVar != null && toVar.getType() instanceof ReferenceType) {
            TransferPoint fromPoint = new TransferPoint(TransferPoint.Kind.VAR, from, null);
            TransferPoint toPoint = new TransferPoint(TransferPoint.Kind.VAR, to, null);

            switch (to) {
                case InvokeUtils.RESULT -> {
                    // collect types in callee method to avoid
                    // r = o.m(..)
                    // r = p
                    for (CSMethod csMethod : getCalleesOf(csCallSite)) {
                        JMethod callee = csMethod.getMethod();
                        for (Type type : getReturnType(csMethod)) {
                            result.add(new InferredTransfer(callee, fromPoint, toPoint, type, DEFAULT_WEIGHT));
                        }
                    }
                }
                case InvokeUtils.BASE -> {
                    Invoke callSite = csCallSite.getCallSite();
                    for(CSObj base : toVar.getObjects()) {
                        Type baseType = base.getObject().getType();
                        JMethod callee = CallGraphs.resolveCallee(baseType, callSite);
                        if(callee != null) {
                            result.add(new InferredTransfer(callee, fromPoint, toPoint, baseType, DEFAULT_WEIGHT));
                        }
                    }
                }
                default -> {
                    Set<Type> toTypes = getArgType(csCallSite, to);
                    for (CSMethod csMethod : getCalleesOf(csCallSite)) {
                        JMethod callee = csMethod.getMethod();
                        for (Type toType : toTypes) {
                            result.add(new InferredTransfer(callee, fromPoint, toPoint, toType, DEFAULT_WEIGHT));
                        }
                    }
                }
            }
        }
        return result;
    }

    @Nullable
    private CSVar getCSVar(CSCallSite csCallSite, int index) {
        return StrategyUtils.getCSVar(csManager, csCallSite, index);
    }

    private Set<Type> getArgType(CSCallSite csCallSite, int index) {
        return StrategyUtils.getArgType(solver, csCallSite, index);
    }

    private Set<Type> getReturnType(CSMethod csMethod) {
        Context context = csMethod.getContext();
        JMethod method = csMethod.getMethod();
        return method.getIR().getReturnVars()
                .stream()
                .map(var -> csManager.getCSVar(context, var))
                .map(solver::getPointsToSetOf)
                .flatMap(PointsToSet::objects)
                .map(CSObj::getObject)
                .map(Obj::getType)
                .collect(Collectors.toSet());
    }

    // Edge with CallKind.OTHER will be filtered
    private Set<CSMethod> getCalleesOf(CSCallSite csCallSite) {
        return solver.getCallGraph().edgesOutOf(csCallSite)
                .filter(edge -> edge.getKind() != CallKind.OTHER)
                .map(Edge::getCallee)
                .collect(Collectors.toSet());
    }
}
