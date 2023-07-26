package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.plugin.util.StrategyUtils;
import pascal.taie.language.classes.ClassHierarchy;
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

class InitialStrategy implements TransInferStrategy {

    private static final int BASE = InvokeUtils.BASE;

    private static final int RESULT = InvokeUtils.RESULT;

    private static final Set<String> COLLECTION_CLASS = Set.of(
            "java.util.Collection", "java.util.Map", "java.util.Dictionary");
    private final MultiMap<JMethod, CSCallSite> method2CSCallSite = Maps.newMultiMap();
    private final TwoKeyMap<JMethod, Integer, Set<Type>> arg2types = Maps.newTwoKeyMap();
    private Solver solver;
    private CSManager csManager;
    private ClassHierarchy hierarchy;
    private Set<JMethod> methodsWithTransfer;
    private Set<JClass> ignoreClasses;
    private Set<JMethod> ignoreMethods;

    @Override
    public void setContext(InfererContext context) {
        solver = context.solver();
        hierarchy = solver.getHierarchy();
        TaintConfig taintConfig = context.config();
        csManager = solver.getCSManager();
        methodsWithTransfer = taintConfig.transfers().stream()
                .map(TaintTransfer::getMethod)
                .collect(Collectors.toSet());
        ignoreClasses = getCollectionClasses();
        ignoreClasses.addAll(taintConfig.inferenceConfig().ignoreClasses());
        ignoreMethods = Sets.newSet(taintConfig.inferenceConfig().ignoreMethods());

        CallGraph<CSCallSite, CSMethod> callGraph = solver.getCallGraph();
        callGraph.reachableMethods()
                .forEach(csMethod ->
                        method2CSCallSite.putAll(csMethod.getMethod(), callGraph.getCallersOf(csMethod)));

        for (JMethod method : method2CSCallSite.keySet()) {
            for (int i = 0; i < method.getParamCount(); i++) {
                arg2types.put(method, i, getArgType(method, i));
            }
            if (!method.isStatic()) {
                arg2types.put(method, BASE, getArgType(method, BASE));
            }
            arg2types.put(method, RESULT, getArgType(method, RESULT));
        }
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

    private void addTransfers(Set<TaintTransfer> result, JMethod method, int from, int to) {
        if (getParamType(method, from) instanceof ReferenceType
                && getParamType(method, to) instanceof ReferenceType) {
            Set<Type> toTypes = arg2types.getOrDefault(method, to, Set.of());
            TransferPoint fromPoint = new TransferPoint(TransferPoint.Kind.VAR, from, null);
            TransferPoint toPoint = new TransferPoint(TransferPoint.Kind.VAR, to, null);
            toTypes.stream()
                    .map(toType -> new InferredTransfer(method, fromPoint, toPoint, toType, getWeight()))
                    .forEach(result::add);
        }
    }

    private Type getParamType(JMethod method, int index) {
        return switch (index) {
            case RESULT -> method.getReturnType();
            case BASE -> method.getDeclaringClass().getType();
            default -> method.getParamType(index);
        };
    }

    private Set<JClass> getCollectionClasses() {
        Set<JClass> collectionClasses = Sets.newSet();
        COLLECTION_CLASS.stream()
                .map(hierarchy::getJREClass)
                .map(hierarchy::getAllSubclassesOf)
                .flatMap(Collection::stream)
                .filter(Predicate.not(JClass::isApplication))
                .forEach(collectionClasses::add);
        Set<JClass> allCollectionClasses = Sets.newSet(collectionClasses);
        collectionClasses.forEach(c ->
                allCollectionClasses.addAll(getAllInnerClassesOf(c)));
        return allCollectionClasses;
    }

    private Set<JClass> getAllInnerClassesOf(JClass jclass) {
        Set<JClass> innerClasses = Sets.newHybridSet();
        hierarchy.getDirectInnerClassesOf(jclass).forEach(inner -> {
            innerClasses.add(inner);
            innerClasses.addAll(getAllInnerClassesOf(inner));
        });
        return innerClasses;
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

    @Override
    public int getPriority() {
        return 0;
    }

    private int getWeight() {
        return 1;
    }
}
