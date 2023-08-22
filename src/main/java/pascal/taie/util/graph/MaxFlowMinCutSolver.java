package pascal.taie.util.graph;

import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.TwoKeyMap;

import java.util.*;
import java.util.function.ToIntFunction;

public class MaxFlowMinCutSolver<N> {
    public static final int INVALID_WEIGHT = Integer.MAX_VALUE;
    private final N source;
    private final N target;
    private final Graph<N> graph;
    private final ToIntFunction<Edge<N>> capacityCal;
    private MultiMap<N, N> successors;
    private MultiMap<N, N> predecessors;
    private TwoKeyMap<N, N, Integer> edge2Capacity;
    private TwoKeyMap<N, N, Edge<N>> new2init;
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
        predecessors = Maps.newMultiMap();
        successors = Maps.newMultiMap();
        edge2Capacity = Maps.newTwoKeyMap();
        node2pred = Maps.newMap();
        new2init = Maps.newTwoKeyMap();
        graph.getNodes().forEach(node -> node2pred.put(node, null));
        graph.getNodes().stream().map(graph::getOutEdgesOf)
                .flatMap(Collection::stream)
                .forEach(edge -> {
                    //if (edge.source() != target && edge.target() != source) {
                    this.recordCapacity(edge.source(), edge.target(), capacityCal.applyAsInt(edge));
                    new2init.put(edge.source(), edge.target(), edge);
                    if (!edge2Capacity.containsKey(edge.target(), edge.source())) {
                        this.recordCapacity(edge.target(), edge.source(), 0);
                    }
                    //}
                });
    }

    private void cleanUnused() {
        successors = null;
        predecessors = null;
        edge2Capacity = null;
        node2pred = null;
        new2init = null;
    }

    private void computeMinCut() {
        if (maxFlowValue == INVALID_WEIGHT) {
            return;
        }
        Set<N> sourceCanReach = sourceCanReach();
        for (N from : sourceCanReach) {
            for (N to : getSuccessorsOf(from)) {
                // whether the condition need to compute nodes can reach sink or not
                if (!sourceCanReach.contains(to) && new2init.get(from, to) != null) {
                    // add cut edge
                    result.add(new2init.get(from, to));
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
                for (N successor : getSuccessorsOf(curr)) {
                    if (getCapacity(curr, successor) > 0) {
                        workList.addLast(successor);
                    }
                }
            }
        }
        return Collections.unmodifiableSet(result);
    }

    private void computeMaxFlow() {
        int result = 0;
        while (bfs(source)) {
            int increase = INVALID_WEIGHT;
            List<N> path = new ArrayList<>();
            N curr = target;
            N last;
            int temp;
            path.add(curr);
            while (!curr.equals(source)) {
                last = curr;
                curr = node2pred.get(last);
                path.add(curr);
                temp = getCapacity(curr, last);
                if (temp < increase) {
                    increase = temp;
                }
            }

            // this condition indicate that there is a path without transfer edge
            if (increase == INVALID_WEIGHT) {
                return;
            }

            result += increase;
            int length = path.size();
            for (int i = length - 1; i > 0; i--) {
                N from = path.get(i);
                N to = path.get(i - 1);
                int capacity = getCapacity(from, to);
                if (capacity != INVALID_WEIGHT) {
                    edge2Capacity.put(from, to, capacity - increase);
                }
                capacity = getCapacity(to, from);
                if (capacity != INVALID_WEIGHT) {
                    edge2Capacity.put(to, from, capacity + increase);
                }
            }
        }
        this.maxFlowValue = result;
    }

    private boolean bfs(N start) {
        Set<N> visited = Sets.newSet();
        Deque<N> workList = new ArrayDeque<>();
        workList.addLast(start);
        while (!workList.isEmpty()) {
            N curr = workList.poll();
            if (visited.add(curr)) {
                for (N to : getSuccessorsOf(curr)) {
                    if (getCapacity(curr, to) > 0 && !visited.contains(to)) {
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

    /**
     * @param from     source node
     * @param to       target node
     * @param capacity if testEdge is existing, then change the capacity,
     *                 else add the edge with the capacity
     */
    private void recordCapacity(N from, N to, int capacity) {
        if (!edge2Capacity.containsKey(from, to)) {
            predecessors.put(to, from);
            successors.put(from, to);
        }
        edge2Capacity.put(from, to, capacity);
    }


    private Set<N> getSuccessorsOf(N node) {
        return successors.get(node);
    }

    private int getCapacity(N from, N to) {
        Integer capacity = edge2Capacity.get(from, to);
        if (capacity == null)
            throw new RuntimeException();
        return capacity;
    }

}
