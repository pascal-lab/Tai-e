package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.taint.Sink;
import pascal.taie.analysis.pta.plugin.taint.TaintConfig;
import pascal.taie.analysis.pta.plugin.taint.TaintTransfer;
import pascal.taie.analysis.pta.plugin.taint.inferer.InfererContext;
import pascal.taie.analysis.pta.plugin.taint.inferer.InferredTransfer;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Sets;

import java.util.Set;
import java.util.stream.Collectors;

public class ScopeFilter implements TransInferStrategy {
    private Set<String> appPackages;
    private Set<JMethod> methodsWithTransfer;
    private Set<JClass> ignoreClasses;
    private Set<JMethod> ignoreMethods;
    private Set<Type> ignoreTypes;
    private Set<JMethod> targetMethods;

    @Override
    public void setContext(InfererContext context) {
        Solver solver = context.solver();
        TaintConfig taintConfig = context.config();
        methodsWithTransfer = taintConfig.transfers().stream()
                .map(TaintTransfer::getMethod)
                .collect(Collectors.toSet());
        appPackages = Sets.newSet(taintConfig.inferenceConfig().appPackages());
        ignoreClasses = Sets.newSet(taintConfig.inferenceConfig().ignoreClasses());
        ignoreMethods = Sets.newSet(taintConfig.inferenceConfig().ignoreMethods());
        // Ignore sink method by default
        ignoreMethods.addAll(taintConfig.sinks().stream().map(Sink::method).toList());
        ignoreTypes = Sets.newSet(taintConfig.inferenceConfig().ignoreTypes());

        PointerAnalysisResult ptaResult = solver.getResult();
        CallGraph<Invoke, JMethod> callGraph = ptaResult.getCallGraph();

        switch (taintConfig.inferenceConfig().scope()) {
            case APP -> {
                targetMethods = callGraph.reachableMethods()
                        .filter(this::isApp)
                        .collect(Collectors.toUnmodifiableSet());
            }
            case APP_LIB -> {
                targetMethods = callGraph.reachableMethods()
                        .filter(this::isAppOrLib)
                        .collect(Collectors.toSet());
            }
            case ALL -> targetMethods = callGraph.reachableMethods()
                    .collect(Collectors.toUnmodifiableSet());
        }
    }

    private boolean isAppOrLib(JMethod method) {
        return method.getDeclaringClass().isApplication();
    }

    private boolean isApp(JMethod method) {
        String className = method.getDeclaringClass().getName();
        return !className.contains(".")
                || appPackages.stream().anyMatch(className::startsWith);
    }

    @Override
    public boolean shouldIgnore(CSCallSite csCallSite, int index) {
        return !targetMethods.contains(csCallSite.getContainer().getMethod());
    }

    @Override
    public Set<InferredTransfer> filter(CSCallSite csCallSite, int index, Set<InferredTransfer> transfers) {
        return transfers.stream()
                .filter(tf -> {
                    JMethod method = tf.getMethod();
                    return !ignoreMethods.contains(method)
                            && !ignoreClasses.contains(method.getDeclaringClass())
                            && !methodsWithTransfer.contains(method)
                            && !ignoreTypes.contains(tf.getType());
                })
                .collect(Collectors.toUnmodifiableSet());
    }
}
