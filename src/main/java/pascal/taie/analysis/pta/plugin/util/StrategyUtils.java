package pascal.taie.analysis.pta.plugin.util;

import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.graph.callgraph.CallKind;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class StrategyUtils {

    private static final List<Class<? extends Type>> concernedTypes = List.of(ClassType.class);

    @Nullable
    public static CSVar getCSVar(CSManager csManager, CSCallSite csCallSite, int index) {
        Context context = csCallSite.getContext();
        if (csCallSite.getCallSite().isStatic() && index == InvokeUtils.BASE) {
            return null;
        }
        Var var = InvokeUtils.getVar(csCallSite.getCallSite(), index);
        if (var == null) {
            return null;
        }
        return csManager.getCSVar(context, var);
    }

    public static Set<Type> getTypes(Solver solver, CSVar csVar) {
        if (csVar == null) {
            return Set.of();
        }
        return solver.getPointsToSetOf(csVar)
                .objects()
                .map(csObj -> csObj.getObject().getType())
                .filter(type -> concernedTypes.contains(type.getClass()))
                .collect(Collectors.toSet());
    }

    public static Type getParamType(JMethod method, int index) {
        return switch (index) {
            case InvokeUtils.RESULT -> method.getReturnType();
            case InvokeUtils.BASE -> method.getDeclaringClass().getType();
            default -> method.getParamType(index);
        };
    }

    // Map from a reachable method to all corresponding CSCallSite.
    // Notice: Call edge with kind CallKind.OTHER will be ignored.
    public static MultiMap<JMethod, CSCallSite> getMethod2CSCallSites(CallGraph<CSCallSite, CSMethod> csCallGraph) {
        MultiMap<JMethod, CSCallSite> method2CSCallSite = Maps.newMultiMap();
        csCallGraph.reachableMethods()
                .forEach(csMethod -> method2CSCallSite.putAll(csMethod.getMethod(),
                        csCallGraph.edgesInTo(csMethod)
                                .filter(edge -> edge.getKind() != CallKind.OTHER)
                                .map(Edge::getCallSite)
                                .toList())
                );
        return method2CSCallSite;
    }

    public static MultiMap<JMethod, Invoke> getMethod2CallSites(CallGraph<Invoke, JMethod> callGraph) {
        MultiMap<JMethod, Invoke> method2CallSite = Maps.newMultiMap();
        callGraph.reachableMethods()
                .forEach(method -> method2CallSite.putAll(method,
                        callGraph.edgesInTo(method)
                                .filter(edge -> edge.getKind() != CallKind.OTHER)
                                .map(Edge::getCallSite)
                                .toList())
                );
        return method2CallSite;
    }
}
