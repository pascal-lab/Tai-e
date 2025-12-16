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

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Bytecode control flow graph representation optimized for performance.</p>
 * <p>The graph is represented using adjacency lists for in-edges, out-edges,
 * and exception edges.
 * Each node can have up to three edges stored directly in the main arrays.
 * If a node has more than three edges, the additional edges are stored
 * in extra arrays.</p>
 *
 * <p>For example, the layout of the {@link #inEdges} array is as follows:</p>
 *
 * <pre>
 * i,                         i + 1, i + 2, i + 3
 * ^^^^^^                     ^^^^^^^^^^^^^^^^^^^
 * the current in edge count    in edges (0, 1, 2)
 * </pre>
 *
 * <p>If a node has more than three in-edges, the remaining edges are stored in the
 * {@link #extraInEdges} array.</p>
 */
public class BytecodeCFG implements IndexedGraph<BytecodeBlock> {

    /**
     * [ count, edge1, edge2, edge3 ]
     */
    private static final int DEFAULT_EDGE_SIZE = 4;

    private final int[] inEdges;

    private final int[] outEdges;

    private final int[][] extraInEdges;

    private final int[][] extraOutEdges;

    private final int[] inEdgesCount;

    private final int[] outEdgesCount;

    private final int[][] exceptionInEdges;

    private final int[][] exceptionOutEdges;

    private final int[] exceptionOutEdgesCount;

    private final int[] exceptionInEdgesCount;

    private final BytecodeBlock entry;

    private final List<BytecodeBlock> sortedBlockList;

    /**
     * Mapping from instruction index to block.
     */
    private final BytecodeBlock[] idx2Block;

    /**
     * Constructs a new BytecodeCFG with the specified parameters.
     *
     * @param maxBlockSize    The maximum number of blocks in the graph.
     * @param entry           The entry block of the CFG.
     * @param sortedBlockList The list of blocks sorted in bytecode order.
     * @param idx2Block       Mapping from instruction index to block.
     */
    BytecodeCFG(int maxBlockSize, BytecodeBlock entry,
                List<BytecodeBlock> sortedBlockList, BytecodeBlock[] idx2Block) {
        this.entry = entry;
        this.sortedBlockList = sortedBlockList;
        this.idx2Block = idx2Block;

        inEdges = new int[maxBlockSize * DEFAULT_EDGE_SIZE];
        outEdges = new int[maxBlockSize * DEFAULT_EDGE_SIZE];
        extraInEdges = new int[maxBlockSize][];
        extraOutEdges = new int[maxBlockSize][];
        inEdgesCount = new int[maxBlockSize];
        outEdgesCount = new int[maxBlockSize];

        exceptionInEdges = new int[maxBlockSize][];
        exceptionOutEdges = new int[maxBlockSize][];
        exceptionOutEdgesCount = new int[maxBlockSize];
        exceptionInEdgesCount = new int[maxBlockSize];
    }

    /**
     * Searches for a valid block starting from the given instruction index.
     * Skips empty or merged blocks.
     *
     * @param startIdx the starting instruction index
     * @return the first valid block at or after startIdx
     */
    BytecodeBlock searchForValidBlock(int startIdx) {
        int idx = startIdx;
        while (idx2Block[idx] == null || idx2Block[idx].getIndex() == -1) {
            idx++;
        }
        return idx2Block[idx];
    }

    /**
     * add an edge (b1 -> b2)
     * @param b1 source block
     * @param b2 target block
     */
    void addEdge(int b1, int b2) {
        addInEdge(b2, b1);
        addOutEdge(b1, b2);
    }

    void addExceptionEdge(int b1, int b2) {
        addExtraRegionEdge(exceptionInEdges, exceptionInEdgesCount, b2, b1, 0);
        addExtraRegionEdge(exceptionOutEdges, exceptionOutEdgesCount, b1, b2, 0);
    }

    private void addInEdge(int b1, int b2) {
        addEdge(inEdges, extraInEdges, inEdgesCount, b1, b2);
    }

    private void addOutEdge(int b1, int b2) {
        addEdge(outEdges, extraOutEdges, outEdgesCount, b1, b2);
    }

    private void addEdge(int[] normalRegion, int[][] extraRegion, int[] count, int b1, int b2) {
        if (count[b1] < DEFAULT_EDGE_SIZE) {
            addNormalRegionEdge(normalRegion, count, b1, b2);
        } else {
            addExtraRegionEdge(extraRegion, count, b1, b2, DEFAULT_EDGE_SIZE);
        }
    }

    /**
     * add an edge (b1 -> b2) to normal region
     */
    private void addNormalRegionEdge(int[] normalRegion, int[] count, int b1, int b2) {
        assert count[b1] < DEFAULT_EDGE_SIZE;
        int index = b1 * DEFAULT_EDGE_SIZE + count[b1];
        normalRegion[index] = b2;
        count[b1]++;
    }

    /**
     * add an edge (b1 -> b2) to extra region
     */
    private void addExtraRegionEdge(int[][] extraRegion, int[] count, int b1, int b2, int defaultEdgeSize) {
        assert count[b1] >= defaultEdgeSize;
        int nextIndex = count[b1] - defaultEdgeSize;
        if (extraRegion[b1] == null) {
            extraRegion[b1] = new int[DEFAULT_EDGE_SIZE];
        } else if (nextIndex >= extraRegion[b1].length) {
            int[] newRegion = new int[extraRegion[b1].length * 2];
            System.arraycopy(extraRegion[b1], 0, newRegion, 0, extraRegion[b1].length);
            extraRegion[b1] = newRegion;
        }
        extraRegion[b1][nextIndex] = b2;
        count[b1]++;
    }

    int getOutEdgesCount(int b) {
        return outEdgesCount[b];
    }

    int getInEdgesCount(int b) {
        return inEdgesCount[b];
    }

    List<BytecodeBlock> getSortedBlockList() {
        return sortedBlockList;
    }

    int getInEdge(int b, int index) {
        if (index < DEFAULT_EDGE_SIZE) {
            return inEdges[b * DEFAULT_EDGE_SIZE + index];
        } else {
            return extraInEdges[b][index - DEFAULT_EDGE_SIZE];
        }
    }

    int getOutEdge(int b, int index) {
        assert b >= 0;
        if (index < DEFAULT_EDGE_SIZE) {
            return outEdges[b * DEFAULT_EDGE_SIZE + index];
        } else {
            return extraOutEdges[b][index - DEFAULT_EDGE_SIZE];
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
    public List<BytecodeBlock> inEdges(BytecodeBlock node) {
        List<BytecodeBlock> r = new ArrayList<>();
        int b = node.getIndex();
        for (int i = 0; i < getMergedInEdgesCount(b); ++i) {
            r.add(getNode(getMergedInEdge(b, i)));
        }
        return r;
    }

    @Override
    public List<BytecodeBlock> outEdges(BytecodeBlock node) {
        List<BytecodeBlock> r = new ArrayList<>();
        int b = node.getIndex();
        for (int i = 0; i < getMergedOutEdgesCount(b); ++i) {
            r.add(getNode(getMergedOutEdge(b, i)));
        }
        return r;
    }

    @Override
    public List<BytecodeBlock> normalOutEdges(BytecodeBlock node) {
        List<BytecodeBlock> r = new ArrayList<>();
        int b = node.getIndex();
        for (int i = 0; i < outEdgesCount[b]; i++) {
            r.add(getNode(getOutEdge(b, i)));
        }
        return r;
    }

    @Override
    public BytecodeBlock getNode(int index) {
        return sortedBlockList.get(index);
    }

    @Override
    public int getIndex(BytecodeBlock node) {
        return node.getIndex();
    }

    @Override
    public int size() {
        return sortedBlockList.size();
    }

    @Override
    public int getMergedInEdgesCount(int node) {
        return inEdgesCount[node] + exceptionInEdgesCount[node];
    }

    @Override
    public int getMergedOutEdgesCount(int node) {
        return outEdgesCount[node] + exceptionOutEdgesCount[node];
    }

    @Override
    public int getMergedInEdge(int node, int index) {
        if (index < inEdgesCount[node]) {
            return getInEdge(node, index);
        } else {
            return exceptionInEdges[node][index - inEdgesCount[node]];
        }
    }

    @Override
    public int getMergedOutEdge(int node, int index) {
        if (index < outEdgesCount[node]) {
            return getOutEdge(node, index);
        } else {
            return exceptionOutEdges[node][index - outEdgesCount[node]];
        }
    }

    // ==================== Block-level convenience methods ====================

    /**
     * Returns the number of outgoing edges for the given block.
     */
    int getOutEdgesCount(BytecodeBlock block) {
        return outEdgesCount[block.getIndex()];
    }

    /**
     * Returns the number of incoming edges for the given block.
     */
    int getInEdgesCount(BytecodeBlock block) {
        return inEdgesCount[block.getIndex()];
    }

    /**
     * Returns the number of merged outgoing edges (normal + exception) for the given block.
     */
    public int getMergedOutEdgesCount(BytecodeBlock block) {
        return getMergedOutEdgesCount(block.getIndex());
    }

    /**
     * Returns the target block of the outgoing edge at the given index.
     */
    BytecodeBlock getOutEdge(BytecodeBlock block, int index) {
        int targetIndex = getOutEdge(block.getIndex(), index);
        return sortedBlockList.get(targetIndex);
    }

    /**
     * Returns the source block of the incoming edge at the given index.
     */
    BytecodeBlock getInEdge(BytecodeBlock block, int index) {
        int sourceIndex = getInEdge(block.getIndex(), index);
        return sortedBlockList.get(sourceIndex);
    }

    /**
     * Returns the target block of the merged outgoing edge at the given index.
     */
    public BytecodeBlock getMergedOutEdge(BytecodeBlock block, int index) {
        int targetIndex = getMergedOutEdge(block.getIndex(), index);
        return sortedBlockList.get(targetIndex);
    }

    /**
     * Checks if the block has no incoming edges.
     * Note: catch blocks are treated as having no incoming edges for control flow purposes.
     */
    public boolean isInEdgeEmpty(BytecodeBlock block) {
        return block.isCatch() || inEdgesCount[block.getIndex()] == 0;
    }

    /**
     * Checks if the block has no outgoing edges.
     */
    boolean isOutEdgeEmpty(BytecodeBlock block) {
        return outEdgesCount[block.getIndex()] == 0;
    }
}
