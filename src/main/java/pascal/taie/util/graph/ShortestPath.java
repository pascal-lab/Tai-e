package pascal.taie.util.graph;

import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
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

    private Map<N, Integer> node2PathDistance;

    private Map<N, N> node2PathPredecessor;

    public ShortestPath(Graph<N> graph, N source, ToIntFunction<Edge<N>> weightCalc) {
        this.graph = graph;
        this.source = source;
        this.weightCalc = weightCalc;
    }

    /**
     * @return the predecessors of all nodes for the shortest
     * path from source to them
     */
    public Map<N, N> getNode2PathPredecessor() {
        return node2PathPredecessor;
    }

    /**
     * Compute the shortest paths from source node to other nodes
     *
     * @param algorithm algorithm to compute the shortest path
     */
    public void compute(SSSPAlgorithm algorithm) {
        if(node2PathDistance == null) {
            node2PathDistance = Maps.newMap();
            node2PathPredecessor = Maps.newMap();
            switch (algorithm) {
                case DIJKSTRA -> runDijkstra();
                case DIAL -> runDial();
                default -> throw new UnsupportedOperationException("No such SSSP algorithm");
            }
        }
    }

    private void runDijkstra() {
        // Initialize
        PriorityQueue<DistPair> queue = new PriorityQueue<>();
        Set<N> finished = Sets.newSet(graph.getNumberOfNodes());
        graph.forEach(node -> node2PathPredecessor.put(node, null));
        graph.forEach(node -> node2PathDistance.put(node, INVALID_WEIGHT));

        // Set source node status
        node2PathDistance.put(source, 0);
        queue.add(new DistPair(source, 0));

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
                        if (newDist < node2PathDistance.get(successor)) {
                            node2PathDistance.put(successor, newDist);
                            node2PathPredecessor.put(successor, minDistNode);
                            queue.add(new DistPair(successor, newDist));
                        }
                    }
                }
            }
        }
    }

    private void runDial() {
        // Initialize
        graph.forEach(node -> node2PathPredecessor.put(node, null));
        graph.forEach(node -> node2PathDistance.put(node, (int) Float.POSITIVE_INFINITY));
        graph.forEach(node -> finished.put(node, false));
        Map<Integer, Set<N>> bucket = Maps.newMap();

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
                if (finished.get(node)) {
                    iterator.remove();
                    continue;
                }
                finished.put(node, true);
                iterator.remove();
                for (N neighbor : graph.getSuccsOf(node)) {
                    if (finished.get(neighbor)) {
                        continue;
                    }
                    int newDist = node2PathDistance.get(node) + weights.get(node).get(neighbor);
                    if (newDist < node2PathDistance.get(neighbor)) {
                        node2PathDistance.put(neighbor, newDist);
                        bucket.computeIfAbsent(newDist, k -> Sets.newSet());
                        bucket.get(newDist).add(neighbor);
                        node2PathPredecessor.put(neighbor, node);
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
    public List<N> getPath(N target) {
        if(node2PathDistance.get(target) == INVALID_WEIGHT) {
            return List.of();
        }
        List<N> path = new ArrayList<>();
        N curr = target;
        while (curr != null) {
            path.add(curr);
            curr = node2PathPredecessor.get(curr);
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

        public DistPair(N node, int dist) {
            this.node = node;
            this.dist = dist;
        }

        @Override
        public int compareTo(DistPair other) {
            return Integer.compare(this.dist, other.dist);
        }
    }
}

