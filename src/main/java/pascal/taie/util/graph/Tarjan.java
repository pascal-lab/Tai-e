package pascal.taie.util.graph;

import pascal.taie.util.collection.Sets;

import java.util.Set;

public class Tarjan<N> {

    private final Graph<N> graph;
    private final N source;
    private Set<Edge<N>> result;


    public Tarjan(Graph<N> graph, N source) {
        this.graph = graph;
        this.source = source;
        result = Sets.newSet();
    }

    public Set<Edge<N>> getResult() {
        if (result.isEmpty()) {
            return Set.of();
        }
        return result;
    }

    public void compute() {

    }

}
