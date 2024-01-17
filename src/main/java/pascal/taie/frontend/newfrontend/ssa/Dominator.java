package pascal.taie.frontend.newfrontend.ssa;

import pascal.taie.frontend.newfrontend.SparseSet;

import java.util.Arrays;
import java.util.List;

/**
 * <p> This class implements "A Simple, Fast Dominance Algorithm"
 * by Keith D. Cooper, Timothy J. Harvey, and Ken Kennedy.
 * </p>
 *
 * <p> See <a href="https://web.cse.ohio-state.edu/~rountev.1/788/papers/cooper-spe01.pdf">The paper</a>
 * for the details.
 * </p>
 */
public class Dominator<N> {
    private final IndexedGraph<N> graph;

    private final int[] postIndex;

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
    private final int[] postOrder;

    private int[] dom;

    private int[] height;

    private int entry;

    public static final int UNDEFINED = -1;

    public Dominator(IndexedGraph<N> graph) {
        this.graph = graph;
        this.entry = graph.getIntEntry();
        postIndex = new int[graph.size()];
        postOrder = new int[graph.size()];
    }

    public record DominatorFrontiers(SparseSet[] res) {
        public SparseSet get(int index) {
            return res[index];
        }
    }

    // TODO: add a method to get explicit dominator tree

    /**
     * Get the idom[] array. This array is indexed by the node index.
     * The value of idom[i] is the index of the immediate dominator of node i.
     * @return the idom[] array
     */
    public int[] getDomTree() {
        if (dom == null) {
            boolean changed = true;
            dom = new int[graph.size()];
            Arrays.fill(dom, UNDEFINED);

            // TODO: can/need we calculates idom in dfs?
            dfsTrav();
            int entry = graph.getIndex(graph.getEntry());
            dom[entry] = entry;
            while (changed) {
                changed = loopTrav(dom);
            }
        }

        return dom;
    }

    public int[] getPostOrder() {
        return postOrder;
    }

    /**
     * Get the dominator frontiers.
     * @return the dominator frontiers
     */
    public DominatorFrontiers getDF() {
        int[] dom = getDomTree();
        // TODO: it seems that sparse set allocation consumes a considerable time,
        //       can we optimize it?
        SparseSet[] df = new SparseSet[graph.size()];
        for (int i = 0; i < graph.size(); ++i) {
            df[i] = new SparseSet(graph.size(), graph.size());
        }
        for (int i = 0; i < graph.size(); ++i) {
            int size = graph.getMergedInEdgesCount(i);
            if (size >= 2) {
                for (int j = 0; j < size; ++j) {
                    int runner = graph.getMergedInEdge(i, j);
                    while (runner != dom[i]) {
                        assert runner != -1;
                        df[runner].add(i);
                        runner = dom[runner];
                    }
                }
            }
        }
        return new DominatorFrontiers(df);
    }

    public int[] assignDomTreeHeight() {
        if (height == null) {
            int[] dom = getDomTree();
            height = new int[graph.size()];
            Arrays.fill(height, -1);
            for (int i = 0; i < graph.size(); ++i) {
                climbDomTree(i, height);
            }
        }
        return height;
    }

    private void climbDomTree(int runner, int[] height) {
        if (height[runner] != -1) {
            return;
        } else if (dom[runner] == runner) {
            height[runner] = 0;
            return;
        } else if (height[dom[runner]] != -1) {
            height[runner] = height[dom[runner]] + 1;
            return;
        } else {
            climbDomTree(dom[runner], height);
            height[runner] = height[dom[runner]] + 1;
        }
    }

    private void dfsTrav() {
        boolean[] visited = new boolean[graph.size()];
        dfs(entry, visited);
    }

    int post;
    private void dfs(int node, boolean[] visited) {
        visited[node] = true;
        for (int i = graph.getMergedOutEdgesCount(node) - 1; i >= 0; i--) {
            int succ = graph.getMergedOutEdge(node, i);
            if (!visited[succ]) {
                dfs(succ, visited);
            }
        }
        int currentPost = post++;
        postOrder[currentPost] = node;
        postIndex[node] = currentPost;
    }

    private boolean loopTrav(int[] dom) {
        boolean changed = false;
        // first set entry node's idom to itself
        dom[entry] = entry;
        // reverse post order
        for (int i = graph.size() - 1; i >= 0; --i) {
            int node = postOrder[i];
            changed |= processNode(dom, node);
        }
        return changed;
    }

    boolean processNode(int[] dom, int node) {
        int prevCount = graph.getMergedInEdgesCount(node);
        if (prevCount == 0 || dom[node] == entry) {
            return false;
        }
        int newIdom = UNDEFINED;
        for (int i = 0; i < prevCount; ++i) {
            int p = graph.getMergedInEdge(node, i);
            if (dom[p] == UNDEFINED) {
                continue;
            }
            if (newIdom == UNDEFINED) {
                newIdom = p;
                continue;
            }
            newIdom = intersect(dom, p, newIdom);
        }
        if (dom[node] != newIdom) {
            dom[node] = newIdom;
            return true;
        }
        return false;
    }

    int intersect(int[] dom, int p, int newIdom) {
        int finger1 = p;
        int finger2 = newIdom;
        while (finger1 != finger2) {
            while (postIndex[finger1] < postIndex[finger2]) {
                finger1 = dom[finger1];
                assert finger1 != UNDEFINED;
            }
            while (postIndex[finger2] < postIndex[finger1]) {
                finger2 = dom[finger2];
                assert finger2 != UNDEFINED;
            }
        }
        return finger1;
    }
}
