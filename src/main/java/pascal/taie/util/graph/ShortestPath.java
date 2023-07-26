package pascal.taie.util.graph;



import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;


import java.util.*;

/**
 * Finds shortest from a single source to other nodes in a graph using
 * Dijkstra's algorithm or Dial's algorithm.
 *
 * @param <N> type of nodes
 */

public class ShortestPath<N> {

    private final Graph<N> graph;

    private final N source;

    private final Map<N, Integer> distances = Maps.newMap();

    private final Map<N, Map<N, Integer>> weights;

    private final Map<N, N> predecessors = Maps.newMap();

    private final Map<N, Boolean> finished = Maps.newMap();

    public enum ComputeKind {
        Dijkstra, Dial
    }

    public ShortestPath(Graph<N> graph, N source, Map<N, Map<N, Integer>> weights) {
        this.graph = graph;
        this.source = source;
        this.weights = weights;
    }

    /**
     * @return the graph of the SSSP
     */
    public Graph<N> getGraph() {
        return graph;
    }

    /**
     * @return the distances from source to the other nodes
     */
    public Map<N, Integer> getDistances() {
        return distances;
    }

    /**
     * @return the source node of SSSP
     */
    public N getSource() {
        return source;
    }

    /**
     * @return the weights of all edges
     */
    public Map<N, Map<N, Integer>> getWeights() {
        return weights;
    }

    /**
     * @return the predecessors of all nodes for the shortest
     * path from source to them
     */
    public Map<N, N> getPredecessors() {
        return predecessors;
    }


    /**
     * @param kind choose the algorithm to compute the shortest path
     * compute the shortest path from source node to other nodes
     */
    public void compute(ComputeKind kind) {
        switch (kind) {
            case Dijkstra -> computeWithDijkstra();
            case Dial -> computeWithDial();
            default -> throw new RuntimeException("There is not this algorithm's implement!");
        }

    }

    /**
     * compute the shortest path from source node to
     * other nodes, using dijkstra's algorithm
     */
    private void computeWithDijkstra() {
        // Initialize
        PriorityQueue<DistPair> queue = new PriorityQueue<>();
        graph.forEach(node -> predecessors.put(node, null));
        graph.forEach(node -> distances.put(node, (int) Float.POSITIVE_INFINITY));
        graph.forEach(node -> finished.put(node, false));

        // Set source node status
        distances.put(source, 0);
        queue.add(new DistPair(source, 0));

        // Main loop
        while (!queue.isEmpty()) {
            DistPair distPair = queue.poll();
            if (finished.get(distPair.node)) {
                continue;
            }
            finished.put(distPair.node, true);
            for (N node : graph.getSuccsOf(distPair.node)) {
                if (!finished.get(node)) {
                    int newDist = distPair.dist + weights.get(distPair.node).get(node);
                    if (newDist < distances.get(node)) {
                        distances.put(node, newDist);
                        predecessors.put(node, distPair.node);
                        queue.add(new DistPair(node, newDist));
                    }
                }
            }
        }

    }

    /**
     * compute the shortest path from source node to
     * other nodes, using dial's algorithm
     */
    private void computeWithDial() {
        // Initialize
        graph.forEach(node -> predecessors.put(node, null));
        graph.forEach(node -> distances.put(node, (int) Float.POSITIVE_INFINITY));
        graph.forEach(node -> finished.put(node, false));
        Map<Integer, Set<N>> bucket = Maps.newMap();

        // Set source node status
        bucket.put(0, Sets.newSet());
        bucket.get(0).add(source);
        distances.put(source, 0);

        // Main loop
        while (!bucket.isEmpty()) {
            Iterator<Integer> iter = bucket.keySet().iterator();
            int minDist = iter.next();
            Iterator<N> iterator = bucket.get(minDist).iterator();
            if(!iterator.hasNext()){
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
                    int newDist = distances.get(node) + weights.get(node).get(neighbor);
                    if (newDist < distances.get(neighbor)) {
                        distances.put(neighbor, newDist);
                        bucket.computeIfAbsent(newDist, k -> Sets.newSet());
                        bucket.get(newDist).add(neighbor);
                        predecessors.put(neighbor, node);
                    }
                }
            }
        }


    }

    /**
     * @param target the target node
     * @return the shortest path from source to target
     * if there is not a path from source node to target
     * node, it will return a single node of target itself
     */
    public LinkedList<N> getPath(N target) {
        LinkedList<N> path = new LinkedList<>();
        N curr = target;
        while (curr != null) {
            path.add(curr);
            curr = predecessors.get(curr);
        }
        Collections.reverse(path);
        return path;
    }

    /**
     * @param node the target node
     * @return the distance from source node to target node
     */
    public int getDistanceOf(N node)
    {
        return distances.get(node);
    }

    private class DistPair implements Comparable<DistPair> {
        private final N node;

        private final Integer dist;

        public DistPair(N node, Integer dist) {
            this.node = node;
            this.dist = dist;
        }

        @Override
        public int compareTo(DistPair other) {
            return Integer.compare(this.dist, other.dist);
        }
    }
}

