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

package pascal.taie.pta.core.cs;

import pascal.taie.callgraph.Edge;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.pta.core.context.Context;

import java.util.Set;

import static pascal.taie.util.CollectionUtils.newHybridSet;

public class CSCallSite extends AbstractCSElement {

    private final InvokeExp callSite;
    /**
     * Context-sensitive method which contains this CS call site.
     */
    private CSMethod container;
    /**
     * Call edges from this call site.
     */
    private final Set<Edge<CSCallSite, CSMethod>> edges = newHybridSet();

    CSCallSite(InvokeExp callSite, Context context) {
        super(context);
        this.callSite = callSite;
    }

    public InvokeExp getCallSite() {
        return callSite;
    }

    public void setContainer(CSMethod container) {
        assert this.container == null; // should be set only once
        this.container = container;
    }

    public CSMethod getContainer() {
        return container;
    }

    public boolean addEdge(Edge<CSCallSite, CSMethod> edge) {
        return edges.add(edge);
    }

    public Set<Edge<CSCallSite, CSMethod>> getEdges() {
        return edges;
    }

    @Override
    public String toString() {
        return context + ":" + callSite;
    }
}
