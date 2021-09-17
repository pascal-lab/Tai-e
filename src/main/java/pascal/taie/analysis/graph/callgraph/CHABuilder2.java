package pascal.taie.analysis.graph.callgraph;

import pascal.taie.World;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.collection.Sets;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.Set;

/**
 * Implementation of the CHA algorithm described in the lecture.
 * It does not use {@link ClassHierarchy#dispatch(JClass, MethodRef)},
 * and implements dispatch by itself.
 */
class CHABuilder2 implements CGBuilder<Invoke, JMethod> {

    private ClassHierarchy hierarchy;

    @Override
    public CallGraph<Invoke, JMethod> build() {
        hierarchy = World.getClassHierarchy();
        return buildCallGraph(World.getMainMethod());
    }

    private CallGraph<Invoke, JMethod> buildCallGraph(JMethod entry) {
        DefaultCallGraph callGraph = new DefaultCallGraph();
        callGraph.addEntryMethod(entry);
        Queue<JMethod> workList = new ArrayDeque<>();
        workList.add(entry);
        while (!workList.isEmpty()) {
            JMethod method = workList.poll();
            callGraph.addReachableMethod(method);
            callGraph.callSitesIn(method).forEach(invoke -> {
                Set<JMethod> callees = resolve(invoke);
                callees.forEach(callee -> {
                    if (!callGraph.contains(callee)) {
                        workList.add(callee);
                    }
                    callGraph.addEdge(new Edge<>(
                            CallGraphs.getCallKind(invoke), invoke, callee));
                });
            });
        }
        return callGraph;
    }

    /**
     * Resolves call targets (callees) of a call site via CHA.
     */
    private Set<JMethod> resolve(Invoke callSite) {
        CallKind kind = CallGraphs.getCallKind(callSite);
        MethodRef methodRef = callSite.getMethodRef();
        JClass jclass = methodRef.getDeclaringClass();
        Subsignature subsignature = methodRef.getSubsignature();
        switch (kind) {
            case INTERFACE:
            case VIRTUAL:
                Set<JMethod> targets = Sets.newSet();
                Queue<JClass> workList = new ArrayDeque<>();
                workList.add(jclass);
                while (!workList.isEmpty()) {
                    JClass c = workList.poll();
                    if (c.isInterface()) {
                        workList.addAll(hierarchy.getDirectSubinterfacesOf(c));
                        workList.addAll(hierarchy.getDirectImplementorsOf(c));
                    } else {
                        JMethod target = dispatch(c, subsignature);
                        if (target != null) {
                            targets.add(target);
                        }
                        workList.addAll(hierarchy.getDirectSubClassesOf(c));
                    }
                }
                return targets;
            case SPECIAL:
                return Set.of(dispatch(jclass, subsignature));
            case STATIC:
                return Set.of(jclass.getDeclaredMethod(subsignature));
            default:
                throw new AnalysisException("Failed to resolve call site: " + callSite);
        }
    }

    /**
     * Looks up the target method based on given class and method subsignature.
     *
     * @return the dispatched target method, or null if no satisfying method
     * can be found.
     */
    private JMethod dispatch(JClass jclass, Subsignature subsignature) {
        JMethod target = null;
        while (true) {
            JMethod m = jclass.getDeclaredMethod(subsignature);
            if (m != null && !m.isAbstract()) {
                target = m;
                break;
            }
            jclass = jclass.getSuperClass();
            if (jclass == null) {
                break;
            }
        }
        return target;
    }
}
