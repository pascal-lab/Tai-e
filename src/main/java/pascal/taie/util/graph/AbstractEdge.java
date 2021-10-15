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

package pascal.taie.util.graph;

public abstract class AbstractEdge<N> implements Edge<N> {

    /**
     * The source node of the edge.
     */
    protected final N source;

    /**
     * The target node of the edge.
     */
    protected final N target;

    protected AbstractEdge(N source, N target) {
        this.source = source;
        this.target = target;
    }

    @Override
    public N getSource() {
        return source;
    }

    @Override
    public N getTarget() {
        return target;
    }
}
