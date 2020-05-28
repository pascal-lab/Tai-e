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

package bamboo.pta.jimple;

import bamboo.callgraph.CallGraph;
import bamboo.callgraph.JimpleCallGraph;
import bamboo.pta.analysis.context.DefaultContext;
import bamboo.pta.analysis.data.CSCallSite;
import bamboo.pta.analysis.data.CSMethod;
import bamboo.pta.analysis.data.DataManager;
import bamboo.pta.analysis.solver.PointerAnalysis;
import bamboo.pta.element.CallSite;
import bamboo.pta.element.Method;
import bamboo.pta.set.PointsToSet;
import bamboo.util.AnalysisException;
import soot.Local;
import soot.SootMethod;
import soot.Unit;

import java.util.Collection;

/**
 * Provides an interface to access pointer analysis results from
 * Jimple elements.
 */
public class JimplePointerAnalysis {

    private static final JimplePointerAnalysis INSTANCE =
            new JimplePointerAnalysis();

    public static JimplePointerAnalysis v() {
        return INSTANCE;
    }

    private PointerAnalysis pta;

    private bamboo.pta.analysis.ci.PointerAnalysis cipta;

    private ElementManager elementManager;

    private DataManager dataManager;

    private JimpleCallGraph jimpleCallGraph;

    public void setPointerAnalysis(PointerAnalysis pta) {
        this.pta = pta;
        elementManager = ((JimpleProgramManager) pta.getProgramManager())
                .getElementManager();
        dataManager = pta.getDataManager();
        jimpleCallGraph = null;
    }

    public void setCIPointerAnalysis(
            bamboo.pta.analysis.ci.PointerAnalysis cipta) {
        this.cipta = cipta;
        elementManager = ((JimpleProgramManager) cipta.getProgramManager())
                .getElementManager();
        jimpleCallGraph = null;
    }

    /**
     * @return all local variables of a given method.
     */
    public Collection<JimpleVariable> localVariablesOf(SootMethod method) {
        return elementManager.getLocalVariablesOf(
                elementManager.getMethod(method));
    }

    /**
     * @return points-to set of a given variable.
     */
    public PointsToSet pointsToSetOf(JimpleVariable var) {
        if (!pta.isContextSensitive()) {
            return dataManager.getCSVariable(DefaultContext.INSTANCE, var)
                    .getPointsToSet();
        } else {
            throw new UnsupportedOperationException(
                    "Context-sensitive points-to set is not supported yet.");
        }
    }

    /**
     * @return points-to set of a local in a given method.
     */
    public PointsToSet pointsToSetOf(SootMethod container, Local local) {
        JimpleMethod m = elementManager.getMethod(container);
        JimpleVariable var = elementManager.getVariable(local, m);
        return pointsToSetOf(var);
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
