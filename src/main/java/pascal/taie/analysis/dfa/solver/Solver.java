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

import pascal.taie.analysis.dfa.analysis.DataflowAnalysis;
import pascal.taie.analysis.dfa.fact.DataflowResult;
import pascal.taie.analysis.graph.cfg.CFG;

import java.util.stream.Stream;

public abstract class Solver<Node, Fact> {

    protected final DataflowAnalysis<Node, Fact> analysis;

    protected final DirectionController controller;

    protected Solver(DataflowAnalysis<Node, Fact> analysis) {
        this.analysis = analysis;
        this.controller = analysis.isForward() ?
                new ForwardController() : new BackwardController();
    }

    public static <Node, Fact> Solver<Node, Fact> makeSolver(
            DataflowAnalysis<Node, Fact> analysis) {
        return new WorkListSolver<>(analysis);
    }

    public DataflowResult<Node, Fact> solve(CFG<Node> cfg) {
        DataflowResult<Node, Fact> result = initialize(cfg);
        doSolve(cfg, result);
        return result;
    }

    protected DataflowResult<Node, Fact> initialize(CFG<Node> cfg) {
        DataflowResult<Node, Fact> result = new DataflowResult<>();
        // TODO: initialize IN/OUT of entry node
        cfg.nodes().forEach(node -> {
            // FIXME: initialize either IN or OUT
            result.setInFact(node, analysis.newInitialFact());
            result.setOutFact(node, analysis.newInitialFact());
            if (analysis.hasEdgeTransfer()) {
                cfg.outEdgesOf(node).forEach(edge ->
                        result.setEdgeFact(edge, analysis.newInitialFact()));
            }
        });
        return result;
    }

    protected abstract void doSolve(CFG<Node> cfg,
                                    DataflowResult<Node, Fact> result);

    abstract class DirectionController {

        abstract Node getEntry(CFG<Node> cfg);

        abstract void meetIncomingFacts(DataflowResult<Node, Fact> result,
                                        CFG<Node> cfg, Node node);

        abstract void applyEdgeTransfer(DataflowResult<Node, Fact> result,
                                        CFG<Node> cfg, Node node);

        abstract Stream<Node> getOutgoingNodes(CFG<Node> cfg, Node node);
    }

    class ForwardController extends DirectionController {

        @Override
        Node getEntry(CFG<Node> cfg) {
            return cfg.getEntry();
        }

        @Override
        void meetIncomingFacts(DataflowResult<Node, Fact> result,
                               CFG<Node> cfg, Node node) {
            cfg.inEdgesOf(node).forEach(inEdge -> {
                Fact in = analysis.hasEdgeTransfer() ?
                        result.getEdgeFact(inEdge) :
                        result.getOutFact(inEdge.getSource());
                // TODO: meet facts
            });
        }

        @Override
        void applyEdgeTransfer(DataflowResult<Node, Fact> result, CFG<Node> cfg, Node node) {
            Fact outFact = result.getOutFact(node);
            cfg.outEdgesOf(node).forEach(outEdge -> {
                Fact edgeFact = result.getEdgeFact(outEdge);
                analysis.transferEdge(outEdge, outFact, edgeFact);
            });
        }

        @Override
        Stream<Node> getOutgoingNodes(CFG<Node> cfg, Node node) {
            return cfg.succsOf(node);
        }
    }

    class BackwardController extends DirectionController {

        @Override
        Node getEntry(CFG<Node> cfg) {
            return cfg.getExit();
        }

        @Override
        void meetIncomingFacts(DataflowResult<Node, Fact> result,
                               CFG<Node> cfg, Node node) {
            cfg.outEdgesOf(node).forEach(outEdge -> {
                Fact in = analysis.hasEdgeTransfer() ?
                        result.getEdgeFact(outEdge) :
                        result.getInFact(outEdge.getTarget());
                // TODO: meet facts
            });
        }

        @Override
        void applyEdgeTransfer(DataflowResult<Node, Fact> result, CFG<Node> cfg, Node node) {
            Fact inFact = result.getInFact(node);
            cfg.inEdgesOf(node).forEach(inEdge -> {
                Fact edgeFact = result.getEdgeFact(inEdge);
                analysis.transferEdge(inEdge, inFact, edgeFact);
            });
        }

        @Override
        Stream<Node> getOutgoingNodes(CFG<Node> cfg, Node node) {
            return cfg.predsOf(node);
        }
    }
}
