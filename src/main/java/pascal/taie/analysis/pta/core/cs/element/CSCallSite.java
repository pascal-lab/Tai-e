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

package pascal.taie.analysis.pta.core.cs.element;

import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.util.collection.ArraySet;
import pascal.taie.util.collection.HybridArrayIndexableSet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;

/**
 * Represents context-sensitive call sites.
 */
public class CSCallSite extends AbstractCSElement {

    private final Invoke callSite;

    /**
     * Context-sensitive method which contains this CS call site.
     */
    private CSMethod container;

    private final Set<CSMethod> callees = new HybridArrayIndexableSet<>(true);

    /**
     * Call edges from this call site.
     */
    private final ArrayList<Edge<CSCallSite, CSMethod>> edges = new ArrayList<>(4);

    CSCallSite(Invoke callSite, Context context) {
        super(context);
        this.callSite = callSite;
    }

    /**
     * @return the call site (without context).
     */
    public Invoke getCallSite() {
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
        if (callees.add(edge.getCallee())) {
            return edges.add(edge);
        }
        return false;
    }

    public Set<Edge<CSCallSite, CSMethod>> getEdges() {
        return Collections.unmodifiableSet(new ArraySet<>(edges, true));
    }

    @Override
    public String toString() {
        return context + ":" + callSite;
    }
}
