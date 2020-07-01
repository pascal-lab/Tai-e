/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.callgraph.cha;

import bamboo.callgraph.CallGraph;
import bamboo.callgraph.CallKind;
import bamboo.callgraph.JimpleCallGraph;
import bamboo.util.AnalysisException;
import soot.FastHierarchy;
import soot.Scene;
import soot.SceneTransformer;
import soot.SootClass;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.InvokeExpr;
import soot.jimple.SpecialInvokeExpr;
import soot.jimple.Stmt;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import static bamboo.callgraph.JimpleCallUtils.getCallKind;

public class CHACallGraphBuilder extends SceneTransformer {

    private static final CHACallGraphBuilder INSTANCE = new CHACallGraphBuilder();

    public static CHACallGraphBuilder v() {
        return INSTANCE;
    }

    private CHACallGraphBuilder() {}

    private CallGraph<Unit, SootMethod> recentCallGraph;

    public CallGraph<Unit, SootMethod> getRecentCallGraph() {
        return recentCallGraph;
    }

    private FastHierarchy hierarchy;

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        hierarchy = Scene.v().getOrMakeFastHierarchy();
        recentCallGraph = build();
    }

    public CallGraph<Unit, SootMethod> build() {
        JimpleCallGraph callGraph = new JimpleCallGraph();
        callGraph.addEntryMethod(Scene.v().getMainMethod());
        buildCallGraph(callGraph);
        return callGraph;
    }

    private void buildCallGraph(JimpleCallGraph callGraph) {
        Queue<SootMethod> queue = new LinkedList<>(callGraph.getEntryMethods());
        while (!queue.isEmpty()) {
            SootMethod method = queue.remove();
            for (Unit callSite : callGraph.getCallSitesIn(method)) {
                Set<SootMethod> sootCallees = resolveCalleesOf(callSite, callGraph);
                Set<SootMethod> callees = resolveCalleesOf(callSite);
                if (!sootCallees.equals(callees)) {
                    System.out.println("sootCallees: " + sootCallees);
                    System.out.println("callees: " + callees);
                    throw new RuntimeException("sootCallees != callees");
                }
                callees.forEach(callee -> {
                    if (!callGraph.contains(callee)) {
                        queue.add(callee);
                    }
                    callGraph.addEdge(callSite, callee, getCallKind(callSite));
                });
            }
        }
    }

    /**
     * Leverages Soot to resolve callees.
     */
    private Set<SootMethod> resolveCalleesOf(
            Unit callSite, CallGraph<Unit, SootMethod> callGraph) {
        InvokeExpr invoke = ((Stmt) callSite).getInvokeExpr();
        SootMethod method = invoke.getMethod();
        CallKind kind = getCallKind(callSite);
        switch (kind) {
            case INTERFACE:
            case VIRTUAL:
                return hierarchy.resolveAbstractDispatch(method.getDeclaringClass(), method);
            case SPECIAL:
                return Collections.singleton(hierarchy.resolveSpecialDispatch(
                        (SpecialInvokeExpr) invoke,
                        callGraph.getContainerMethodOf(callSite)));
            case STATIC:
                return Collections.singleton(method);
            default:
                throw new AnalysisException("Unknown invocation expression: " + invoke);
        }
    }

    /**
     * Resolves call targets (callees) of a call site via class hierarchy analysis.
     */
    private Set<SootMethod> resolveCalleesOf(Unit callSite) {
        InvokeExpr invoke = ((Stmt) callSite).getInvokeExpr();
        SootMethod method = invoke.getMethod();
        CallKind kind = getCallKind(callSite);
        switch (kind) {
            case INTERFACE: // invokeinterface
            case VIRTUAL: { // invokevirtual
                Set<SootMethod> targets = new HashSet<>();
                SootClass cls = method.getDeclaringClass();
                Deque<SootClass> workList = new ArrayDeque<>();
                workList.add(cls);
                while (!workList.isEmpty()) {
                    SootClass c = workList.poll();
                    if (c.isInterface()) {
                        workList.addAll(hierarchy.getAllImplementersOfInterface(c));
                    } else {
                        SootMethod target = dispatch(c, method);
                        if (target != null) {
                            targets.add(target);
                        }
                        workList.addAll(hierarchy.getSubclassesOf(c));
                    }
                }
                return targets;
            }
            case SPECIAL: // invokespecial
            case STATIC: // invokestatic
                return Collections.singleton(method);
            default:
                throw new AnalysisException("Unknown invocation expression: " + invoke);
        }
    }

    /**
     * Looks up the target method based on given class and method signature.
     * Returns null if no satisfying method can be found.
     */
    private SootMethod dispatch(SootClass cls, SootMethod method) {
        SootMethod target = null;
        String subSig = method.getSubSignature();
        while (true) {
            SootMethod m = cls.getMethodUnsafe(subSig);
            if (m != null && m.isConcrete()) {
                target = m;
                break;
            }
            cls = cls.getSuperclassUnsafe();
            if (cls == null) {
                break;
            }
        }
        return target;
    }
}
