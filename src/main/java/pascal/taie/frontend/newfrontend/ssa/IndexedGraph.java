package pascal.taie.frontend.newfrontend.ssa;

import java.util.List;

/**
 * <p>A graph with indexed nodes.</p>
 * <p>This graph must an entry node.
 * (i.e. for every node n, there is a path from the entry node to n)
 * Because we're handling CFGs, so there may be multiple entry nodes.
 * To fix this, the implementation should insert a new entry node pointing to all entry nodes
 * </p>
 */
public interface IndexedGraph <N> {
    List<N> inEdges(N node);

    List<N> outEdges(N node);

    List<N> normalOutEdges(N node);

    default int getMergedInEdgesCount(int node) {
        return inEdges(getNode(node)).size();
    }

    default int getMergedOutEdgesCount(int node) {
        return outEdges(getNode(node)).size();
    }

    default int getMergedInEdge(int node, int index) {
        return getIndex(inEdges(getNode(node)).get(index));
    }

    default int getMergedOutEdge(int node, int index) {
        return getIndex(outEdges(getNode(node)).get(index));
    }

    N getNode(int index);

    int getIndex(N node);

    int size();

    N getEntry();

    default int getIntEntry() {
        return getIndex(getEntry());
    }
}
