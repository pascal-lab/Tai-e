package pascal.taie.analysis.pta.plugin.taint.inferer;

import pascal.taie.analysis.graph.flowgraph.FlowEdge;
import pascal.taie.analysis.graph.flowgraph.Node;
import pascal.taie.analysis.graph.flowgraph.ObjectFlowGraph;
import pascal.taie.analysis.pta.PointerAnalysisResult;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.util.Map;
import java.util.Set;

public class TransWeightHandler {

    private final Map<FlowEdge, InferredTransfer> fEdge2iTrans = Maps.newHybridMap();

    public TransWeightHandler(Set<InferredTransfer> transfers, Solver solver){
        PointerAnalysisResult result = solver.getResult();
        MultiMap<JMethod, Invoke> method2CallSite = Maps.newMultiMap();
        ObjectFlowGraph ofg = result.getObjectFlowGraph();

        result.getCallGraph().getNodes().forEach(jMethod ->
                method2CallSite.putAll(jMethod, result.getCallGraph().getCallersOf(jMethod)));
        transfers.forEach(t -> {
            JMethod jm = t.getMethod();
            method2CallSite.get(jm).forEach(invoke -> {
                Node from = ofg.getVarNode(InvokeUtils.getVar(invoke, t.getFrom().index()));
                Node to = ofg.getVarNode(InvokeUtils.getVar(invoke, t.getTo().index()));
                ofg.getOutEdgesOf(from).forEach(edge -> {
                    if(edge.target().equals(to)){
                        fEdge2iTrans.put(edge, t);
                    }
                });
            });
        });
    }

    public int calWeight(FlowEdge edge){
        InferredTransfer transfer = fEdge2iTrans.get(edge);
        int weight = 0;
        if(transfer != null){
            weight = transfer.getWeight();
        }
        return weight;
    }
}
