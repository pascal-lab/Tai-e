package pascal.taie.analysis.pta.plugin.taint.inferer;

import pascal.taie.analysis.graph.flowgraph.FlowEdge;
import pascal.taie.analysis.graph.flowgraph.Node;
import pascal.taie.analysis.graph.flowgraph.ObjectFlowGraph;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.plugin.util.StrategyUtils;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class TransWeightHandler {

    private final Map<FlowEdge, TreeSet<InferredTransfer>> edge2InferTrans = Maps.newMap();

    public TransWeightHandler(PointerAnalysisResult result, Set<InferredTransfer> transfers) {
        MultiMap<JMethod, Invoke> method2CallSite = StrategyUtils.getMethod2CallSites(result.getCallGraph());
        ObjectFlowGraph ofg = result.getObjectFlowGraph();

        transfers.forEach(tf -> {
            JMethod method = tf.getMethod();
            method2CallSite.get(method).forEach(invoke -> {
                Node from = ofg.getVarNode(InvokeUtils.getVar(invoke, tf.getFrom().index()));
                Node to = ofg.getVarNode(InvokeUtils.getVar(invoke, tf.getTo().index()));
                ofg.getOutEdgesOf(from).forEach(edge -> {
                    if (edge.target().equals(to)) {
                        edge2InferTrans.computeIfAbsent(edge, __ -> Sets.newOrderedSet()).add(tf);
                    }
                });
            });
        });
    }

    public int getWeight(FlowEdge edge) {
        TreeSet<InferredTransfer> transfers = edge2InferTrans.get(edge);
        if(transfers == null) {
            return 0;
        }
        return transfers.first().getWeight();
    }
}
