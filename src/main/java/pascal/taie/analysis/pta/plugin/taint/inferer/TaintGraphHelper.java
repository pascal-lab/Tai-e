package pascal.taie.analysis.pta.plugin.taint.inferer;

import pascal.taie.analysis.graph.flowgraph.FlowKind;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.taint.TaintNode;
import pascal.taie.analysis.pta.plugin.taint.TaintObjectFlowEdge;
import pascal.taie.analysis.pta.plugin.taint.TaintObjectFlowGraph;
import pascal.taie.analysis.pta.plugin.taint.TaintPointerFlowGraph;
import pascal.taie.analysis.pta.plugin.taint.TaintTransfer;
import pascal.taie.analysis.pta.plugin.util.StrategyUtils;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.graph.MaxFlowMinCutSolver;

import java.util.Set;
import java.util.stream.Collectors;

public class TaintGraphHelper {

    private static final MultiMap<PointerFlowEdge, TaintTransfer> pfgEdge2TaintTrans = Maps.newMultiMap();

    private final MultiMap<TaintObjectFlowEdge, TaintTransfer> tofgEdge2TaintTrans = Maps.newMultiMap();

    public static void init(Solver solver, TaintPointerFlowGraph tpfg, Set<TaintTransfer> transfers) {
        CSManager csManager = solver.getCSManager();
        MultiMap<JMethod, CSCallSite> method2CSCallSite = StrategyUtils.getMethod2CSCallSites(
                solver.getCallGraph());
        for (TaintTransfer tf : transfers) {
            JMethod method = tf.getMethod();
            method2CSCallSite.get(method).forEach(csCallSite -> {
                CSVar from = StrategyUtils.getCSVar(csManager, csCallSite, tf.getFrom().index());
                CSVar to = StrategyUtils.getCSVar(csManager, csCallSite, tf.getTo().index());
                if (from != null && to != null) {
                    tpfg.getOutEdgesOf(from).stream()
                            .filter(edge -> edge.target().equals(to) && edge.kind() == FlowKind.OTHER)
                            .forEach(edge -> pfgEdge2TaintTrans.put(edge, tf));
                }
            });
        }
    }

    public TaintGraphHelper(TaintObjectFlowGraph tofg) {
        for (TaintNode node : tofg) {
            for (TaintObjectFlowEdge edge : tofg.getOutEdgesOf(node)) {
                Set<TaintTransfer> possibleTrans = pfgEdge2TaintTrans.get(edge.pointerFlowEdge());
                Type targetType = edge.target().taintObj().getObject().getType();
                // Only collect transfers with target type
                possibleTrans.stream()
                        .filter(tf -> tf.getType().equals(targetType))
                        .forEach(tf -> tofgEdge2TaintTrans.put(edge, tf));
            }
        }
    }

    private int getWeight(TaintTransfer transfer) {
        return transfer instanceof InferredTransfer inferredTransfer
                ? inferredTransfer.getWeight() : 0;
    }

    public int getWeight(TaintObjectFlowEdge edge) {
        return tofgEdge2TaintTrans.get(edge)
                .stream()
                .mapToInt(this::getWeight)
                .min()
                .orElse(0);
    }

    public int getCost(TaintObjectFlowEdge edge) {
        return 1;
    }

    public int getCapacity(TaintObjectFlowEdge edge) {
        if (tofgEdge2TaintTrans.containsKey(edge) && getWeight(edge) != 0) {
            return 1;
        }
        return MaxFlowMinCutSolver.INVALID_WEIGHT;
    }

    public Set<TaintTransfer> getTransfers(TaintObjectFlowEdge edge) {
        return tofgEdge2TaintTrans.get(edge);
    }

    public Set<InferredTransfer> getInferredTransfers(TaintObjectFlowEdge edge) {
        return tofgEdge2TaintTrans.get(edge)
                .stream()
                .filter(tf -> tf instanceof InferredTransfer)
                .map(tf -> (InferredTransfer) tf)
                .collect(Collectors.toUnmodifiableSet());
    }

    public Set<InferredTransfer> getAllInferredTransfers() {
        return tofgEdge2TaintTrans.values()
                .stream()
                .filter(tf -> tf instanceof InferredTransfer)
                .map(tf -> (InferredTransfer) tf)
                .collect(Collectors.toUnmodifiableSet());
    }

}
