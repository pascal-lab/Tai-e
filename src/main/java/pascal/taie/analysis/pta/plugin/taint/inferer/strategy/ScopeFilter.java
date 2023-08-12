package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.taint.TaintConfig;
import pascal.taie.analysis.pta.plugin.taint.TaintTransfer;
import pascal.taie.analysis.pta.plugin.taint.inferer.InfererContext;
import pascal.taie.analysis.pta.plugin.taint.inferer.InferredTransfer;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Sets;

import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class ScopeFilter implements TransInferStrategy {
    private Set<String> appPackages;
    private Set<JMethod> methodsWithTransfer;
    private Set<JClass> ignoreClasses;
    private Set<JMethod> ignoreMethods;
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

        PointerAnalysisResult ptaResult = solver.getResult();
        CallGraph<Invoke, JMethod> callGraph = ptaResult.getCallGraph();

        switch (taintConfig.inferenceConfig().scope()) {
            case APP -> {
                Set<JMethod> appMethods = callGraph.reachableMethods()
                        .filter(this::isApp)
                        .collect(Collectors.toUnmodifiableSet());
                Set<JMethod> firstNonAppMethods = appMethods.stream()
                        .map(callGraph::getCalleesOfM)
                        .flatMap(Collection::stream)
                        .filter(Predicate.not(this::isApp))
                        .collect(Collectors.toUnmodifiableSet());
                targetMethods = Sets.newSet(appMethods);
                targetMethods.addAll(firstNonAppMethods);
            }
            case APP_LIB -> {
                Set<JMethod> appLibMethods = callGraph.reachableMethods()
                        .filter(this::isAppOrLib)
                        .collect(Collectors.toSet());
                Set<JMethod> firstNonAppLibMethods = appLibMethods.stream()
                        .map(callGraph::getCalleesOfM)
                        .flatMap(Collection::stream)
                        .filter(Predicate.not(this::isAppOrLib))
                        .collect(Collectors.toSet());
                targetMethods = Sets.newSet(appLibMethods);
                targetMethods.addAll(firstNonAppLibMethods);
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
    public Set<InferredTransfer> filter(CSCallSite csCallSite, int index, Set<InferredTransfer> transfers) {
        return transfers.stream()
                .filter(tf -> {
                    JMethod method = tf.getMethod();
                    return !ignoreMethods.contains(method)
                            && !ignoreClasses.contains(method.getDeclaringClass())
                            && !methodsWithTransfer.contains(method)
                            && targetMethods.contains(method);
                })
                .collect(Collectors.toUnmodifiableSet());
    }
}
