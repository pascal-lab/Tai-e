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
import pascal.taie.analysis.dfa.fact.DataFlowResult;
import pascal.taie.analysis.graph.cfg.CFG;

public interface Solver<Node, Flow> {

    DataFlowResult<Node, Flow> solve(
            DataflowAnalysis<Node, Flow> analysis, CFG<Node> cfg);
}
