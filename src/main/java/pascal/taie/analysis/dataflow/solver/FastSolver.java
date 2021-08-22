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
import pascal.taie.analysis.dataflow.fact.NodeResult;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.Edge;

import java.util.TreeSet;

/**
 * Work-list solver with optimization.
 */
class FastSolver<Node, Fact> extends AbstractSolver<Node, Fact> {

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
        if (analysis.hasEdgeTransfer()) {
            cfg.outEdgesOf(entry).forEach(edge ->
                    result.setEdgeFact(edge, entryFact));
        }
        cfg.forEach(node -> {
            // skip entry which has been initialized
            if (cfg.isEntry(node)) {
                return;
            }
            // initialize in fact
            if (cfg.inEdgesOf(node).count() == 1) {
                cfg.inEdgesOf(node).forEach(edge -> {
                    Fact in;
                    if (analysis.hasEdgeTransfer()) {
                        in = getOrNewEdgeOutFact(result, edge);
                    } else {
                        in = getOrNewOutFact(result, edge.getSource());
                    }
                    result.setInFact(edge.getTarget(), in);
                });
            } else {
                result.setInFact(node, analysis.newInitialFact());
            }
            // initialize out fact
            getOrNewOutFact(result, node);
            // initialize edge fact
            if (analysis.hasEdgeTransfer()) {
                cfg.outEdgesOf(node).forEach(edge ->
                        getOrNewEdgeOutFact(result, edge));
            }
        });
    }

    private Fact getOrNewOutFact(NodeResult<Node, Fact> result, Node node) {
        Fact fact = result.getOutFact(node);
        if (fact == null) {
            fact = analysis.newInitialFact();
            result.setOutFact(node, fact);
        }
        return fact;
    }

    private Fact getOrNewEdgeOutFact(DataflowResult<Node, Fact> result, Edge<Node> edge) {
        Fact fact = result.getEdgeFact(edge);
        if (fact == null) {
            if (analysis.needTransfer(edge)) {
                fact = analysis.newInitialFact();
            } else {
                fact = getOrNewOutFact(result, edge.getSource());
            }
            result.setEdgeFact(edge, fact);
        }
        return fact;
    }

    @Override
    protected void initializeBackward(CFG<Node> cfg, DataflowResult<Node, Fact> result) {
        // initialize exit
        Node exit = cfg.getExit();
        Fact exitFact = analysis.newBoundaryFact(cfg);
        result.setInFact(exit, exitFact);
        result.setOutFact(exit, exitFact);
        if (analysis.hasEdgeTransfer()) {
            cfg.inEdgesOf(exit).forEach(edge ->
                    result.setEdgeFact(edge, exitFact));
        }
        cfg.forEach(node -> {
            // skip exit which has been initialized
            if (cfg.isExit(node)) {
                return;
            }
            // initialize out fact
            if (cfg.outEdgesOf(node).count() == 1) {
                cfg.outEdgesOf(node).forEach(edge -> {
                    Fact out;
                    if (analysis.hasEdgeTransfer()) {
                        out = getOrNewEdgeInFact(result, edge);
                    } else {
                        out = getOrNewInFact(result, edge.getTarget());
                    }
                    result.setOutFact(edge.getSource(), out);
                });
            } else {
                result.setOutFact(node, analysis.newInitialFact());
            }
            // initialize in fact
            getOrNewInFact(result, node);
            // initialize edge fact
            if (analysis.hasEdgeTransfer()) {
                cfg.inEdgesOf(node).forEach(edge ->
                        getOrNewEdgeInFact(result, edge));
            }
        });
    }

    private Fact getOrNewInFact(NodeResult<Node, Fact> result, Node node) {
        Fact fact = result.getInFact(node);
        if (fact == null) {
            fact = analysis.newInitialFact();
            result.setInFact(node, fact);
        }
        return fact;
    }

    private Fact getOrNewEdgeInFact(DataflowResult<Node, Fact> result, Edge<Node> edge) {
        Fact fact = result.getEdgeFact(edge);
        if (fact == null) {
            if (analysis.needTransfer(edge)) {
                fact = analysis.newInitialFact();
            } else {
                fact = getOrNewInFact(result, edge.getTarget());
            }
            result.setEdgeFact(edge, fact);
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
            Fact in = result.getInFact(node);
            if (cfg.inEdgesOf(node).count() > 1) {
                cfg.inEdgesOf(node).forEach(inEdge -> {
                    Fact predOut = analysis.hasEdgeTransfer() ?
                            result.getEdgeFact(inEdge) :
                            result.getOutFact(inEdge.getSource());
                    analysis.mergeInto(predOut, in);
                });
            }
            // apply node transfer function
            Fact out = result.getOutFact(node);
            boolean changed = analysis.transferNode(node, in, out);
            if (changed) {
                cfg.outEdgesOf(node).forEach(outEdge -> {
                    if (analysis.hasEdgeTransfer() &&
                            analysis.needTransfer(outEdge)) {
                        // apply edge transfer if necessary
                        Fact edgeFact = result.getEdgeFact(outEdge);
                        analysis.transferEdge(outEdge, out, edgeFact);
                    }
                    // prepare to process successors
                    workList.add(outEdge.getTarget());
                });
            }
        }
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
            Fact out = result.getOutFact(node);
            if (cfg.outEdgesOf(node).count() > 1) {
                cfg.outEdgesOf(node).forEach(outEdge -> {
                    Fact succIn = analysis.hasEdgeTransfer() ?
                            result.getEdgeFact(outEdge) :
                            result.getInFact(outEdge.getTarget());
                    analysis.mergeInto(succIn, out);
                });
            }
            // apply node transfer function
            Fact in = result.getInFact(node);
            boolean changed = analysis.transferNode(node, in, out);
            if (changed) {
                cfg.inEdgesOf(node).forEach(inEdge -> {
                    if (analysis.hasEdgeTransfer() &&
                            analysis.needTransfer(inEdge)) {
                        // apply edge transfer if necessary
                        Fact edgeFact = result.getEdgeFact(inEdge);
                        analysis.transferEdge(inEdge, in, edgeFact);
                    }
                    // prepare to process successors
                    workList.add(inEdge.getSource());
                });
            }
        }
    }
}
