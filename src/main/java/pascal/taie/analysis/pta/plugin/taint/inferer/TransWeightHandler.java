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

import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

public class TransWeightHandler {

    private final MultiMap<FlowEdge, InferredTransfer> edge2InferTrans = Maps.newMultiMap(TreeSet::new);

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
                        edge2InferTrans.put(edge, tf);
                    }
                });
            });
        });
    }

    public int getWeight(FlowEdge edge) {
        Set<InferredTransfer> transfers = edge2InferTrans.get(edge);
        Optional<InferredTransfer> minWeightTrans = transfers.stream().findFirst();
        return minWeightTrans.map(InferredTransfer::getWeight).orElse(0);
    }
}
