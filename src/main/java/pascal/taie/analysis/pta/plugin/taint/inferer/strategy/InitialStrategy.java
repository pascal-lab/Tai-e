package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

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
import java.util.stream.Collectors;

public class InitialStrategy implements TransInferStrategy {

    public static String ID = "initialize";

    private static final int BASE = InvokeUtils.BASE;

    private static final int RESULT = InvokeUtils.RESULT;

    private MultiMap<JMethod, CSCallSite> method2CSCallSite;
    private final TwoKeyMap<JMethod, Integer, Set<Type>> arg2types = Maps.newTwoKeyMap();
    private Solver solver;
    private CSManager csManager;
    private Set<JMethod> methodsWithTransfer;
    private Set<JClass> ignoreClasses;
    private Set<JMethod> ignoreMethods;

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
    public Set<InferredTransfer> apply(JMethod method, Set<InferredTransfer> transfers) {
        if (ignoreMethods.contains(method)
                || ignoreClasses.contains(method.getDeclaringClass())
                || methodsWithTransfer.contains(method))
            return Set.of();

        // add whole transfers for this method
        Set<InferredTransfer> result = Sets.newSet();
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
