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

import pascal.taie.frontend.java.ir.ssa.IndexedGraph;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * <p>Bytecode control flow graph representation optimized for performance.</p>
 *
 * <h2>Memory Layout Design</h2>
 * <p>The graph uses a hybrid storage strategy for edges:</p>
 * <ul>
 *   <li><b>Inline storage</b>: First 3 edges per node stored in flat arrays (most cases)</li>
 *   <li><b>Overflow storage</b>: Additional edges stored in separate jagged arrays (rare cases)</li>
 * </ul>
 *
 * <h3>Inline Array Layout</h3>
 * <pre>
 * Block 0: [count, edge1, edge2, edge3] | Block 1: [count, edge1, edge2, edge3] | ...
 *           ^^^^^  ^^^^^^^^^^^^^^^^^^^             ^^^^^  ^^^^^^^^^^^^^^^^^^^
 *           slot0  slot1  slot2  slot3             slot0  slot1  slot2  slot3
 * </pre>
 *
 * <p>Example: A node with 5 edges stores the first 3 in inline region,
 * and the remaining 2 in its overflow array.</p>
 */
public class BytecodeCFG implements
        IndexedGraph<BytecodeBlock>, Iterable<BytecodeBlock> {

    /**
     * Number of edges that can be stored inline per node (in flat arrays).
     * Most CFG nodes have ≤ 3 edges, making inline storage efficient.
     */
    private static final int INLINE_CAPACITY = 3;

    /**
     * Number of slots per node in flat arrays: 1 count slot + INLINE_CAPACITY.
     * Each node occupies SLOT_SIZE consecutive integers in the flat array.
     */
    private static final int SLOT_SIZE = 1 + INLINE_CAPACITY;

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
     * @param maxBlockSize    The maximum number of blocks in the graph.
     * @param entry           The entry block of the CFG.
     * @param insn2Block       Mapping from instruction index to block.
     */
    BytecodeCFG(int maxBlockSize, BytecodeBlock entry, BytecodeBlock[] insn2Block) {
        blocks = new ArrayList<>(maxBlockSize);
        this.entry = entry;
        this.insn2Block = insn2Block;

        preds = new int[maxBlockSize * SLOT_SIZE];
        overflowPreds = new int[maxBlockSize][];
        inDegree = new int[maxBlockSize];

        succs = new int[maxBlockSize * SLOT_SIZE];
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
     * @param source source block
     * @param target target block
     */
    void addEdge(int source, int target) {
        addEdge(preds, overflowPreds, inDegree, target, source);
        addEdge(succs, overflowSuccs, outDegree, source, target);
    }

    private void addEdge(int[] inlineRegion, int[][] overflowRegion, int[] degree,
                         int source, int taregt) {
        if (degree[source] < SLOT_SIZE) {
            addInlineRegionEdge(inlineRegion, degree, source, taregt);
        } else {
            addOverflowEdge(overflowRegion, degree, source, taregt, SLOT_SIZE);
        }
    }

    /**
     * add an edge (source -> target) to inline region
     */
    private void addInlineRegionEdge(int[] inlineRegion, int[] degree,
                                     int source, int target) {
        assert degree[source] < SLOT_SIZE;
        int index = source * SLOT_SIZE + degree[source];
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
            overflowRegion[source] = new int[SLOT_SIZE];
        } else if (nextIndex >= overflowRegion[source].length) {
            int[] newRegion = new int[overflowRegion[source].length * 2];
            System.arraycopy(overflowRegion[source], 0, newRegion, 0,
                    overflowRegion[source].length);
            overflowRegion[source] = newRegion;
        }
        overflowRegion[source][nextIndex] = target;
        degree[source]++;
    }

    void addExceptionEdge(int source, int target) {
        addOverflowEdge(exceptionPreds, exceptionInDegree, target, source, 0);
        addOverflowEdge(exceptionSuccs, exceptionOutDegree, source, target, 0);
    }

    int getOutDegree(int block) {
        return outDegree[block];
    }

    int getPred(int block, int index) {
        if (index < SLOT_SIZE) {
            return preds[block * SLOT_SIZE + index];
        } else {
            return overflowPreds[block][index - SLOT_SIZE];
        }
    }

    int getSucc(int block, int index) {
        if (index < SLOT_SIZE) {
            return succs[block * SLOT_SIZE + index];
        } else {
            return overflowSuccs[block][index - SLOT_SIZE];
        }
    }

    @Override
    public BytecodeBlock getEntry() {
        return entry;
    }

    /**
     * Only for debug propose. The performance will be very bad.
     */
    @Override
    public List<BytecodeBlock> getPredsEx(BytecodeBlock block) {
        List<BytecodeBlock> preds = new ArrayList<>();
        int index = block.getIndex();
        for (int i = 0; i < getInDegreeEx(index); ++i) {
            preds.add(getNode(getPredEx(index, i)));
        }
        return preds;
    }

    @Override
    public List<BytecodeBlock> getSuccsEx(BytecodeBlock block) {
        List<BytecodeBlock> succs = new ArrayList<>();
        int index = block.getIndex();
        for (int i = 0; i < getOutDegreeEx(index); ++i) {
            succs.add(getNode(getSuccEx(index, i)));
        }
        return succs;
    }

    @Override
    public List<BytecodeBlock> getSuccs(BytecodeBlock block) {
        List<BytecodeBlock> preds = new ArrayList<>();
        int index = block.getIndex();
        for (int i = 0; i < outDegree[index]; i++) {
            preds.add(getNode(getSucc(index, i)));
        }
        return preds;
    }

    @Override
    public BytecodeBlock getNode(int index) {
        return blocks.get(index);
    }

    @Override
    public int getIndex(BytecodeBlock node) {
        return node.getIndex();
    }

    @Override
    public int size() {
        return blocks.size();
    }

    @Override
    public int getInDegreeEx(int node) {
        return inDegree[node] + exceptionInDegree[node];
    }

    @Override
    public int getOutDegreeEx(int node) {
        return outDegree[node] + exceptionOutDegree[node];
    }

    @Override
    public int getPredEx(int node, int index) {
        if (index < inDegree[node]) {
            return getPred(node, index);
        } else {
            return exceptionPreds[node][index - inDegree[node]];
        }
    }

    @Override
    public int getSuccEx(int node, int index) {
        if (index < outDegree[node]) {
            return getSucc(node, index);
        } else {
            return exceptionSuccs[node][index - outDegree[node]];
        }
    }

    @Nonnull
    @Override
    public Iterator<BytecodeBlock> iterator() {
        return blocks.iterator();
    }

    // ==================== Block-level convenience methods ====================

    /**
     * Returns the source block of the incoming edge at the given index.
     */
    BytecodeBlock getPred(BytecodeBlock block, int index) {
        int sourceIndex = getPred(block.getIndex(), index);
        return blocks.get(sourceIndex);
    }

    /**
     * Returns the number of incoming edges for the given block.
     */
    int getInDegree(BytecodeBlock block) {
        return inDegree[block.getIndex()];
    }

    /**
     * Checks if the block has no incoming edges.
     * NOTE: catch blocks are treated as having no incoming edges for control flow purposes.
     */
    boolean hasNoIncomingEdges(BytecodeBlock block) {
        return block.isCatch() || getInDegree(block) == 0;
    }
}
