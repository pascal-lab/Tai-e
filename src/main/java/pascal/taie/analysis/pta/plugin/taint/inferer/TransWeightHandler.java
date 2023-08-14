package pascal.taie.analysis.pta.plugin.taint.inferer;

import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.plugin.taint.TaintPointerFlowGraph;
import pascal.taie.analysis.pta.plugin.util.StrategyUtils;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class TransWeightHandler {

    private final Map<PointerFlowEdge, TreeSet<InferredTransfer>> edge2InferTrans = Maps.newMap();

    public TransWeightHandler(CallGraph<CSCallSite, CSMethod> callGraph,
                              TaintPointerFlowGraph tpfg,
                              CSManager csManager,
                              Set<InferredTransfer> transfers) {
        MultiMap<JMethod, CSCallSite> method2CSCallSite = StrategyUtils.getMethod2CSCallSites(callGraph);
        transfers.forEach(tf -> {
            JMethod method = tf.getMethod();
            method2CSCallSite.get(method).forEach(csCallSite -> {
                CSVar from = StrategyUtils.getCSVar(csManager, csCallSite, tf.getFrom().index());
                CSVar to = StrategyUtils.getCSVar(csManager, csCallSite, tf.getTo().index());
                tpfg.getOutEdgesOf(from).forEach(edge -> {
                    if (edge.target().equals(to)) {
                        edge2InferTrans.computeIfAbsent(edge, __ -> Sets.newOrderedSet()).add(tf);
                    }
                });
            });
        });
    }

    public int getWeight(PointerFlowEdge edge) {
        TreeSet<InferredTransfer> transfers = edge2InferTrans.get(edge);
        if (transfers == null) {
            return 0;
        }
        return transfers.first().getWeight();
    }

    public int getCost(PointerFlowEdge edge) {
        return 1;
    }

    public Set<InferredTransfer> getInferredTrans(PointerFlowEdge edge) {
        Set<InferredTransfer> transfers = edge2InferTrans.get(edge);
        if (transfers == null) {
            return Set.of();
        }
        return transfers;
    }
}
