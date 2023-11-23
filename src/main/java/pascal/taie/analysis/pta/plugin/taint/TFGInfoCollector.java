package pascal.taie.analysis.pta.plugin.taint;

import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.taint.inferer.TaintGraphHelper;
import pascal.taie.ir.exp.Var;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.TwoKeyMap;
import pascal.taie.util.collection.TwoKeyMultiMap;
import pascal.taie.util.graph.MaxFlowMinCutSolver;
import pascal.taie.util.graph.ShortestPath;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class TFGInfoCollector {

    private final Solver solver;
    private final TaintManager manager;
    private final TaintConfig config;
    private final TransferHandler transferHandler;
    private final Set<TaintFlow> taintFlows;
    private TwoKeyMap<Var, Var, TaintPath> shortestTaintPaths;
    private TwoKeyMultiMap<Var, Var, TaintObjectFlowEdge> minimumCutEdges;

    public TFGInfoCollector(Solver solver,
                            TaintManager manager,
                            TaintConfig config,
                            TransferHandler transferHandler,
                            Set<TaintFlow> taintFlows) {
        this.solver = solver;
        this.manager = manager;
        this.config = config;
        this.transferHandler = transferHandler;
        this.taintFlows = taintFlows;
    }

    public TaintPath getShortestTaintPath(Var sourceVar, Var sinkVar) {
        return shortestTaintPaths.get(sourceVar, sinkVar);
    }

    public Set<TaintObjectFlowEdge> getMinimumCutEdges(Var sourceVar, Var sinkVar) {
        return minimumCutEdges.get(sourceVar, sinkVar);
    }

    public Set<Pair<Var, Var>> getInfoKeySet() {
        return shortestTaintPaths.twoKeySet();
    }

    public Collection<TaintPath> getAllShortestTaintPath() {
        return shortestTaintPaths.values();
    }

    // TODO: handle other call source type
    public void collectShortestTaintPaths() {
        shortestTaintPaths = Maps.newTwoKeyMap();
        TaintPointerFlowGraph tpfg = new TPFGBuilder(solver, manager, taintFlows, false, true).build();
        TaintGraphHelper.init(solver, tpfg, transferHandler.getTransfers());
        TOFGBuilder tofgBuilder = new TOFGBuilder(tpfg, solver, manager, config, taintFlows);
        Set<Pointer> sourcesReachSink = tpfg.getSourcePointers().stream()
                                            .filter(source -> tpfg.getOutDegreeOf(source) > 0)
                                            .collect(Collectors.toSet());

        for (Pointer source : sourcesReachSink) {
            source.objects()
                  .filter(csObj -> manager.isTaint(csObj.getObject()))
                  .forEach(taintObj -> {
                      TaintObjectFlowGraph tofg = tofgBuilder.build(source, taintObj);
                      TaintGraphHelper graphHelper = new TaintGraphHelper(tofg);
                      ShortestPath<TaintNode, TaintObjectFlowEdge> shortestPath = new ShortestPath<>(
                              tofg, tofg.getSourceNode(), graphHelper::getWeight, graphHelper::getCost);
                      shortestPath.compute();

                      tofg.getSinkNodes().stream()
                          .filter(shortestPath::canReach)
                          .forEach(taintNode -> {
                              int weight = shortestPath.getDistance(taintNode);
                              List<TaintObjectFlowEdge> path = shortestPath.getPath(taintNode);
                              Var sourceVar = ((CSVar) source).getVar();
                              Var sinkVar = ((CSVar) taintNode.pointer()).getVar();
                              TaintPath oldPath = shortestTaintPaths.get(sourceVar, sinkVar);
                              TaintPath newPath = new TaintPath(path, weight, tofg, taintNode, graphHelper);
                              if (oldPath == null || newPath.compareTo(oldPath) < 0) {
                                  shortestTaintPaths.put(sourceVar, sinkVar, newPath);
                              }
                          });
                  });

        }
    }

    public void collectMinimumCuteEdges() {
        minimumCutEdges = Maps.newTwoKeyMultiMap();
        for (var entry : shortestTaintPaths.entrySet()) {
            Var sourceVar = entry.key1();
            Var sinkVar = entry.key2();
            TaintPath taintPath = entry.value();
            TaintGraphHelper helper = taintPath.graphHelper();
            MaxFlowMinCutSolver<TaintNode> minCut = new MaxFlowMinCutSolver<>(taintPath.tofg(),
                    taintPath.tofg().getSourceNode(),
                    taintPath.sinkNode(),
                    edge -> helper.getCapacity((TaintObjectFlowEdge) edge));
            minCut.compute();
            minCut.getMinCutEdges()
                  .forEach(edge -> minimumCutEdges.put(sourceVar, sinkVar, (TaintObjectFlowEdge) edge));
        }
    }

    public record TaintPath(List<TaintObjectFlowEdge> path,
                            int weight,
                            TaintObjectFlowGraph tofg,
                            TaintNode sinkNode,
                            TaintGraphHelper graphHelper) implements Comparable<TaintPath> {

        @Override
        public int compareTo(TaintPath other) {
            return weight != other.weight ? Integer.compare(weight, other.weight)
                    : Integer.compare(path.size(), other.path.size());
        }
    }

}
