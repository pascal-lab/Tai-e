package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.taint.TaintConfig;
import pascal.taie.analysis.pta.plugin.taint.TaintTransfer;
import pascal.taie.analysis.pta.plugin.taint.TransferPoint;
import pascal.taie.analysis.pta.plugin.taint.inferer.InfererContext;
import pascal.taie.analysis.pta.plugin.taint.inferer.InferredTransfer;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.plugin.util.StrategyUtils;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassMember;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.TwoKeyMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class InitialStrategy implements TransInferStrategy {

    private static final int BASE = InvokeUtils.BASE;
    private static final int RESULT = InvokeUtils.RESULT;
    public static String ID = "initialize";
    private final TwoKeyMap<JMethod, Integer, Set<Type>> arg2types = Maps.newTwoKeyMap();
    private MultiMap<JMethod, CSCallSite> method2CSCallSite;
    private Solver solver;
    private CSManager csManager;
    private Set<JMethod> methodsWithTransfer;
    private Set<JClass> ignoreClasses;
    private Set<JMethod> ignoreMethods;

    private Set<JMethod> targetMethods;

    private long inferredTransCnt = 0;

    @Override
    public void setContext(InfererContext context) {
        solver = context.solver();
        TaintConfig taintConfig = context.config();
        csManager = solver.getCSManager();
        methodsWithTransfer = taintConfig.transfers().stream()
                .map(TaintTransfer::getMethod)
                .collect(Collectors.toSet());
        ignoreClasses = Sets.newSet(taintConfig.inferenceConfig().ignoreClasses());
        ignoreMethods = Sets.newSet(taintConfig.inferenceConfig().ignoreMethods());
        method2CSCallSite = StrategyUtils.getMethod2CSCallSites(solver.getCallGraph());

        for (JMethod method : method2CSCallSite.keySet()) {
            for (int i = 0; i < method.getParamCount(); i++) {
                arg2types.put(method, i, getArgType(method, i));
            }
            if (!method.isStatic()) {
                arg2types.put(method, BASE, getArgType(method, BASE));
            }
            arg2types.put(method, RESULT, getArgType(method, RESULT));
        }

        PointerAnalysisResult ptaResult = solver.getResult();
        CallGraph<Invoke, JMethod> callGraph = ptaResult.getCallGraph();
        Set<JMethod> appMethods = callGraph.reachableMethods()
                .filter(ClassMember::isApplication)
                .collect(Collectors.toSet());
        Set<JMethod> firstNonAppMethods = appMethods.stream()
                .map(callGraph::getCalleesOfM)
                .flatMap(Collection::stream)
                .filter(Predicate.not(ClassMember::isApplication))
                .collect(Collectors.toSet());
        targetMethods = Sets.newSet(appMethods);
        targetMethods.addAll(firstNonAppMethods);
    }

    private Set<Type> getArgType(JMethod method, int index) {
        return method2CSCallSite.get(method).stream()
                .map(csCallSite -> {
                    CSVar csVar = StrategyUtils.getCSVar(csManager, csCallSite, index);
                    return StrategyUtils.getTypes(solver, csVar);
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private void addTransfers(Set<InferredTransfer> result, JMethod method, int from, int to) {
        if (StrategyUtils.getParamType(method, from) instanceof ReferenceType
                && StrategyUtils.getParamType(method, to) instanceof ReferenceType) {
            Set<Type> toTypes = arg2types.getOrDefault(method, to, Set.of());
            TransferPoint fromPoint = new TransferPoint(TransferPoint.Kind.VAR, from, null);
            TransferPoint toPoint = new TransferPoint(TransferPoint.Kind.VAR, to, null);
            toTypes.stream()
                    .map(toType -> new InferredTransfer(method, fromPoint, toPoint, toType, getWeight()))
                    .forEach(result::add);
        }
    }

    @Override
    public boolean shouldIgnore(JMethod method, int index) {
        return ignoreMethods.contains(method)
                || ignoreClasses.contains(method.getDeclaringClass())
                || methodsWithTransfer.contains(method)
                || !targetMethods.contains(method);
    }

    @Override
    public Set<InferredTransfer> apply(JMethod method, int index, Set<InferredTransfer> transfers) {
        Set<InferredTransfer> result = Sets.newSet();
        if(index == BASE) {
            assert !method.isStatic();
            // base-to-result
            addTransfers(result, method, index, RESULT);
        } else {
            if (!method.isStatic()) {
                // arg-to-base
                addTransfers(result, method, index, BASE);
            }
            // arg-to-result
            addTransfers(result, method, index, RESULT);
        }
        inferredTransCnt += result.size();
        return Collections.unmodifiableSet(result);
    }

    public long getInferredTransCnt() {
        return inferredTransCnt;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    private int getWeight() {
        return 1;
    }
}
