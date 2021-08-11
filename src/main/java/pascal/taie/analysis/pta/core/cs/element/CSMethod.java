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
import pascal.taie.analysis.pta.plugin.exception.CSMethodThrowResult;
import pascal.taie.language.classes.JMethod;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static pascal.taie.util.collection.SetUtils.newHybridSet;

public class CSMethod extends AbstractCSElement {

    private final JMethod method;
    /**
     * Callers of this CS method.
     */
    private final Set<Edge<CSCallSite, CSMethod>> edges = newHybridSet();

    private CSMethodThrowResult throwResult;

    CSMethod(JMethod method, Context context) {
        super(context);
        this.method = method;
    }

    public JMethod getMethod() {
        return method;
    }

    public void addEdge(Edge<CSCallSite, CSMethod> edge) {
        edges.add(edge);
    }

    public Stream<Edge<CSCallSite, CSMethod>> edges() {
        return edges.stream();
    }

    public CSMethodThrowResult getOrCreateThrowResult() {
        if (throwResult == null) {
            throwResult = new CSMethodThrowResult();
        }
        return throwResult;
    }

    public Optional<CSMethodThrowResult> getThrowResult() {
        return Optional.ofNullable(throwResult);
    }

    @Override
    public String toString() {
        return context + ":" + method;
    }
}
