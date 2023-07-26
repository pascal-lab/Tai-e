package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.*;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Sets;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

class InitialStrategy implements TransInferStrategy {

    private static final int BASE = InvokeUtils.BASE;

    private static final int RESULT = InvokeUtils.RESULT;

    private Set<JMethod> methodsWithTransfer;

    private Set<JClass> ignoreClasses;

    private Set<JMethod> ignoreMethods;

    private CallGraph<CSCallSite, CSMethod> callGraph;

    private CSManager csManager;

    private Solver solver;


    @Override
    public void setContext(InfererContext context) {
        this.methodsWithTransfer = context.config().transfers().stream()
                .map(TaintTransfer::getMethod)
                .collect(Collectors.toSet());
        this.ignoreClasses = Sets.newSet(context.config().inferenceConfig().ignoreClasses());
        this.ignoreMethods = Sets.newSet(context.config().inferenceConfig().ignoreMethods());
        this.callGraph = context.solver().getCallGraph();
        this.csManager = context.solver().getCSManager();
        this.solver = context.solver();
    }

    @Override
    public Set<TaintTransfer> apply(JMethod method, Set<TaintTransfer> transfers) {
        // check whether this method or its class needs to be ignored
        if (ignoreMethods.contains(method) || ignoreClasses.contains(method.getDeclaringClass()))
            return Set.of();
        // check if exists transfer for this method
        if (methodsWithTransfer.contains(method))
            return Set.of();

        // add whole transfers for this method
        Set<TaintTransfer> taintTransferSet = Sets.newSet();
        AtomicReference<TransferPoint> fromPoint = new AtomicReference<>();
        AtomicReference<TransferPoint> toPoint = new AtomicReference<>();
        Set<CSCallSite> csCallSites = getCSCallSitesOf(method);
        if (!method.isStatic()) {
            // add base-to-result transfer
            fromPoint.set(new TransferPoint(TransferPoint.Kind.VAR, BASE, null));
            toPoint.set(new TransferPoint(TransferPoint.Kind.VAR, RESULT, null));
            addInferredTransfers(method, taintTransferSet, fromPoint, toPoint, csCallSites, RESULT);
            // add arg-to-base transfer(s)
            addInferredTransfersFromArgs(method, taintTransferSet, fromPoint, toPoint, csCallSites, BASE);
        }
        // add arg-to-result transfer(s)
        addInferredTransfersFromArgs(method, taintTransferSet, fromPoint, toPoint, csCallSites, RESULT);

        return Collections.unmodifiableSet(taintTransferSet);
    }

    private void addInferredTransfersFromArgs(JMethod method, Set<TaintTransfer> taintTransferSet,
                                              AtomicReference<TransferPoint> fromPoint,
                                              AtomicReference<TransferPoint> toPoint,
                                              Set<CSCallSite> csCallSites, int target) {
        for (int i = 0; i < method.getParamCount(); i++) {
            fromPoint.set(new TransferPoint(TransferPoint.Kind.VAR, i, null));
            toPoint.set(new TransferPoint(TransferPoint.Kind.VAR, target, null));
            addInferredTransfers(method, taintTransferSet, fromPoint, toPoint, csCallSites, target);
        }
    }

    private void addInferredTransfers(JMethod method, Set<TaintTransfer> taintTransferSet,
                                      AtomicReference<TransferPoint> fromPoint,
                                      AtomicReference<TransferPoint> toPoint,
                                      Set<CSCallSite> csCallSites, int target) {
        csCallSites.stream().map(csCallSite -> getCSVar(csCallSite, target))
                .map(solver::getPointsToSetOf)
                .map(PointsToSet::getObjects)
                .flatMap(Collection::stream)
                .map(CSObj::getObject)
                .map(Obj::getType)
                .forEach(type -> taintTransferSet
                        .add(new InferredTransfer(method, fromPoint.get(), toPoint.get(), type, getWeight())));
    }

    @Override
    public int getPriority() {
        return 0;
    }

    private int getWeight() {
        return 0;
    }

    private Set<CSCallSite> getCSCallSitesOf(JMethod method) {
        return callGraph.edges()
                .filter(edge -> edge.getCallee().getMethod().equals(method))
                .map(Edge::getCallSite)
                .collect(Collectors.toSet());
    }

    @Nullable
    private CSVar getCSVar(CSCallSite csCallSite, int index) {
        Context context = csCallSite.getContext();
        Var var = InvokeUtils.getVar(csCallSite.getCallSite(), index);
        if (var == null) {
            return null;
        }
        return csManager.getCSVar(context, var);
    }
}
