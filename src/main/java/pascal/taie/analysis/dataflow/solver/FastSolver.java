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
import pascal.taie.util.collection.Streams;

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
            if (cfg.inEdgesOf(node).count() == 1) {
                cfg.inEdgesOf(node).forEach(edge -> {
                    if (!analysis.hasEdgeTransfer()) {
                        result.setInFact(node,
                                getOrNewOutFact(result, edge.getSource()));
                    }
                });
            } else {
                result.setInFact(node, analysis.newInitialFact());
            }
            // initialize out fact
            getOrNewOutFact(result, node);
        });
    }

    private Fact getOrNewOutFact(DataflowResult<Node, Fact> result, Node node) {
        Fact fact = result.getOutFact(node);
        if (fact == null) {
            fact = analysis.newInitialFact();
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
            int inDegree = (int) cfg.inEdgesOf(node).count();
            if (inDegree > 1) {
                in = result.getInFact(node);
                cfg.inEdgesOf(node).forEach(inEdge -> {
                    Fact fact = result.getOutFact(inEdge.getSource());
                    if (analysis.hasEdgeTransfer()) {
                        fact = analysis.transferEdge(inEdge, fact);
                    }
                    analysis.meetInto(fact, in);
                });
            } else if (inDegree == 1 && analysis.hasEdgeTransfer()) {
                Edge<Node> inEdge = Streams.getOne(cfg.inEdgesOf(node));
                in = analysis.transferEdge(inEdge,
                        result.getOutFact(inEdge.getSource()));
                result.setInFact(node, in);
            } else {
                in = result.getInFact(node);
            }
            // apply node transfer function
            Fact out = result.getOutFact(node);
            boolean changed = analysis.transferNode(node, in, out);
            if (changed) {
                cfg.succsOf(node).forEach(workList::add);
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
            if (cfg.outEdgesOf(node).count() == 1) {
                cfg.outEdgesOf(node).forEach(edge -> {
                    if (!analysis.hasEdgeTransfer()) {
                        result.setOutFact(node,
                                getOrNewInFact(result, edge.getTarget()));
                    }
                });
            } else {
                result.setOutFact(node, analysis.newInitialFact());
            }
            // initialize in fact
            getOrNewInFact(result, node);
        });
    }

    private Fact getOrNewInFact(DataflowResult<Node, Fact> result, Node node) {
        Fact fact = result.getInFact(node);
        if (fact == null) {
            fact = analysis.newInitialFact();
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
            int outDegree = (int) cfg.outEdgesOf(node).count();
            if (outDegree > 1) {
                out = result.getOutFact(node);
                cfg.outEdgesOf(node).forEach(outEdge -> {
                    Fact fact = result.getInFact(outEdge.getTarget());
                    if (analysis.hasEdgeTransfer()) {
                        fact = analysis.transferEdge(outEdge, fact);
                    }
                    analysis.meetInto(fact, out);
                });
            } else if (outDegree == 1 && analysis.hasEdgeTransfer()) {
                Edge<Node> outEdge = Streams.getOne(cfg.outEdgesOf(node));
                out = analysis.transferEdge(outEdge,
                        result.getOutFact(outEdge.getTarget()));
                result.setOutFact(node, out);
            } else {
                out = result.getOutFact(node);
            }
            // apply node transfer function
            Fact in = result.getInFact(node);
            boolean changed = analysis.transferNode(node, in, out);
            if (changed) {
                cfg.predsOf(node).forEach(workList::add);
            }
        }
    }
}
