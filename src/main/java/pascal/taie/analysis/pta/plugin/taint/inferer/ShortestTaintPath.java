package pascal.taie.analysis.pta.plugin.taint.inferer;

import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.taint.TaintObjectFlowGraph;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.ToIntFunction;
public class ShortestTaintPath {

    private static final int INVALID_WEIGHT = Integer.MAX_VALUE;

    private final TaintObjectFlowGraph tofg;

    private final Pointer source;

    private final ToIntFunction<PointerFlowEdge> weightCalc;

    private final ToIntFunction<PointerFlowEdge> costCalc;

    private final Solver solver;

    private Map<Pointer, Integer> node2PathDistance;

    private Map<Pointer, PointerFlowEdge> node2PathPredecessor;

    private Map<Pointer, Integer> node2PathCost;

    public ShortestTaintPath(TaintObjectFlowGraph tofg,
                             Pointer source,
                             ToIntFunction<PointerFlowEdge> weightCalc,
                             ToIntFunction<PointerFlowEdge> costCalc,
                             Solver solver) {
        this.tofg = tofg;
        this.source = source;
        this.weightCalc = weightCalc;
        this.costCalc = costCalc;
        this.solver = solver;
    }

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
        PriorityQueue<DistPair> queue = new PriorityQueue<>();
        Set<Pointer> finished = Sets.newSet(tofg.getNumberOfNodes());
        tofg.forEach(node -> node2PathPredecessor.put(node, null));
        tofg.forEach(node -> node2PathDistance.put(node, INVALID_WEIGHT));
        tofg.forEach(node -> node2PathCost.put(node, 0));

        // Set source node status
        node2PathDistance.put(source, 0);
        node2PathCost.put(source, 0);
        queue.add(new DistPair(source, 0, 0));

        // Main loop
        while (!queue.isEmpty()) {
            DistPair distPair = queue.poll();
            Pointer minDistNode = distPair.node;
            int minDist = distPair.dist;
            if (finished.add(minDistNode)) {
                for (PointerFlowEdge outEdge : tofg.getOutEdgesOf(minDistNode)) {
                    Pointer successor = outEdge.target();
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
    public List<Pointer> getPathNode(Pointer target) {
        if (node2PathDistance.get(target) == INVALID_WEIGHT || node2PathPredecessor.get(target) == null) {
            return List.of();
        }
        List<Pointer> path = new ArrayList<>();
        Pointer curr = target;
        while (curr != null) {
            path.add(curr);
            curr = node2PathPredecessor.get(curr).source();
        }
        Collections.reverse(path);
        return path;
    }

    public List<PointerFlowEdge> getPath(Pointer target) {
        if (node2PathDistance.get(target) == INVALID_WEIGHT) {
            return List.of();
        }
        List<PointerFlowEdge> path = new ArrayList<>();
        PointerFlowEdge curr = node2PathPredecessor.get(target);
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
    public int getDistance(Pointer node) {
        return node2PathDistance.get(node);
    }

    record DistPair(Pointer node, int dist, int cost) implements Comparable<DistPair> {
        @Override
        public int compareTo(DistPair other) {
            if(this.dist == other.dist){
                return Integer.compare(this.cost, other.cost);
            }
            return Integer.compare(this.dist, other.dist);
        }
    }
}

