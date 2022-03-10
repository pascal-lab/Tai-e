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

package pascal.taie.analysis.dataflow.solver;

import pascal.taie.analysis.dataflow.analysis.DataflowAnalysis;
import pascal.taie.analysis.dataflow.fact.DataflowResult;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.Edge;
import pascal.taie.util.collection.CollectionUtils;

import java.util.TreeSet;

/**
 * Work-list solver with optimization.
 */
class FastSolver<Node, Fact> extends Solver<Node, Fact> {

    FastSolver(DataflowAnalysis<Node, Fact> analysis) {
        super(analysis);
    }

    @Override
    protected void initializeForward(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        // initialize entry
        Node entry = cfg.getEntry();
        Fact entryFact = analysis.newBoundaryFact(cfg);
        result.setInFact(entry, entryFact);
        result.setOutFact(entry, entryFact);
        cfg.forEach(node -> {
            // skip entry which has been initialized
            if (cfg.isEntry(node)) {
                return;
            }
            // initialize in fact
            if (cfg.getInDegreeOf(node) == 1) {
                cfg.getInEdgesOf(node).forEach(edge -> {
                    if (!analysis.needTransferEdge(edge)) {
                        result.setInFact(node,
                                getOrNewOutFact(result, cfg, edge.getSource()));
                    }
                });
            } else {
                result.setInFact(node, analysis.newInitialFact(cfg));
            }
            // initialize out fact
            getOrNewOutFact(result, cfg, node);
        });
    }

    private Fact getOrNewOutFact(
            DataflowResult<Node, Fact> result, CFG<Node> cfg, Node node) {
        Fact fact = result.getOutFact(node);
        if (fact == null) {
            fact = analysis.newInitialFact(cfg);
            result.setOutFact(node, fact);
        }
        return fact;
    }

    @Override
    protected void doSolveForward(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        TreeSet<Node> workList = new TreeSet<>(
                new Orderer<>(cfg, analysis.isForward()));
        cfg.forEach(node -> {
            if (!cfg.isEntry(node)) {
                workList.add(node);
            }
        });
        while (!workList.isEmpty()) {
            Node node = workList.pollFirst();
            // meet incoming facts
            Fact in;
            int inDegree = cfg.getInDegreeOf(node);
            if (inDegree > 1) {
                in = result.getInFact(node);
                cfg.getInEdgesOf(node).forEach(inEdge -> {
                    Fact fact = result.getOutFact(inEdge.getSource());
                    if (analysis.needTransferEdge(inEdge)) {
                        fact = analysis.transferEdge(inEdge, fact);
                    }
                    analysis.meetInto(fact, in);
                });
            } else if (inDegree == 1) {
                Edge<Node> inEdge = CollectionUtils.getOne(cfg.getInEdgesOf(node));
                if (analysis.needTransferEdge(inEdge)) {
                    in = analysis.transferEdge(inEdge,
                            result.getOutFact(inEdge.getSource()));
                    result.setInFact(node, in);
                } else {
                    in = result.getInFact(node);
                }
            } else {
                in = result.getInFact(node);
            }
            // apply node transfer function
            Fact out = result.getOutFact(node);
            boolean changed = analysis.transferNode(node, in, out);
            if (changed) {
                workList.addAll(cfg.getSuccsOf(node));
            }
        }
    }

    @Override
    protected void initializeBackward(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        // initialize exit
        Node exit = cfg.getExit();
        Fact exitFact = analysis.newBoundaryFact(cfg);
        result.setInFact(exit, exitFact);
        result.setOutFact(exit, exitFact);
        cfg.forEach(node -> {
            // skip exit which has been initialized
            if (cfg.isExit(node)) {
                return;
            }
            // initialize out fact
            if (cfg.getOutDegreeOf(node) == 1) {
                cfg.getOutEdgesOf(node).forEach(edge -> {
                    if (!analysis.needTransferEdge(edge)) {
                        result.setOutFact(node,
                                getOrNewInFact(result, cfg, edge.getTarget()));
                    }
                });
            } else {
                result.setOutFact(node, analysis.newInitialFact(cfg));
            }
            // initialize in fact
            getOrNewInFact(result, cfg, node);
        });
    }

    private Fact getOrNewInFact(
            DataflowResult<Node, Fact> result, CFG<Node> cfg, Node node) {
        Fact fact = result.getInFact(node);
        if (fact == null) {
            fact = analysis.newInitialFact(cfg);
            result.setInFact(node, fact);
        }
        return fact;
    }

    @Override
    protected void doSolveBackward(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        TreeSet<Node> workList = new TreeSet<>(
                new Orderer<>(cfg, analysis.isForward()));
        cfg.forEach(node -> {
            if (!cfg.isExit(node)) {
                workList.add(node);
            }
        });
        while (!workList.isEmpty()) {
            Node node = workList.pollFirst();
            // meet incoming facts
            Fact out;
            int outDegree = cfg.getOutDegreeOf(node);
            if (outDegree > 1) {
                out = result.getOutFact(node);
                cfg.getOutEdgesOf(node).forEach(outEdge -> {
                    Fact fact = result.getInFact(outEdge.getTarget());
                    if (analysis.needTransferEdge(outEdge)) {
                        fact = analysis.transferEdge(outEdge, fact);
                    }
                    analysis.meetInto(fact, out);
                });
            } else if (outDegree == 1) {
                Edge<Node> outEdge = CollectionUtils.getOne(cfg.getOutEdgesOf(node));
                if (analysis.needTransferEdge(outEdge)) {
                    out = analysis.transferEdge(outEdge,
                            result.getOutFact(outEdge.getTarget()));
                    result.setOutFact(node, out);
                } else {
                    out = result.getOutFact(node);
                }
            } else {
                out = result.getOutFact(node);
            }
            // apply node transfer function
            Fact in = result.getInFact(node);
            boolean changed = analysis.transferNode(node, in, out);
            if (changed) {
                workList.addAll(cfg.getPredsOf(node));
            }
        }
    }
}
