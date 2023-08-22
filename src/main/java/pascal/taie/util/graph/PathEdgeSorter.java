package pascal.taie.util.graph;

import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;

import java.util.*;

public class PathEdgeSorter<N> {

    private final Graph<N> graph;
    private final N source;
    private final N target;
    private final Map<Edge<N>, Integer> edge2PathCount = Maps.newMap();
    private final List<EdgeRecord<N>> result = new ArrayList<>();
    private final Set<N> canReachTarget;
    private final Set<N> visited;
    private final Set<List<Edge<N>>> paths;

    public PathEdgeSorter(Graph<N> graph, N source, N target) {
        this.graph = graph;
        this.source = source;
        this.target = target;
        Reachability<N> reachability = new Reachability<>(graph);
        canReachTarget = reachability.nodesCanReach(target);
        visited = Sets.newSet();
        paths = Sets.newSet();
        graph.forEach(node -> graph.getOutEdgesOf(node).forEach(nEdge -> edge2PathCount.put(nEdge, 0)));
        compute();
    }

    private void computePaths(Edge<N> currEdge, List<Edge<N>> path) {
        N currNode = currEdge.target();
        if (visited.add(currNode)) {
            path.add(currEdge);
            if (currNode == target) {
                paths.add(new ArrayList<>(path));
            } else {
                graph.getOutEdgesOf(target).forEach(edge -> {
                    if (!visited.contains(edge.target()) && canReachTarget.contains(edge.target())) {
                        computePaths(edge, path);
                    }
                });
            }
            path.remove(currEdge);
            visited.remove(currNode);
        }

    }

    private void computePathsFrom(N node) {
        graph.getOutEdgesOf(node).forEach(nEdge -> computePaths(nEdge, new ArrayList<>()));
    }

    private void compute() {
        computePathsFrom(source);
        paths.stream().
                flatMap(Collection::stream).
                forEach(nEdge -> edge2PathCount.put(nEdge, edge2PathCount.get(nEdge) + 1));
        edge2PathCount.keySet().forEach(nEdge -> result.add(new EdgeRecord<>(edge2PathCount.get(nEdge), nEdge)));
        Collections.sort(result);
    }

    public List<EdgeRecord<N>> getResult() {
        if (result.isEmpty()) {
            return List.of();
        }
        return result;
    }

    public Edge<N> getEdgeWithMaxPathCount() {
        assert !result.isEmpty();
        return result.get(0).edge;
    }

    public record EdgeRecord<N>(Integer count, Edge<N> edge) implements Comparable<EdgeRecord<N>> {
        @Override
        public int compareTo(EdgeRecord<N> other) {
            return this.count - other.count;
        }
    }
}
