/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.frontend.soot;

import pascal.taie.callgraph.CallGraph;
import pascal.taie.callgraph.JimpleCallGraph;
import pascal.taie.java.classes.JMethod;
import pascal.taie.pta.core.cs.CSCallSite;
import pascal.taie.pta.core.cs.CSMethod;
import pascal.taie.pta.core.solver.PointerAnalysis;
import pascal.taie.pta.ir.CallSite;
import pascal.taie.pta.ir.DefaultCallSite;
import pascal.taie.util.AnalysisException;
import soot.SootMethod;
import soot.Unit;

/**
 * Provides an interface to access pointer analysis results from
 * Jimple elements.
 */
public class JimplePointerAnalysis {

    private static final JimplePointerAnalysis INSTANCE =
            new JimplePointerAnalysis();

    private PointerAnalysis pta;

    private pascal.taie.pta.core.ci.PointerAnalysis cipta;

    private JimpleCallGraph jimpleCallGraph;

    public static JimplePointerAnalysis v() {
        return INSTANCE;
    }

    public void setPointerAnalysis(PointerAnalysis pta) {
        this.pta = pta;
        jimpleCallGraph = null;
    }

    public void setCIPointerAnalysis(
            pascal.taie.pta.core.ci.PointerAnalysis cipta) {
        this.cipta = cipta;
        jimpleCallGraph = null;
    }

    /**
     * Converts on-the-fly call graph to a JimpleCallGraph.
     * The result is cached.
     */
    public CallGraph<Unit, SootMethod> getJimpleCallGraph() {
        if (jimpleCallGraph == null) {
            jimpleCallGraph = new JimpleCallGraph();
            if (pta != null) {
                // Process context-sensitive call graph
                CallGraph<CSCallSite, CSMethod> callGraph = pta.getCallGraph();
                // Add entry methods
                callGraph.getEntryMethods()
                        .stream()
                        .map(this::toSootMethod)
                        .forEach(jimpleCallGraph::addEntryMethod);
                // Add call graph edges
                callGraph.forEach(edge -> {
                    Unit call = toSootUnit(edge.getCallSite());
                    SootMethod target = toSootMethod(edge.getCallee());
                    jimpleCallGraph.addEdge(call, target, edge.getKind());
                });
            } else if (cipta != null) {
                // Process context-insensitive call graph
                CallGraph<CallSite, JMethod> callGraph = cipta.getCallGraph();
                callGraph.getEntryMethods()
                        .stream()
                        .map(e -> (SootMethod) e.getMethodSource())
                        .forEach(jimpleCallGraph::addEntryMethod);
                // Add call graph edges
                callGraph.forEach(edge -> {
                    Unit call = ((DefaultCallSite) edge.getCallSite())
                            .getSootStmt();
                    SootMethod target =
                            (SootMethod) edge.getCallee().getMethodSource();
                    jimpleCallGraph.addEdge(call, target, edge.getKind());
                });
            } else {
                throw new AnalysisException("Pointer analysis has not run");
            }
        }
        return jimpleCallGraph;
    }

    private Unit toSootUnit(CSCallSite callSite) {
        return ((DefaultCallSite) callSite.getCallSite()).getSootStmt();
    }

    private SootMethod toSootMethod(CSMethod method) {
        return (SootMethod) method.getMethod().getMethodSource();
    }
}
