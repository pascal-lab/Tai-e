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

package pascal.taie.analysis.graph.callgraph;

import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;

import java.util.Set;

/**
 * Default implementation of call graph.
 */
public class DefaultCallGraph extends AbstractCallGraph<Invoke, JMethod> {

    /**
     * Adds an entry method to this call graph.
     */
    public void addEntryMethod(JMethod entryMethod) {
        entryMethods.add(entryMethod);
    }

    /**
     * Adds a reachable method to this call graph.
     *
     * @return true if this call graph changed as a result of the call,
     * otherwise false.
     */
    public boolean addReachableMethod(JMethod method) {
        if (reachableMethods.add(method)) {
            if (!method.isAbstract()) {
                method.getIR().forEach(stmt -> {
                    if (stmt instanceof Invoke invoke) {
                        callSiteToContainer.put(invoke, method);
                        callSitesIn.put(method, invoke);
                    }
                });
            }
            return true;
        }
        return false;
    }

    /**
     * Adds a new call graph edge to this call graph.
     *
     * @param edge the call edge to be added
     * @return true if the call graph changed as a result of the call,
     * otherwise false.
     */
    public boolean addEdge(Edge<Invoke, JMethod> edge) {
        if (callSiteToEdges.put(edge.getCallSite(), edge)) {
            calleeToEdges.put(edge.getCallee(), edge);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public JMethod getContainerOf(Invoke invoke) {
        return invoke.getContainer();
    }

    @Override
    public boolean isRelevant(Stmt stmt) {
        return stmt instanceof Invoke;
    }

    @Override
    public Set<JMethod> getResult(Stmt stmt) {
        return getCalleesOf((Invoke) stmt);
    }
}
