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

package pascal.taie.analysis.graph.callgraph;

import pascal.taie.util.Hashes;

/**
 * Represents call edges in the call graph.
 *
 * @param <CallSite> type of call sites
 * @param <Method>   type of methods
 */
public class Edge<CallSite, Method> {

    private final CallKind kind;

    private final CallSite callSite;

    private final Method callee;

    private final int hashCode;

    public Edge(CallKind kind, CallSite callSite, Method callee) {
        this.kind = kind;
        this.callSite = callSite;
        this.callee = callee;
        hashCode = Hashes.hash(kind, callSite, callee);
    }

    /**
     * @return kind of the call edge.
     */
    public CallKind getKind() {
        return kind;
    }

    /**
     * @return the call site (i.e., the source) of the call edge.
     */
    public CallSite getCallSite() {
        return callSite;
    }

    /**
     * @return the callee method (i.e., the target) of the call edge.
     */
    public Method getCallee() {
        return callee;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge<?, ?> edge = (Edge<?, ?>) o;
        return kind == edge.kind &&
                callSite.equals(edge.callSite) &&
                callee.equals(edge.callee);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return "[" + kind + "]" +
                callSite + " -> " + callee;
    }
}
