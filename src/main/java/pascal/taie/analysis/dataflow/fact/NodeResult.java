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

package pascal.taie.analysis.dataflow.fact;

import pascal.taie.analysis.StmtResult;
import pascal.taie.ir.stmt.Stmt;

/**
 * An interface for querying data-flow results.
 *
 * @param <Node> type of graph nodes
 * @param <Fact> type of data-flow facts
 */
public interface NodeResult<Node, Fact> extends StmtResult<Fact> {

    /**
     * @return the flowing-in fact of given node.
     */
    Fact getInFact(Node node);

    /**
     * @return the flowing-out fact of given node.
     */
    Fact getOutFact(Node node);

    /**
     * Typically, all {@code stmt}s are relevant in {@code NodeResult}.
     *
     * @return {@code true}.
     */
    @Override
    default boolean isRelevant(Stmt stmt) {
        return true;
    }

    /**
     * {@link NodeResult} is designed to be compatible with CFGs of both
     * stmt nodes and block nodes. When the node result instance represent
     * results of stmt nodes, it can be used as a {@link StmtResult}.
     *
     * @return out fact as the analysis result for given stmt.
     */
    @Override
    default Fact getResult(Stmt stmt) {
        return getOutFact((Node) stmt);
    }
}
