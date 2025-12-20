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

package pascal.taie.util.graph;

import pascal.taie.util.MutableInt;
import pascal.taie.util.collection.IntList;
import pascal.taie.util.collection.LazyArray;
import pascal.taie.util.collection.SparseIntSet;

import java.util.Arrays;
import java.util.Set;

/**
 * Computes dominance information for a graph.
 *
 * <p>This class implements "A Simple, Fast Dominance Algorithm"
 * by Keith D. Cooper, Timothy J. Harvey, and Ken Kennedy (2001).
 *
 * @param <N> the type of nodes in the graph
 */
public class Dominators<N> {

    /**
     * Represents an undefined node index, used to indicate
     * that a node's dominator has not been computed yet.
     */
    private static final int UNDEFINED = -1;

    /**
     * The graph on which dominance analysis is performed.
     */
    private final IndexedGraph<N> graph;

    /**
     * The index of the entry node in the graph.
     */
    private final int entryI;

    /**
     * <p>The (semi, or partial) post order of the graph.
     * The value of {@code postOrder[i]} is the node index of the ith node in the post order.
     * </p>
     *
     * <p>It can be proved that, if the {@code postOrder} array is constructed by dfs
     * (as implemented here), then such array satisfies the following property:
     * </p>
     *
     * <p>for all {@code i}, there're {@code j}, s.t.
     * <ol>
     *     <li>{@code j >= i}</li>
     *     <li>{@code postOrder[j] `idom` postOrder[i]}</li>
     * </ol>
     * </p>
     *
     * <p>So, if we traverse the graph in reverse post order, for any block,
     * its immediate dominator must have been visited.</p>
     */
    private int[] postOrder;

    /**
     * Reverse mapping of postOrder.
     * {@code postIndex[nodeIndex]} gives the position of the node in the post order sequence.
     */
    private final int[] postIndex;

    /**
     * iDoms[i] is the index of the immediate dominator of node i.
     */
    private final int[] iDom;

    /**
     * Entry timestamp for each node in the dominator tree DFS traversal.
     * Used together with {@link #outClock} to determine dominance relationships in O(1) time.
     */
    private final int[] inClock;

    /**
     * Exit timestamp for each node in the dominator tree DFS traversal.
     * Used together with {@link #inClock} to determine dominance relationships in O(1) time.
     */
    private final int[] outClock;

    /**
     * Pre-order traversal sequence of the dominator tree.
     * {@code domTreePreOrder[i]} is the node index of the i-th node visited in pre-order.
     */
    private final int[] domTreePreOrder;

    /**
     * Constructs a Dominator analysis for the given indexed graph.
     * This constructor computes the post-order, immediate dominators,
     * and dominator tree clock values.
     *
     * @param graph the indexed graph to analyze
     */
    public Dominators(IndexedGraph<N> graph) {
        this.graph = graph;
        this.entryI = graph.getIndex(graph.getEntry());

        postIndex = new int[graph.nodeCount()];
        computePostOrder();

        iDom = new int[graph.nodeCount()];
        computeIDom();

        domTreePreOrder = new int[graph.nodeCount()];
        inClock = new int[graph.nodeCount()];
        outClock = new int[graph.nodeCount()];
        computeDomTreeClock();
    }

    private void computePostOrder() {
        postOrder = new int[graph.nodeCount()];
        Arrays.fill(postOrder, UNDEFINED);
        boolean[] visited = new boolean[graph.nodeCount()];
        // counter for recording post order
        MutableInt counter = new MutableInt(0);
        dfsGraph(entryI, visited, counter);
        if (counter.intValue() != graph.nodeCount()) {
            postOrder = Arrays.copyOf(postOrder, counter.intValue());
        }
    }

    private void dfsGraph(int node, boolean[] visited, MutableInt counter) {
        visited[node] = true;
        for (N succ : graph.getSuccsOf(graph.getObject(node))) {
            int succI = graph.getIndex(succ);
            if (!visited[succI]) {
                dfsGraph(succI, visited, counter);
            }
        }
        int currentPost = counter.intValue();
        counter.add(1);
        postOrder[currentPost] = node;
        postIndex[node] = currentPost;
    }

    private void computeIDom() {
        Arrays.fill(iDom, UNDEFINED);
        // first set entry node's iDom to itself
        iDom[entryI] = entryI;
        boolean changed = true;
        while (changed) {
            changed = processNodes();
        }
    }

    private boolean processNodes() {
        // iterate nodes in reverse post order
        boolean changed = false;
        for (int i = postOrder.length - 1; i >= 0; --i) {
            int node = postOrder[i];
            changed |= processNode(node);
        }
        return changed;
    }

