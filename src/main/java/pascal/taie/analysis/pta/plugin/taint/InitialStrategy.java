package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.plugin.util.StrategyUtils;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.MultiMapCollector;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.TwoKeyMap;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

class InitialStrategy implements TransInferStrategy {

    private static final int BASE = InvokeUtils.BASE;

    private static final int RESULT = InvokeUtils.RESULT;
    private final TwoKeyMap<JMethod, Integer, Set<Type>> arg2types = Maps.newTwoKeyMap();
    private Set<JMethod> methodsWithTransfer;
    private Set<JClass> ignoreClasses;
    private Set<JMethod> ignoreMethods;

    @Override
    public void setContext(InfererContext context) {
        Solver solver = context.solver();
        TaintConfig taintConfig = context.config();
        CSManager csManager = solver.getCSManager();
        this.methodsWithTransfer = taintConfig.transfers().stream()
                .map(TaintTransfer::getMethod)
                .collect(Collectors.toSet());
        this.ignoreClasses = Sets.newSet(taintConfig.inferenceConfig().ignoreClasses());
        this.ignoreMethods = Sets.newSet(taintConfig.inferenceConfig().ignoreMethods());

        CallGraph<CSCallSite, CSMethod> callGraph = solver.getCallGraph();
        MultiMap<JMethod, CSCallSite> method2CSCallSite = Maps.newMultiMap();
        callGraph.reachableMethods()
                .forEach(csMethod ->
                        method2CSCallSite.putAll(csMethod.getMethod(), callGraph.getCallersOf(csMethod)));

        for(JMethod method : method2CSCallSite.keySet()) {
            // TODO: fix undefined behavior
            for(int i = RESULT; i < method.getParamCount(); i++) {
                int finalI = i;
                Set<Type> types= method2CSCallSite.get(method).stream()
                        .map(csCallSite -> {
                            CSVar csVar = StrategyUtils.getCSVar(csManager, csCallSite, finalI);
                            return StrategyUtils.getTypes(solver, csVar);
                        })
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet());
                arg2types.put(method, i, types);
            }
        }
    }

    @Override
    public Set<TaintTransfer> apply(JMethod method, Set<TaintTransfer> transfers) {
        if (ignoreMethods.contains(method)
                || ignoreClasses.contains(method.getDeclaringClass())
                || methodsWithTransfer.contains(method))
            return Set.of();

        // add whole transfers for this method
        Set<TaintTransfer> result = Sets.newSet();
        if (!method.isStatic()) {
            // base-to-result
            addTransfers(result, method, BASE, RESULT);
            // arg-to-base
            for (int i = 0; i < method.getParamCount(); i++) {
                addTransfers(result, method, i, BASE);
            }
        }
        // arg-to-result
        for (int i = 0; i < method.getParamCount(); i++) {
            addTransfers(result, method, i, RESULT);
        }

        return Collections.unmodifiableSet(result);
    }

    private void addTransfers(Set<TaintTransfer> result, JMethod method, int from, int to) {
        Set<Type> toTypes = arg2types.getOrDefault(method, to, Set.of());
        TransferPoint fromPoint = new TransferPoint(TransferPoint.Kind.VAR, from, null);
        TransferPoint toPoint = new TransferPoint(TransferPoint.Kind.VAR, to, null);
        toTypes.stream()
                .map(toType -> new InferredTransfer(method, fromPoint, toPoint, toType, getWeight()))
                .forEach(result::add);
    }

    @Override
    public int getPriority() {
        return 0;
    }

    private int getWeight() {
        return 1;
    }
}
