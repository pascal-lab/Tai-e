package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

class InitialStrategy implements TransInferStrategy {

    private static final int BASE = InvokeUtils.BASE;

    private static final int RESULT = InvokeUtils.RESULT;
    private final MultiMap<JMethod, CSCallSite> method2CSCallSite = Maps.newMultiMap();
    private Set<JMethod> methodsWithTransfer;
    private Set<JClass> ignoreClasses;
    private Set<JMethod> ignoreMethods;
    private CSManager csManager;
    private Solver solver;

    @Override
    public void setContext(InfererContext context) {
        this.solver = context.solver();
        TaintConfig taintConfig = context.config();
        this.methodsWithTransfer = taintConfig.transfers().stream()
                .map(TaintTransfer::getMethod)
                .collect(Collectors.toSet());
        this.ignoreClasses = Sets.newSet(taintConfig.inferenceConfig().ignoreClasses());
        this.ignoreMethods = Sets.newSet(taintConfig.inferenceConfig().ignoreMethods());
        this.csManager = solver.getCSManager();

        CallGraph<CSCallSite, CSMethod> callGraph = solver.getCallGraph();
        callGraph.reachableMethods()
                .forEach(csMethod ->
                        method2CSCallSite.putAll(csMethod.getMethod(), callGraph.getCallersOf(csMethod)));
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
        Set<CSCallSite> csCallSites = method2CSCallSite.get(method);
        if (!method.isStatic()) {
            // add base-to-result transfer
            TransferPoint fromPoint = new TransferPoint(TransferPoint.Kind.VAR, BASE, null);
            TransferPoint toPoint = new TransferPoint(TransferPoint.Kind.VAR, RESULT, null);
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
        return 1;
    }
}
