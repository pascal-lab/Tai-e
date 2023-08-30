package pascal.taie.util.graph;

import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;

import java.util.*;
import java.util.function.ToIntFunction;

/**
 * Computes shortest paths from a single source node to other nodes in a graph
 * using Dijkstra's algorithm or Dial's algorithm.
 *
 * @param <N> type of nodes
 * @param <E> type of edges
 */

public class ShortestPath<N, E extends Edge<N>> {

    public static final int INVALID_WEIGHT = Integer.MAX_VALUE;

    private final Graph<N> graph;

    private final N source;

    private final ToIntFunction<E> weightCalc;

    private final ToIntFunction<E> costCalc;

    private Map<N, Integer> node2PathDistance;

    private Map<N, E> node2PathPredecessor;

    private Map<N, Integer> node2PathCost;

    public ShortestPath(Graph<N> graph,
                        N source,
                        ToIntFunction<E> weightCalc,
                        ToIntFunction<E> costCalc) {
        this.graph = graph;
        this.source = source;
        this.weightCalc = weightCalc;
        this.costCalc = costCalc;
    }

    /**
     * Compute the shortest paths from source node to other nodes
     */
    public void compute() {
        if (node2PathDistance == null) {
            node2PathDistance = Maps.newMap();
            node2PathPredecessor = Maps.newMap();
            node2PathCost = Maps.newMap();
            runDijkstra();
        }
    }

    private void runDijkstra() {
        // Initialize
        PriorityQueue<DistPair> queue = new PriorityQueue<>(graph.getNumberOfNodes());
        Set<N> finished = Sets.newSet(graph.getNumberOfNodes());
        graph.forEach(node -> node2PathPredecessor.put(node, null));
        graph.forEach(node -> node2PathDistance.put(node, INVALID_WEIGHT));
        graph.forEach(node -> node2PathCost.put(node, 0));

        // Set source node status
        node2PathDistance.put(source, 0);
        node2PathCost.put(source, 0);
        queue.add(new DistPair(source, 0, 0));

        // Main loop
        while (!queue.isEmpty()) {
            DistPair distPair = queue.poll();
            N minDistNode = distPair.node;
            int minDist = distPair.dist;
            if (finished.add(minDistNode)) {
                for (Edge<N> edge : graph.getOutEdgesOf(minDistNode)) {
                    E outEdge = (E) edge;
                    N successor = outEdge.target();
                    if (!finished.contains(successor)) {
                        int newDist = minDist + weightCalc.applyAsInt(outEdge);
                        int newCost = node2PathCost.get(minDistNode) + costCalc.applyAsInt(outEdge);
                        if (newDist < node2PathDistance.get(successor)) {
                            node2PathDistance.put(successor, newDist);
                            node2PathPredecessor.put(successor, outEdge);
                            node2PathCost.put(successor, newCost);
                            queue.add(new DistPair(successor, newDist, newCost));
                        }
                        else if(newDist == node2PathDistance.get(successor) && newCost < node2PathCost.get(successor))
                        {
                            node2PathPredecessor.put(successor, outEdge);
                            node2PathCost.put(successor, newCost);
                            queue.add(new DistPair(successor, newDist, newCost));
                        }
                    }
                }
            }
        }
    }

    /**
     * Get the shortest path from source node to target node.
     * If there is no such path, it will return an empty list.
     *
     * @param target the target node
     * @return the shortest path from source node to target node
     */
    public List<N> getPathNode(N target) {
        if (node2PathDistance.get(target) == INVALID_WEIGHT || node2PathPredecessor.get(target) == null) {
            return List.of();
        }
        List<N> path = new ArrayList<>();
        N curr = target;
        while (curr != null) {
            path.add(curr);
            curr = node2PathPredecessor.get(curr).source();
        }
        Collections.reverse(path);
        return path;
    }

    public List<E> getPath(N target) {
        if (node2PathDistance.get(target) == INVALID_WEIGHT) {
            return List.of();
        }
        List<E> path = new ArrayList<>();
        E curr = node2PathPredecessor.get(target);
        while (curr != null) {
            path.add(curr);
            curr = node2PathPredecessor.get(curr.source());
        }
        Collections.reverse(path);
        return path;
    }

    /**
     * @param node the target node
     * @return the distance from source node to target node
     */
    public int getDistance(N node) {
        return node2PathDistance.get(node);
    }

    private class DistPair implements Comparable<DistPair> {
        private final N node;

        private final int dist;

        private final int cost;

        public DistPair(N node, int dist, int cost) {
            this.node = node;
            this.dist = dist;
            this.cost = cost;
        }

        @Override
        public int compareTo(DistPair other) {
            if(this.dist == other.dist){
                return Integer.compare(this.cost, other.cost);
            }
            return Integer.compare(this.dist, other.dist);
        }
    }
}

