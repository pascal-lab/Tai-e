/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.analysis.pta.core.cs;

import pascal.taie.analysis.graph.callgraph.AbstractCallGraph;
import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class CSCallGraph extends AbstractCallGraph<CSCallSite, CSMethod> {

    private final CSManager csManager;

    public CSCallGraph(CSManager csManager) {
        this.csManager = csManager;
    }

    public void addEntryMethod(CSMethod entryMethod) {
        entryMethods.add(entryMethod);
        // Let pointer analysis explicitly call addNewMethod()
    }

    public void addEdge(Edge<CSCallSite, CSMethod> edge) {
        edge.getCallSite().addEdge(edge);
        edge.getCallee().addEdge(edge);
    }

    public boolean containsEdge(Edge<CSCallSite, CSMethod> edge) {
        return edge.getCallSite().getEdges().contains(edge);
    }

    public boolean addReachableMethod(CSMethod csMethod) {
        if (reachableMethods.add(csMethod)) {
            callSitesIn(csMethod).forEach(csCallSite ->
                    csCallSite.setContainer(csMethod));
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Stream<CSMethod> calleesOf(CSCallSite csCallSite) {
        return csCallSite.edges().map(Edge::getCallee);
    }

    @Override
    public Stream<CSCallSite> callersOf(CSMethod callee) {
        return callee.edges().map(Edge::getCallSite);
    }

    @Override
    public CSMethod getContainerMethodOf(CSCallSite csCallSite) {
        return csCallSite.getContainer();
    }

    @Override
    public Stream<CSCallSite> callSitesIn(CSMethod csMethod) {
        JMethod method = csMethod.getMethod();
        Context context = csMethod.getContext();
        List<CSCallSite> callSites = new ArrayList<>();
        for (Stmt s : method.getIR()) {
            if (s instanceof Invoke) {
                CSCallSite csCallSite = csManager.getCSCallSite(context, (Invoke) s);
                callSites.add(csCallSite);
            }
        }
        return callSites.stream();
    }

    @Override
    public Stream<Edge<CSCallSite, CSMethod>> edgesOf(CSCallSite csCallSite) {
        return csCallSite.edges();
    }

    @Override
    public Stream<Edge<CSCallSite, CSMethod>> edgesTo(CSMethod csMethod) {
        return csMethod.edges();
    }

    @Override
    public Stream<Edge<CSCallSite, CSMethod>> edges() {
        return reachableMethods.stream()
                .flatMap(this::callSitesIn)
                .flatMap(this::edgesOf);
    }
}
