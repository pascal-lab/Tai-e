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

package pascal.taie.frontend.java.ir;

import pascal.taie.util.collection.Lists;
import pascal.taie.util.graph.IndexedGraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * <p>Bytecode control flow graph representation optimized for performance.</p>
 *
 * <h2>Memory Layout Design</h2>
 * <p>The graph uses a hybrid storage strategy for edges:</p>
 * <ul>
 *   <li><b>Inline storage</b>: First 4 edges per node stored in flat arrays (most cases)</li>
 *   <li><b>Overflow storage</b>: Additional edges stored in separate jagged arrays (rare cases)</li>
 * </ul>
 *
 * <h3>Inline Array Layout</h3>
 * <pre>
 * Block 0: [edge1, edge2, edge3, edge4] | Block 1: [edge1, edge2, edge3, edge4] | ...
 *           ^^^^^  ^^^^^  ^^^^^  ^^^^^             ^^^^^  ^^^^^  ^^^^^  ^^^^^
 *           slot0  slot1  slot2  slot3             slot0  slot1  slot2  slot3
 * </pre>
 * <p>Edge count is stored separately in degree arrays (inDegree, outDegree, etc.).</p>
 *
 * <p>Example: A node with 6 edges stores the first 4 in inline region,
 * and the remaining 2 in its overflow array.</p>
 */
public class BytecodeCFG implements IndexedGraph<BytecodeBlock> {

    /**
     * Number of edges that can be stored inline per node (in flat arrays).
     * Each node occupies INLINE_CAPACITY consecutive integers in the flat array.
     * Most CFG nodes have ≤ 4 edges, making inline storage efficient.
     */
    private static final int INLINE_CAPACITY = 4;

    private final List<BytecodeBlock> blocks;

    private final BytecodeBlock entry;

    /**
     * Mapping from instruction index to block.
     */
    private final BytecodeBlock[] insn2Block;

    private final int[] preds;

    private final int[][] overflowPreds;

    private final int[] inDegree;

    private final int[] succs;

    private final int[][] overflowSuccs;

    private final int[] outDegree;

    private final int[][] exceptionPreds;

    private final int[] exceptionInDegree;

    private final int[][] exceptionSuccs;

    private final int[] exceptionOutDegree;

    /**
     * Constructs a new BytecodeCFG with the specified parameters.
     *
     * @param maxBlockSize The maximum number of blocks in the graph.
     * @param entry        The entry block of the CFG.
     * @param insn2Block   Mapping from instruction index to block.
     */
    BytecodeCFG(int maxBlockSize, BytecodeBlock entry, BytecodeBlock[] insn2Block) {
        blocks = new ArrayList<>(maxBlockSize);
        this.entry = entry;
        this.insn2Block = insn2Block;

        preds = new int[maxBlockSize * INLINE_CAPACITY];
        overflowPreds = new int[maxBlockSize][];
        inDegree = new int[maxBlockSize];

        succs = new int[maxBlockSize * INLINE_CAPACITY];
        overflowSuccs = new int[maxBlockSize][];
        outDegree = new int[maxBlockSize];

        exceptionPreds = new int[maxBlockSize][];
        exceptionInDegree = new int[maxBlockSize];

        exceptionSuccs = new int[maxBlockSize][];
        exceptionOutDegree = new int[maxBlockSize];
    }

    /**
     * Adds a block to this CFG, and returns the index of given block.
     */
    int addBlock(BytecodeBlock block) {
        int index = blocks.size();
        block.setIndex(index);
        blocks.add(block);
        return index;
    }

    /**
     * Searches for a valid block starting from the given instruction index.
     * Skips empty or merged blocks.
     *
     * @param start the starting instruction index
     * @return the first valid block at or after startIdx
     */
    BytecodeBlock searchForValidBlock(int start) {
        int i = start;
        while (insn2Block[i] == null || insn2Block[i].getIndex() == -1) {
            i++;
        }
        return insn2Block[i];
    }

    /**
     * Adds a control-flow edge (source -> target).
     *
     * @param source source block
     * @param target target block
     */
    void addEdge(int source, int target) {
        addEdge(preds, overflowPreds, inDegree, target, source);
        addEdge(succs, overflowSuccs, outDegree, source, target);
    }

    void addExceptionEdge(int source, int target) {
        addOverflowEdge(exceptionPreds, exceptionInDegree, target, source, 0);
        addOverflowEdge(exceptionSuccs, exceptionOutDegree, source, target, 0);
    }

    private void addEdge(int[] inlineRegion, int[][] overflowRegion, int[] degree,
                         int source, int taregt) {
        if (degree[source] < INLINE_CAPACITY) {
            addInlineRegionEdge(inlineRegion, degree, source, taregt);
        } else {
            addOverflowEdge(overflowRegion, degree, source, taregt, INLINE_CAPACITY);
        }
    }

