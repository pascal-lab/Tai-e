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

package pascal.taie.analysis.graph.cfg;

import pascal.taie.ir.IR;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.graph.Graph;

import java.util.Set;

/**
 * Representation of a control-flow graph of a method.
 *
 * @param <N> type of CFG nodes.
 */
public interface CFG<N> extends Graph<N> {

    /**
     * @return the IR of the method this CFG represents.
     */
    IR getIR();

    /**
     * @return the method this CFG represents.
     */
    JMethod getMethod();

    /**
     * @return the entry node of this CFG.
     */
    N getEntry();

    /**
     * @return the exit node of this CFG.
     */
    N getExit();

    /**
     * @return true if the given node is the entry of this CFG, otherwise false.
     */
    boolean isEntry(N node);

    /**
     * @return true if the given node is the exit of this CFG, otherwise false.
     */
    boolean isExit(N node);

    /**
     * @return a unique index for given node in this CFG.
     */
    int getIndex(N node);

    /**
     * @return the corresponding node specified by the given index.
     */
    N getNode(int index);

    /**
     * @return incoming edges of the given node.
     */
    @Override
    Set<CFGEdge<N>> getInEdgesOf(N node);

    /**
     * @return outgoing edges of the given node.
     */
    @Override
    Set<CFGEdge<N>> getOutEdgesOf(N node);
}
