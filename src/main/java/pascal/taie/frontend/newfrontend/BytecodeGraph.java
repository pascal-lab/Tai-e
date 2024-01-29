package pascal.taie.frontend.newfrontend;

import pascal.taie.frontend.newfrontend.ssa.IndexedGraph;

import java.util.ArrayList;
import java.util.List;

public class BytecodeGraph implements IndexedGraph<BytecodeBlock> {

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

    private final static int DEFAULT_EDGE_SIZE = 4;

    BytecodeBlock entry;

    List<BytecodeBlock> blockSortedList;

    BytecodeGraph(int maxBlockSize) {
        // [ count, edge1, edge2, edge3 ]
        int defaultSize = 4;
        inEdges = new int[maxBlockSize * defaultSize];
        outEdges = new int[maxBlockSize * defaultSize];
        extraInEdges = new int[maxBlockSize][];
        extraOutEdges = new int[maxBlockSize][];
        inEdgesCount = new int[maxBlockSize];
        outEdgesCount = new int[maxBlockSize];

        exceptionInEdges = new int[maxBlockSize][];
        exceptionOutEdges = new int[maxBlockSize][];
        exceptionOutEdgesCount = new int[maxBlockSize];
        exceptionInEdgesCount = new int[maxBlockSize];
    }

    void setEntry(BytecodeBlock entry) {
        this.entry = entry;
    }

    void setBlockSortedList(List<BytecodeBlock> blockSortedList) {
        this.blockSortedList = blockSortedList;
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

    public int getOutEdgesCount(int b) {
        return outEdgesCount[b];
    }

    public int getInEdgesCount(int b) {
        return inEdgesCount[b];
    }

    public List<BytecodeBlock> getBlockSortedList() {
        return blockSortedList;
    }

    public int getInEdge(int b, int index) {
        if (index < DEFAULT_EDGE_SIZE) {
            return inEdges[b * DEFAULT_EDGE_SIZE + index];
        } else {
            return extraInEdges[b][index - DEFAULT_EDGE_SIZE];
        }
    }

    public int getOutEdge(int b, int index) {
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
     * Only for debug propose
     * <br/>
     * Performance will be very bad
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
        return blockSortedList.get(index);
    }

    @Override
    public int getIndex(BytecodeBlock node) {
        return node.getIndex();
    }

    @Override
    public int size() {
        return blockSortedList.size();
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
}
