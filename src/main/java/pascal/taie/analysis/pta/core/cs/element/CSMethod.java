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

package pascal.taie.analysis.pta.core.cs.element;

import pascal.taie.analysis.graph.callgraph.Edge;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.AbstractResultHolder;
import pascal.taie.util.ResultHolder;
import pascal.taie.util.collection.ArraySet;

import java.util.ArrayList;
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
    private final ArrayList<Edge<CSCallSite, CSMethod>> edges = new ArrayList<>(4);

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
        // The caller has ensured that each edge added to CSMethod is unique
        edges.add(edge);
    }

    public Set<Edge<CSCallSite, CSMethod>> getEdges() {
        return Collections.unmodifiableSet(new ArraySet<>(edges, true));
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
