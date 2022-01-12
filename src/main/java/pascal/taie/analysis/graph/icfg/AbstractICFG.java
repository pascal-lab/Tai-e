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

package pascal.taie.analysis.graph.icfg;

import pascal.taie.analysis.graph.callgraph.CallGraph;

import java.util.Set;
import java.util.stream.Stream;

abstract class AbstractICFG<Method, Node> implements ICFG<Method, Node> {

    protected final CallGraph<Node, Method> callGraph;

    protected AbstractICFG(CallGraph<Node, Method> callGraph) {
        this.callGraph = callGraph;
    }

    @Override
    public Stream<Method> entryMethods() {
        return callGraph.entryMethods();
    }

    @Override
    public Set<Method> getCalleesOf(Node callSite) {
        return callGraph.getCalleesOf(callSite);
    }

    @Override
    public Set<Node> getCallersOf(Method method) {
        return callGraph.getCallersOf(method);
    }
}
