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

import pascal.taie.util.Hashes;

public abstract class ICFGEdge<Node> {

    private final Node source;

    private final Node target;

    private int hashCode = 0;

    ICFGEdge(Node source, Node target) {
        this.source = source;
        this.target = target;
    }

    public Node getSource() {
        return source;
    }

    public Node getTarget() {
        return target;
    }

    protected int computeHashCode() {
        return Hashes.hash(source, target);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ICFGEdge<?> edge = (ICFGEdge<?>) o;
        return source.equals(edge.source) && target.equals(edge.target);
    }

    @Override
    public int hashCode() {
        if (hashCode == 0) {
            hashCode = computeHashCode();
        }
        return hashCode;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                "{" + source + " -> " + target + '}';
    }
}
