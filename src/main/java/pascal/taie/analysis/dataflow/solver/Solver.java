/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.dataflow.solver;

import pascal.taie.analysis.dataflow.analysis.DataflowAnalysis;
import pascal.taie.analysis.dataflow.fact.DataflowResult;

/**
 * Interface of data-flow analysis solver.
 *
 * @param <Node> type of control-flow graph nodes
 * @param <Fact> type of data facts
 */
public interface Solver<Node, Fact> {

    /**
     * The default solver.
     */
    @SuppressWarnings("rawtypes")
    Solver SOLVER = new WorkListSolver<>();

    /**
     * Static factory method for obtaining a solver.
     */
    @SuppressWarnings("unchecked")
    static <Node, Fact> Solver<Node, Fact> getSolver() {
        return (Solver<Node, Fact>) SOLVER;
    }

    /**
     * Solves the given analysis problem.
     *
     * @return the data-flow analysis result
     */
    DataflowResult<Node, Fact> solve(DataflowAnalysis<Node, Fact> analysis);
}
