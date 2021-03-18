/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.analysis.dataflow.analysis;

/**
 * @param <Domain> Type for lattice values
 * @param <Node>   Type for nodes of control-flow graph
 */
public interface DataFlowAnalysis<Domain, Node> {

    /**
     * Returns whether the analysis is forward.
     */
    boolean isForward();

    /**
     * Returns initial in-flow for entry node.
     */
    Domain getEntryInitialFlow(Node entry);

    /**
     * Returns initial out-flow value for other nodes.
     */
    Domain newInitialFlow();

    /**
     * Meet function for two lattice values.
     * This function is used to handle control-flow confluences.
     */
    Domain meet(Domain v1, Domain v2);

    /**
     * Transfer function for the analysis.
     * The function transfer data-flow from in to out, and return whether
     * the out flow has been changed by the transfer.
     */
    boolean transfer(Node node, Domain in, Domain out);

}