    private boolean processNode(int node) {
        Set<N> preds = graph.getPredsOf(graph.getObject(node));
        if (preds.isEmpty() || iDom[node] == entryI) {
            return false;
        }
        int newIDom = UNDEFINED;
        for (N pred : preds) {
            int predI = graph.getIndex(pred);
            if (iDom[predI] == UNDEFINED) {
                continue;
            }
            if (newIDom == UNDEFINED) {
                newIDom = predI;
                continue;
            }
            newIDom = intersect(predI, newIDom);
        }
        if (iDom[node] != newIDom) {
            iDom[node] = newIDom;
            return true;
        }
        return false;
    }

    private int intersect(int pred, int newIDom) {
        int finger1 = pred;
        int finger2 = newIDom;
        while (finger1 != finger2) {
            while (postIndex[finger1] < postIndex[finger2]) {
                finger1 = iDom[finger1];
                assert finger1 != UNDEFINED;
            }
            while (postIndex[finger2] < postIndex[finger1]) {
                finger2 = iDom[finger2];
                assert finger2 != UNDEFINED;
            }
        }
        return finger1;
    }

    private void computeDomTreeClock() {
        IntTree domTree = new IntTree(graph.nodeCount());
        for (int i = 0; i < graph.nodeCount(); ++i) {
            if (iDom[i] != i // avoid self reference
                    && iDom[i] != UNDEFINED) { // avoid unreachable node
                domTree.addEdge(iDom[i], i);
            }
        }
        dfsDomTree(domTree, entryI, new MutableInt(0), new MutableInt(0));
    }

    private void dfsDomTree(IntTree domTree, int node,
                            MutableInt counter, MutableInt clock) {
        domTreePreOrder[counter.intValue()] = node;
        counter.add(1);
        inClock[node] = clock.intValue();
        clock.add(1);
        if (domTree.contains(node)) {
            IntList out = domTree.get(node);
            for (int i = 0; i < out.size(); ++i) {
                int succ = out.get(i);
                dfsDomTree(domTree, succ, counter, clock);
            }
        }
        outClock[node] = clock.intValue();
        clock.add(1);
    }

    /**
     * A tree represented as an adjacency list.
     */
    private static class IntTree extends LazyArray<IntList> {

        IntTree(int initialCapacity) {
            super(initialCapacity);
        }

        @Override
        protected IntList createElement() {
            return new IntList(4);
        }

        /**
         * Add an edge from {@code source} to {@code target}.
         */
        private void addEdge(int source, int target) {
            get(source).add(target);
        }
    }

    /**
     * @param a a node index
     * @param b a node index
     * @return {@code true} if a dominates b
     */
    public boolean dominates(int a, int b) {
        return inClock[a] <= inClock[b] && outClock[a] >= outClock[b];
    }

    /**
     * Returns the post-order traversal sequence of the graph.
     *
     * @return an array where {@code postOrder[i]} is the node index of the i-th node
     *         visited in post-order DFS traversal
     */
    public int[] getPostOrder() {
        return postOrder;
    }

    public int[] getDomTreePreOrder() {
        return domTreePreOrder;
    }

    /**
     * @return the idom[] array. The value of iDom[i]
     * is the index of the immediate dominator of node i.
     */
    int[] getIDom() {
        return iDom;
    }

    public record DominatorFrontiers(LazyArray<SparseIntSet> res) {
        public SparseIntSet get(int index) {
            return res.get(index);
        }
    }

    /**
     * @return the dominator frontiers
     */
    public DominatorFrontiers getDomFront() {
        // TODO: it seems that sparse set allocation consumes a considerable time,
        //       can we optimize it?
        int size = graph.nodeCount();
        LazyArray<SparseIntSet> df = new LazyArray<>(size) {
            @Override
            protected SparseIntSet createElement() {
                return new SparseIntSet(size);
            }
        };
        for (N node : graph) {
            Set<N> preds = graph.getPredsOf(node);
            int nodeI = graph.getIndex(node);
            if (preds.size() >= 2 || nodeI == entryI) {
                // nodeI == entry for that we do not have pseudo entry,
                // so we have to force dominator frontier calculator
                // to calculate df for the actual entry.
                for (N pred : preds) {
                    int runner = graph.getIndex(pred);
                    while (runner != iDom[nodeI] && runner >= 0) {
                        df.get(runner).add(nodeI);
                        runner = iDom[runner];
                    }
                }
            }
        }
        return new DominatorFrontiers(df);
    }
}
