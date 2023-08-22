package pascal.taie.util.graph;

import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;

import javax.annotation.Nullable;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;

public class MaxFlowMinCutSolver<N> {
    public static final int INVALID_WEIGHT = Integer.MAX_VALUE;
    private final N source;
    private final N target;
    private final Graph<N> graph;
    private final ToIntFunction<Edge<N>> capacityCal;
    private final Set<N> nodes = Sets.newSet();
    private MultiMap<N, CapacityEdge<N>> inEdges;
    private MultiMap<N, CapacityEdge<N>> outEdges;
    private Map<CapacityEdge<N>, Integer> edge2Capacity;
    private Map<CapacityEdge<N>, Edge<N>> new2init;
    private Map<N, N> node2pred;
    private Set<Edge<N>> result;
    private int maxFlowValue = INVALID_WEIGHT;

    public MaxFlowMinCutSolver(Graph<N> graph, N source, N target, ToIntFunction<Edge<N>> capacityCal) {
        this.graph = graph;
        this.source = source;
        this.target = target;
        this.capacityCal = capacityCal;
    }

    public int getMaxFlowValue() {
        return maxFlowValue;
    }

    public Set<Edge<N>> getMinCutEdges() {
        if (result.isEmpty() || maxFlowValue == INVALID_WEIGHT) {
            return Set.of();
        }
        return result;
    }

    public void compute() {
        if (result == null) {
            init();
            computeMaxFlow();
            computeMinCut();
            cleanUnused();
        }
    }

    private void init() {
        result = Sets.newSet();
        inEdges = Maps.newMultiMap();
        outEdges = Maps.newMultiMap();
        edge2Capacity = Maps.newMap();
        node2pred = Maps.newMap();
        new2init = Maps.newMap();
        graph.getNodes().forEach(node -> node2pred.put(node, null));
        graph.getNodes().stream().map(graph::getOutEdgesOf)
                .flatMap(Collection::stream)
                .forEach(edge -> {
                    //if (edge.source() != target && edge.target() != source) {
                    CapacityEdge<N> newEdge1 = new CapacityEdge<>(edge.source(), edge.target());
                    this.addEdge(newEdge1, capacityCal.applyAsInt(edge));
                    new2init.put(newEdge1, edge);
                    CapacityEdge<N> newEdge2 = new CapacityEdge<>(edge.target(), edge.source());
                    if (!edge2Capacity.containsKey(newEdge2)) {
                        this.addEdge(newEdge2, 0);
                    }
                    //}
                });
    }

    private void cleanUnused() {
        inEdges = null;
        outEdges = null;
        edge2Capacity = null;
        node2pred = null;
        new2init = null;
    }

    private void computeMinCut() {
        if (maxFlowValue == INVALID_WEIGHT) {
            return;
        }
        Set<N> sourceCanReach = sourceCanReach();
        for (N node : sourceCanReach) {
            for (CapacityEdge<N> edge : getOutEdgesOf(node)) {
                // whether the condition need to compute nodes can reach sink or not
                if (!sourceCanReach.contains(edge.target()) && new2init.get(edge) != null) {
                    // add cut edge
                    result.add(new2init.get(edge));
                }
            }
        }
    }

    private Set<N> sourceCanReach() {
        Set<N> result = Sets.newSet();
        Deque<N> workList = new ArrayDeque<>();
        workList.addLast(source);
        while (!workList.isEmpty()) {
            N curr = workList.poll();
            if (result.add(curr)) {
                for (CapacityEdge<N> edge : getOutEdgesOf(curr)) {
                    if (edge2Capacity.get(edge) > 0) {
                        workList.addLast(edge.target());
                    }
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private void computeMaxFlow() {
        int result = 0;
        while (bfs(source)) {
            Deque<CapacityEdge<N>> path = new ArrayDeque<>();
            N curr = target;
            N last;
            while (!curr.equals(source)) {
                last = curr;
                curr = node2pred.get(last);
                path.addFirst(getEdge(curr, last));
            }
            int increase = getMinCapacity(path);

            // this condition indicate that there is a path without transfer edge
            if (increase == INVALID_WEIGHT) {
                return;
            }

            result += increase;
            for (CapacityEdge<N> edge : path) {
                int capacity = edge2Capacity.get(edge);
                if (capacity != INVALID_WEIGHT) {
                    edge2Capacity.put(edge, capacity - increase);
                }
                CapacityEdge<N> newEdge = getEdge(edge.target(), edge.source());
                capacity = edge2Capacity.get(newEdge);
                if (capacity != INVALID_WEIGHT) {
                    edge2Capacity.put(newEdge, capacity + increase);
                }
            }
        }
        this.maxFlowValue = result;
    }

    private int getMinCapacity(Deque<CapacityEdge<N>> path) {
        int result = INVALID_WEIGHT;
        for (CapacityEdge<N> edge : path) {
            int capacity = edge2Capacity.get(edge);
            if (capacity < result) {
                result = capacity;
            }
        }
        return result;
    }

    private boolean bfs(N start) {
        Set<N> visited = Sets.newSet();
        Deque<N> workList = new ArrayDeque<>();
        workList.addLast(start);
        while (!workList.isEmpty()) {
            N curr = workList.poll();
            if (visited.add(curr)) {
                for (CapacityEdge<N> edge : getOutEdgesOf(curr)) {
                    N to = edge.target;
                    if (edge2Capacity.get(edge) > 0 && !visited.contains(to)) {
                        node2pred.put(to, curr);
                        if (to.equals(target)) {
                            return true;
                        }
                        workList.addLast(to);
                    }
                }
            }
        }
        return false;
    }

    @Nullable
    private CapacityEdge<N> getEdge(N from, N to) {
        CapacityEdge<N> result = null;
        for (CapacityEdge<N> edge : getOutEdgesOf(from)) {
            if (edge.target().equals(to)) {
                result = edge;
                break;
            }
        }
        return result;
    }

    /**
     * @param edge     edge
     * @param capacity if testEdge is existing, then change the capacity,
     *                 else add the edge with the capacity
     */
    private void addEdge(CapacityEdge<N> edge, int capacity) {
        if (!edge2Capacity.containsKey(edge)) {
            N source = edge.source();
            N target = edge.target();
            edge2Capacity.put(edge, capacity);
            inEdges.put(target, edge);
            outEdges.put(source, edge);
            nodes.add(source);
            nodes.add(target);
        } else {
            edge2Capacity.put(edge, capacity);
        }
    }

    private Set<CapacityEdge<N>> getOutEdgesOf(N node) {
        return outEdges.get(node);
    }

    private record CapacityEdge<N>(N source, N target) implements Edge<N> {

    }
}
