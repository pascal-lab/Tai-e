/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C)  2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C)  2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package panda.pta.jimple;

import panda.callgraph.CallGraph;
import panda.callgraph.JimpleCallGraph;
import panda.pta.core.cs.CSCallSite;
import panda.pta.core.cs.CSManager;
import panda.pta.core.cs.CSMethod;
import panda.pta.core.solver.PointerAnalysis;
import panda.pta.element.CallSite;
import panda.pta.element.Method;
import panda.util.AnalysisException;
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
    private panda.pta.core.ci.PointerAnalysis cipta;
    private IRBuilder irBuilder;
    private CSManager csManager;
    private JimpleCallGraph jimpleCallGraph;

    public static JimplePointerAnalysis v() {
        return INSTANCE;
    }

    public void setPointerAnalysis(PointerAnalysis pta) {
        this.pta = pta;
        irBuilder = ((JimpleProgramManager) pta.getProgramManager())
                .getIRBuilder();
        csManager = pta.getCSManager();
        jimpleCallGraph = null;
    }

    public void setCIPointerAnalysis(
            panda.pta.core.ci.PointerAnalysis cipta) {
        this.cipta = cipta;
        irBuilder = ((JimpleProgramManager) cipta.getProgramManager())
                .getIRBuilder();
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
                CallGraph<CallSite, Method> callGraph = cipta.getCallGraph();
                callGraph.getEntryMethods()
                        .stream()
                        .map(m -> ((JimpleMethod) m).getSootMethod())
                        .forEach(jimpleCallGraph::addEntryMethod);
                // Add call graph edges
                callGraph.forEach(edge -> {
                    Unit call = ((JimpleCallSite) edge.getCallSite())
                            .getSootStmt();
                    SootMethod target = ((JimpleMethod) edge.getCallee())
                            .getSootMethod();
                    jimpleCallGraph.addEdge(call, target, edge.getKind());
                });
            } else {
                throw new AnalysisException("Pointer analysis has not run");
            }
        }
        return jimpleCallGraph;
    }

    private Unit toSootUnit(CSCallSite callSite) {
        return ((JimpleCallSite) callSite.getCallSite()).getSootStmt();
    }

    private SootMethod toSootMethod(CSMethod method) {
        return ((JimpleMethod) method.getMethod()).getSootMethod();
    }
}
