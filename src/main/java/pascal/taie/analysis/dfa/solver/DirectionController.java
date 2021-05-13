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

package pascal.taie.analysis.dfa.solver;

import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.Edge;

import java.util.stream.Stream;

enum DirectionController {

    FORWARD {
        @Override
        <Node> Node getEntry(CFG<Node> cfg) {
            return cfg.getEntry();
        }

        @Override
        <Node> Stream<Edge<Node>> getNextEdges(CFG<Node> cfg, Node node) {
            return cfg.outEdgesOf(node);
        }
    },

    BACKWARD {
        @Override
        <Node> Node getEntry(CFG<Node> cfg) {
            return cfg.getExit();
        }

        @Override
        <Node> Stream<Edge<Node>> getNextEdges(CFG<Node> cfg, Node node) {
            return cfg.inEdgesOf(node);
        }
    },
    ;

    abstract <Node> Node getEntry(CFG<Node> cfg);

    abstract <Node> Stream<Edge<Node>> getNextEdges(CFG<Node> cfg, Node node);
}
