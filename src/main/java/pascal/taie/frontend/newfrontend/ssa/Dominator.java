package pascal.taie.frontend.newfrontend.ssa;

import pascal.taie.frontend.newfrontend.SparseSet;

import java.util.Arrays;
import java.util.List;

/**
 * <p>This class implements "A Simple, Fast Dominance Algorithm"
 * by Keith D. Cooper, Timothy J. Harvey, and Ken Kennedy.
 * </p>
 *
 * See <a href="https://web.cse.ohio-state.edu/~rountev.1/788/papers/cooper-spe01.pdf">The paper</a>
 * for the details.
 */
public class Dominator {
    private final IndexedGraph<?> graph;

    private final int[] postIndex;

    private final int[] postOrder;

    public final static int UNDEFINED = -1;

    public Dominator(IndexedGraph<?> graph) {
        this.graph = graph;
        postIndex = new int[graph.size()];
        postOrder = new int[graph.size()];
    }

    public record DominatorFrontiers(SparseSet[] res) {
        public SparseSet get(int index) {
            return res[index];
        }
    }

    public int[] getDomTree() {
        boolean changed = true;
        int[] dom = new int[graph.size()];
        Arrays.fill(dom, UNDEFINED);

        // TODO: can/need we calculates idom in dfs?
        dfsTrav();
        dom[graph.getEntry()] = graph.getEntry();
        while (changed) {
            changed = loopTrav(dom);
        }

        return dom;
    }


    public DominatorFrontiers getDF() {
        int[] dom = getDomTree();
        SparseSet[] df = new SparseSet[graph.size()];
        for (int i = 0; i < graph.size(); ++i) {
            df[i] = new SparseSet(graph.size(), graph.size());
        }
        for (int i = 0; i < graph.size(); ++i) {
            if (graph.inEdges(i).size() >= 2) {
                for (int p : graph.inEdges(i)) {
                    int runner = p;
                    while (runner != dom[i]) {
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
    private void dfs(int node, boolean[] visited) {
        visited[node] = true;
        for (int succ : graph.outEdges(node)) {
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
        // reverse post order
        for (int i = graph.size() - 1; i >= 0; --i) {
            int node = postOrder[i];
            changed |= processNode(dom, node);
        }
        return changed;
    }

    boolean processNode(int[] dom, int node) {
        List<Integer> pred = graph.inEdges(node);
        if (pred.isEmpty()) {
            return false;
        }
        int newIdom = UNDEFINED;
        for (int p : pred) {
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
