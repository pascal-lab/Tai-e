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
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.AbstractResultHolder;
import pascal.taie.util.ResultHolder;
import pascal.taie.util.collection.Sets;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Represents context-sensitive methods.
 */
public class CSMethod extends AbstractCSElement {

    private final JMethod method;

    /**
     * Call edges to this CS method.
     */
    private final Set<Edge<CSCallSite, CSMethod>> edges = Sets.newHybridSet();

    private final ResultHolder resultHolder = new AbstractResultHolder() {};

    CSMethod(JMethod method, Context context) {
        super(context);
        this.method = method;
    }

    /**
     * @return the method (without context).
     */
    public JMethod getMethod() {
        return method;
    }

    public void addEdge(Edge<CSCallSite, CSMethod> edge) {
        edges.add(edge);
    }

    public Set<Edge<CSCallSite, CSMethod>> getEdges() {
        return Collections.unmodifiableSet(edges);
    }

    public <R> R getResult(String id, Supplier<R> supplier) {
        return resultHolder.getResult(id, supplier);
    }

    public <R> Optional<R> getResult(String id) {
        return Optional.ofNullable(resultHolder.getResult(id));
    }

    @Override
    public String toString() {
        return context + ":" + method;
    }
}
