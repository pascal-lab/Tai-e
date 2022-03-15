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

package pascal.taie.analysis.graph.cfg;

import pascal.taie.util.Indexer;

/**
 * Indexer for nodes in a CFG.
 *
 * @param <Node> type of CFG nodes.
 */
public record CFGNodeIndexer<Node>(CFG<Node> cfg) implements Indexer<Node> {

    @Override
    public int getIndex(Node node) {
        return cfg.getIndex(node);
    }

    @Override
    public Node getObject(int index) {
        return cfg.getNode(index);
    }
}
