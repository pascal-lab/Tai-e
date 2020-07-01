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

package bamboo.callgraph;

import java.util.Objects;

public class Edge<CallSite, Method> {

    private final CallKind kind;

    private final CallSite callSite;

    private final Method callee;

    private final int hashCode;

    public Edge(CallKind kind, CallSite callSite, Method callee) {
        this.kind = kind;
        this.callSite = callSite;
        this.callee = callee;
        hashCode = Objects.hash(kind, callSite, callee);
    }

    public CallKind getKind() {
        return kind;
    }

    public CallSite getCallSite() {
        return callSite;
    }

    public Method getCallee() {
        return callee;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge<?, ?> edge = (Edge<?, ?>) o;
        return kind == edge.kind &&
                Objects.equals(callSite, edge.callSite) &&
                Objects.equals(callee, edge.callee);
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
