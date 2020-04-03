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

package bamboo.icfg;

import bamboo.dataflow.analysis.EdgeTransfer;

import java.util.Objects;

public abstract class Edge<Node> {

    public enum Kind {
        LOCAL, // intra-procedural edge
        CALL, // call edge
        RETURN, // return edge
    }

    protected final Kind kind;

    protected final Node source;

    protected final Node target;

    private int hashCode = 0;

    public Edge(Kind kind, Node source, Node target) {
        this.kind = kind;
        this.source = source;
        this.target = target;
    }

    public Kind getKind() {
        return kind;
    }

    public Node getSource() {
        return source;
    }

    public Node getTarget() {
        return target;
    }

    public abstract <Domain> void accept(EdgeTransfer<Node, Domain> transfer,
                                Domain sourceInFlow, Domain sourceOutFlow,
                                Domain edgeFlow);

    protected int computeHashCode() {
        return Objects.hash(kind, source, target);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge<?> edge = (Edge<?>) o;
        return kind == edge.kind &&
                Objects.equals(source, edge.source) &&
                Objects.equals(target, edge.target);
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
        return kind + " Edge{" +  source + " -> " + target + '}';
    }
}
