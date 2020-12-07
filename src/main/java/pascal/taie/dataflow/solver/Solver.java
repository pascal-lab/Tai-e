/*
 * Tai'e - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai'e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.dataflow.solver;

import pascal.taie.dataflow.analysis.DataFlowAnalysis;
import pascal.taie.util.ReversedDirectedGraph;
import soot.toolkits.graph.DirectedGraph;

import java.util.Map;

/**
 * @param <Domain> Type for lattice values
 * @param <Node>   Type for nodes of control-flow graph
 */
public abstract class Solver<Domain, Node> {

    protected final DataFlowAnalysis<Domain, Node> analysis;

    protected final DirectedGraph<Node> cfg;

    /**
     * In-flow value of each node.
     */
    protected Map<Node, Domain> inFlow;

    /**
     * Out-flow value of each node.
     */
    protected Map<Node, Domain> outFlow;

    protected Solver(DataFlowAnalysis<Domain, Node> analysis,
                     DirectedGraph<Node> cfg) {
        this.analysis = analysis;
        this.cfg = analysis.isForward() ? cfg : new ReversedDirectedGraph<>(cfg);
    }

    public void solve() {
        initialize(cfg);
        solveFixedPoint(cfg);
    }

    /**
     * Returns the data-flow value before each node.
     */
    public Map<Node, Domain> getBeforeFlow() {
        return analysis.isForward() ? inFlow : outFlow;
    }

    /**
     * Returns the data-flow value after each node.
     */
    public Map<Node, Domain> getAfterFlow() {
        return analysis.isForward() ? outFlow : inFlow;
    }

    protected void initialize(DirectedGraph<Node> cfg) {
        for (Node node : cfg) {
            if (cfg.getHeads().contains(node)) {
                inFlow.put(node, analysis.getEntryInitialFlow(node));
            }
            outFlow.put(node, analysis.newInitialFlow());
        }
    }

    protected abstract void solveFixedPoint(DirectedGraph<Node> cfg);
}
