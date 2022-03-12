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

package pascal.taie.analysis.dataflow.analysis;

import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.Edge;

public abstract class AbstractDataflowAnalysis<Node, Fact>
        implements DataflowAnalysis<Node, Fact> {

    protected final CFG<Node> cfg;

    protected AbstractDataflowAnalysis(CFG<Node> cfg) {
        this.cfg = cfg;
    }

    /**
     * By default, a data-flow analysis does not have edge transfer, i.e.,
     * does not need to perform transfer for any edges.
     */
    @Override
    public boolean needTransferEdge(Edge<Node> edge) {
        return false;
    }

    @Override
    public Fact transferEdge(Edge<Node> edge, Fact nodeFact) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CFG<Node> getCFG() {
        return cfg;
    }
}
