package pascal.taie.util.graph;

import pascal.taie.util.MutableInt;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.SetQueue;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.TwoKeyMap;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.ToIntFunction;

public class MaxFlowMinCutSolver<N> {
    public static final int INVALID_WEIGHT = Integer.MAX_VALUE;
    private final N source;
    private final N target;
    private final Graph<N> graph;
    private final ToIntFunction<Edge<N>> capacityCal;
    private MultiMap<N, N> successors;
    private TwoKeyMap<N, N, MutableInt> edge2Capacity;
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
        successors = Maps.newMultiMap();
        edge2Capacity = Maps.newTwoKeyMap();
        node2pred = Maps.newMap();
        new2init = Maps.newTwoKeyMap();
        graph.getNodes().forEach(node -> node2pred.put(node, null));
        graph.getNodes().stream().map(graph::getOutEdgesOf)
                .flatMap(Collection::stream)
                .forEach(edge -> {
                    //if (edge.source() != target && edge.target() != source) {
                    setCapacity(edge.source(), edge.target(), capacityCal.applyAsInt(edge));
                    new2init.put(edge.source(), edge.target(), edge);
                    //}
                });
    }

    private void cleanUnused() {
        successors = null;
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
            for (N to : graph.getSuccsOf(from)) {
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
                    workList.addLast(successor);
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
                    setCapacity(from, to, capacity - increase);
                }
                capacity = getCapacity(to, from);
                if (capacity != INVALID_WEIGHT) {
                    setCapacity(to, from, capacity + increase);
                }
            }
        }
        this.maxFlowValue = result;
    }

    private boolean bfs(N start) {
        Set<N> visited = Sets.newSet();
        SetQueue<N> workList = new SetQueue<>();
        workList.add(start);
        while (!workList.isEmpty()) {
            N curr = workList.poll();
            //logger.info("current visited nodes: {}", visited.size());
            if (visited.add(curr)) {
                for (N to : getSuccessorsOf(curr)) {
                    if (!visited.contains(to)) {
                        node2pred.put(to, curr);
                        if (to.equals(target)) {
                            return true;
                        }
                        workList.add(to);
                    }
                }
            }
        }
        return false;
    }

    private Set<N> getSuccessorsOf(N node) {
        return successors.get(node);
    }

    private int getCapacity(N from, N to) {
        MutableInt capacity = edge2Capacity.get(from, to);
        // return 0 for avoiding initialize the edge capacity of the edge out of init graph
        if (capacity == null) {
            return 0;
        }
        return capacity.intValue();
    }

    private void setCapacity(N from, N to, int capacity) {
        if (capacity <= 0) {
            successors.remove(from, to);
        } else {
            successors.put(from, to);
        }
        edge2Capacity.computeIfAbsent(from, to, (__, ___) -> new MutableInt(0)).set(capacity);
    }
}