    /**
     * add an edge (source -> target) to inline region
     */
    private void addInlineRegionEdge(int[] inlineRegion, int[] degree,
                                     int source, int target) {
        assert degree[source] < INLINE_CAPACITY;
        int index = source * INLINE_CAPACITY + degree[source];
        inlineRegion[index] = target;
        degree[source]++;
    }

    /**
     * add an edge (source -> target) to overflow region
     */
    private void addOverflowEdge(int[][] overflowRegion, int[] degree,
                                 int source, int target, int slotSize) {
        assert degree[source] >= slotSize;
        int nextIndex = degree[source] - slotSize;
        if (overflowRegion[source] == null) {
            overflowRegion[source] = new int[INLINE_CAPACITY];
        } else if (nextIndex >= overflowRegion[source].length) {
            int[] newRegion = new int[overflowRegion[source].length * 2];
            System.arraycopy(overflowRegion[source], 0, newRegion, 0,
                    overflowRegion[source].length);
            overflowRegion[source] = newRegion;
        }
        overflowRegion[source][nextIndex] = target;
        degree[source]++;
    }

    // ==================== IndexedGraph interface implementations ====================
    @Override
    public BytecodeBlock getEntry() {
        return entry;
    }

    @Override
    public int getIndex(BytecodeBlock node) {
        return node.getIndex();
    }

    @Override
    public BytecodeBlock getObject(int index) {
        return blocks.get(index);
    }

    @Override
    public int getInDegreeOf(BytecodeBlock node) {
        int index = getIndex(node);
        return inDegree[index] + exceptionInDegree[index];
    }

    @Override
    public Set<BytecodeBlock> getPredsOf(BytecodeBlock node) {
        int index = getIndex(node);
        int inDegree = getInDegreeOf(node);
        List<BytecodeBlock> preds = new ArrayList<>(inDegree);
        for (int i = 0; i < inDegree; ++i) {
            preds.add(getObject(getPredOf(index, i)));
        }
        return Lists.asSet(preds);
    }

    private int getPredOf(int node, int index) {
        if (index < inDegree[node]) {
            return getNormalPredOf(node, index);
        } else {
            return exceptionPreds[node][index - inDegree[node]];
        }
    }

    private int getNormalPredOf(int node, int index) {
        if (index < INLINE_CAPACITY) {
            return preds[node * INLINE_CAPACITY + index];
        } else {
            return overflowPreds[node][index - INLINE_CAPACITY];
        }
    }

    @Override
    public int getOutDegreeOf(BytecodeBlock node) {
        int index = getIndex(node);
        return outDegree[index] + exceptionOutDegree[index];
    }

    @Override
    public Set<BytecodeBlock> getSuccsOf(BytecodeBlock node) {
        int index = getIndex(node);
        int outDegree = getOutDegreeOf(node);
        List<BytecodeBlock> succs = new ArrayList<>(outDegree);
        for (int i = 0; i < outDegree; ++i) {
            succs.add(getObject(getSuccOf(index, i)));
        }
        return Lists.asSet(succs);
    }

    private int getSuccOf(int node, int index) {
        if (index < outDegree[node]) {
            return getNormalSuccOf(node, index);
        } else {
            return exceptionSuccs[node][index - outDegree[node]];
        }
    }

    private int getNormalSuccOf(int node, int index) {
        if (index < INLINE_CAPACITY) {
            return succs[node * INLINE_CAPACITY + index];
        } else {
            return overflowSuccs[node][index - INLINE_CAPACITY];
        }
    }

    @Override
    public Set<BytecodeBlock> getNodes() {
        return Lists.asSet(blocks);
    }

    // ========== Block-level convenience methods used by other classes ==========
    /**
     * @return the number of incoming normal edges for the given block.
     */
    int getNormalInDegreeOf(BytecodeBlock block) {
        return inDegree[block.getIndex()];
    }

    /**
     * @return the source block of the incoming normal edge at the given index.
     */
    BytecodeBlock getNormalPredOf(BytecodeBlock block, int index) {
        int sourceIndex = getNormalPredOf(block.getIndex(), index);
        return blocks.get(sourceIndex);
    }

    /**
     * Checks if the block has no incoming normal edges.
     * NOTE: catch blocks are treated as having no incoming edges for control flow purposes.
     */
    boolean hasNoIncomingNormalEdges(BytecodeBlock block) {
        return block.isCatch() || getNormalInDegreeOf(block) == 0;
    }

    /**
     * @return the normal successors of given block.
     */
    List<BytecodeBlock> getNormalSuccsOf(BytecodeBlock block) {
        int index = block.getIndex();
        int outDegree = this.outDegree[index];
        List<BytecodeBlock> succs = new ArrayList<>(outDegree);
        for (int i = 0; i < outDegree; i++) {
            succs.add(getObject(getNormalSuccOf(index, i)));
        }
        return succs;
    }
}
