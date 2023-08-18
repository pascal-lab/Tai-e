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
 */

public class ShortestPath<N> {

    private static final int INVALID_WEIGHT = Integer.MAX_VALUE;

    private final Graph<N> graph;

    private final N source;

    private final ToIntFunction<Edge<N>> weightCalc;

    private final ToIntFunction<Edge<N>> costCalc;

    private Map<N, Integer> node2PathDistance;

    private Map<N, Edge<N>> node2PathPredecessor;

    private Map<N, Integer> node2PathCost;

    public ShortestPath(Graph<N> graph,
                        N source,
                        ToIntFunction<Edge<N>> weightCalc,
                        ToIntFunction<Edge<N>> costCalc) {
        this.graph = graph;
        this.source = source;
        this.weightCalc = weightCalc;
        this.costCalc = costCalc;
    }

    /**
     * Compute the shortest paths from source node to other nodes
     *
     * @param algorithm algorithm to compute the shortest path
     */
    public void compute(SSSPAlgorithm algorithm) {
        if (node2PathDistance == null) {
            node2PathDistance = Maps.newMap();
            node2PathPredecessor = Maps.newMap();
            node2PathCost = Maps.newMap();
            switch (algorithm) {
                case DIJKSTRA -> runDijkstra();
                case DIAL -> runDial();
                default -> throw new UnsupportedOperationException("No such SSSP algorithm");
            }
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
                for (Edge<N> outEdge : graph.getOutEdgesOf(minDistNode)) {
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

    private void runDial() {
        // Initialize
        Set<N> finished = Sets.newSet(graph.getNumberOfNodes());
        graph.forEach(node -> node2PathPredecessor.put(node, null));
        graph.forEach(node -> node2PathDistance.put(node, INVALID_WEIGHT));
        TreeMap<Integer, Set<N>> bucket = new TreeMap<>();

        // Set source node status
        bucket.put(0, Sets.newSet());
        bucket.get(0).add(source);
        node2PathDistance.put(source, 0);

        // Main loop
        while (!bucket.isEmpty()) {
            Iterator<Integer> iter = bucket.keySet().iterator();
            int minDist = iter.next();
            Iterator<N> iterator = bucket.get(minDist).iterator();
            if (!iterator.hasNext()) {
                iter.remove();
                continue;
            }
            while (iterator.hasNext()) {
                N node = iterator.next();
                if (finished.add(node)) {
                    for (Edge<N> outEdge : graph.getOutEdgesOf(node)) {
                        N neighbor = outEdge.target();
                        if (finished.contains(neighbor)) {
                            continue;
                        }
                        int newDist = node2PathDistance.get(node) + weightCalc.applyAsInt(outEdge);
                        if (newDist < node2PathDistance.get(neighbor)) {
                            node2PathDistance.put(neighbor, newDist);
                            bucket.computeIfAbsent(newDist, k -> Sets.newSet());
                            bucket.get(newDist).add(neighbor);
                            node2PathPredecessor.put(neighbor, outEdge);
                        }
                    }
                }
                iterator.remove();
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

    public List<Edge<N>> getPath(N target) {
        if (node2PathDistance.get(target) == INVALID_WEIGHT) {
            return List.of();
        }
        List<Edge<N>> path = new ArrayList<>();
        Edge<N> curr = node2PathPredecessor.get(target);
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

    public enum SSSPAlgorithm {
        DIJKSTRA, DIAL
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

