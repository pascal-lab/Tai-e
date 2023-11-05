/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
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
import pascal.taie.util.collection.ArraySet;
import pascal.taie.util.collection.Views;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Represents context-sensitive call graph.
 */
public class CSCallGraph extends AbstractCallGraph<CSCallSite, CSMethod> {

    private final CSManager csManager;

    public CSCallGraph(CSManager csManager) {
        this.csManager = csManager;
    }

    /**
     * Adds an entry method to this call graph.
     */
    public void addEntryMethod(CSMethod entryMethod) {
        entryMethods.add(entryMethod);
    }

    /**
     * Adds a reachable method to this call graph.
     *
     * @return true if this call graph changed as a result of the call,
     * otherwise false.
     */
    public boolean addReachableMethod(CSMethod csMethod) {
        return reachableMethods.add(csMethod);
    }

    /**
     * Adds a new call graph edge to this call graph.
     *
     * @param edge the call edge to be added
     * @return true if the call graph changed as a result of the call,
     * otherwise false.
     */
    public boolean addEdge(Edge<CSCallSite, CSMethod> edge) {
        if (edge.getCallSite().addEdge(edge)) {
            edge.getCallee().addEdge(edge);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Set<CSCallSite> getCallersOf(CSMethod callee) {
        return Views.toMappedSet(callee.getEdges(), Edge::getCallSite);
    }

    @Override
    public Set<CSMethod> getCalleesOf(CSCallSite csCallSite) {
        return Views.toMappedSet(csCallSite.getEdges(), Edge::getCallee);
    }

    @Override
    public CSMethod getContainerOf(CSCallSite csCallSite) {
        return csCallSite.getContainer();
    }

    @Override
    public Set<CSCallSite> getCallSitesIn(CSMethod csMethod) {
        // Note: this method does not return the artificial Invokes
        // added to csMethod.getMethod().
        JMethod method = csMethod.getMethod();
        Context context = csMethod.getContext();
        ArrayList<CSCallSite> callSites = new ArrayList<>();
        for (Stmt s : method.getIR()) {
            if (s instanceof Invoke) {
                CSCallSite csCallSite = csManager.getCSCallSite(context, (Invoke) s);
                // each Invoke is iterated once, that we can ensure that
                // callSites contain no duplicate Invokes
                callSites.add(csCallSite);
            }
        }
        return Collections.unmodifiableSet(new ArraySet<>(callSites, true));
    }

    @Override
    public Stream<Edge<CSCallSite, CSMethod>> edgesOutOf(CSCallSite csCallSite) {
        return csCallSite.getEdges().stream();
    }

    @Override
    public Stream<Edge<CSCallSite, CSMethod>> edgesInTo(CSMethod csMethod) {
        return csMethod.getEdges().stream();
    }

    @Override
    public Stream<Edge<CSCallSite, CSMethod>> edges() {
        return reachableMethods.stream()
                .flatMap(this::callSitesIn)
                .flatMap(this::edgesOutOf);
    }

    @Override
    public boolean isRelevant(Stmt stmt) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<CSMethod> getResult(Stmt stmt) {
        throw new UnsupportedOperationException();
    }
}
