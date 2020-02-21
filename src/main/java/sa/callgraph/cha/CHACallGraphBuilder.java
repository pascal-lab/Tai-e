package sa.callgraph.cha;

import sa.callgraph.CallGraph;
import sa.callgraph.CallKind;
import sa.callgraph.JimpleCallGraph;
import sa.util.AnalysisException;
import soot.FastHierarchy;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InterfaceInvokeExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.StaticInvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.VirtualInvokeExpr;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

public class CHACallGraphBuilder extends SceneTransformer {

    private static CallGraph<Unit, SootMethod> recentCallGraph;

    public static CallGraph<Unit, SootMethod> getRecentCallGraph() {
        return recentCallGraph;
    }

    private FastHierarchy hierarchy;

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        hierarchy = Scene.v().getOrMakeFastHierarchy();
        CallGraph<Unit, SootMethod> callGraph = build();
        recentCallGraph = callGraph;
        callGraph.forEach(System.out::println);
    }

    public CallGraph<Unit, SootMethod> build() {
        JimpleCallGraph callGraph = new JimpleCallGraph();
        callGraph.addEntryMethod(Scene.v().getMainMethod());
        buildEdges(callGraph);
        return callGraph;
    }

    private void buildEdges(JimpleCallGraph callGraph) {
        Queue<SootMethod> queue = new LinkedList<>(callGraph.getEntryMethods());
        while (!queue.isEmpty()) {
            SootMethod method = queue.remove();
            for (Unit callSite : callGraph.getCallSitesIn(method)) {
                Set<SootMethod> callees = resolveCalleesOf(callSite, callGraph);
                callees.forEach(callee -> {
                    if (!callGraph.contains(callee)) {
                        queue.add(callee);
                    }
                    callGraph.addEdge(callSite, callee, getCallKind(callSite));
                });
            }
        }
    }

    private Set<SootMethod> resolveCalleesOf(
            Unit callSite, CallGraph<Unit, SootMethod> callGraph) {
        InvokeExpr invoke = ((Stmt) callSite).getInvokeExpr();
        SootMethod method = invoke.getMethod();
        CallKind kind = getCallKind(callSite);
        switch (kind) {
            case VIRTUAL:
                return hierarchy.resolveAbstractDispatch(method.getDeclaringClass(), method);
            case SPECIAL:
                return Collections.singleton(hierarchy.resolveSpecialDispatch(
                        (SpecialInvokeExpr) invoke,
                        callGraph.getContainingMethod(callSite)));
            case STATIC:
                return Collections.singleton(method);
            default:
                throw new AnalysisException("Unknown invocation expression: " + invoke);
        }
    }

    private CallKind getCallKind(Unit callSite) {
        InvokeExpr invoke = ((Stmt) callSite).getInvokeExpr();
        if (invoke instanceof VirtualInvokeExpr ||
                invoke instanceof InterfaceInvokeExpr) {
            return CallKind.VIRTUAL;
        } else if (invoke instanceof SpecialInvokeExpr) {
            return CallKind.SPECIAL;
        } else if (invoke instanceof StaticInvokeExpr) {
            return CallKind.STATIC;
        } else {
            return CallKind.OTHER;
        }
    }
}
