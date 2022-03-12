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

import pascal.taie.util.ObjectIdMapper;

/**
 * Maintains mappings between nodes in given CFG and their indexes.
 *
 * @param <Node> type of CFG nodes.
 */
public class CFGNodeMapper<Node> implements ObjectIdMapper<Node> {

    private final CFG<Node> cfg;

    public CFGNodeMapper(CFG<Node> cfg) {
        this.cfg = cfg;
    }

    @Override
    public int getId(Node node) {
        return cfg.getIndex(node);
    }

    @Override
    public Node getObject(int id) {
        return cfg.getNode(id);
    }
}
