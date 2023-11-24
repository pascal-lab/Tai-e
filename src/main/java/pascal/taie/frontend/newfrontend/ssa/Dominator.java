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

    private final int[] postOrder;

    private int[] dom;

    public final static int UNDEFINED = -1;

    public Dominator(IndexedGraph<N> graph) {
        this.graph = graph;
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
            N node = graph.getNode(i);
            if (graph.inEdges(node).size() >= 2) {
                for (N p : graph.inEdges(node)) {
                    int runner = graph.getIndex(p);
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

    private void dfsTrav() {
        boolean[] visited = new boolean[graph.size()];
        dfs(graph.getEntry(), visited);
    }

    int post;
    private void dfs(N node, boolean[] visited) {
        int idx = graph.getIndex(node);
        visited[idx] = true;
        for (N succ : graph.outEdges(node)) {
            if (!visited[graph.getIndex(succ)]) {
                dfs(succ, visited);
            }
        }
        int currentPost = post++;
        postOrder[currentPost] = idx;
        postIndex[idx] = currentPost;
    }

    private boolean loopTrav(int[] dom) {
        boolean changed = false;
        // reverse post order
        for (int i = graph.size() - 1; i >= 0; --i) {
            int node = postOrder[i];
            changed |= processNode(dom, graph.getNode(node));
        }
        return changed;
    }

    boolean processNode(int[] dom, N node) {
        List<N> pred = graph.inEdges(node);
        if (pred.isEmpty()) {
            return false;
        }
        int newIdom = UNDEFINED;
        for (N pn : pred) {
            int p = graph.getIndex(pn);
            if (dom[p] == UNDEFINED) {
                continue;
            }
            if (newIdom == UNDEFINED) {
                newIdom = p;
                continue;
            }
            newIdom = intersect(dom, p, newIdom);
        }
        if (dom[graph.getIndex(node)] != newIdom) {
            dom[graph.getIndex(node)] = newIdom;
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
