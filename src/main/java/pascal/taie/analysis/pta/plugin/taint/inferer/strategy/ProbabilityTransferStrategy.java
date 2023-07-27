package pascal.taie.analysis.pta.plugin.taint.inferer.strategy;

import pascal.taie.analysis.graph.callgraph.CallGraph;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.taint.TaintManager;
import pascal.taie.analysis.pta.plugin.taint.TaintTransfer;
import pascal.taie.analysis.pta.plugin.taint.TransferPoint;
import pascal.taie.analysis.pta.plugin.taint.inferer.InfererContext;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import java.util.Set;

public class ProbabilityTransferStrategy implements TransInferStrategy{

    private Solver solver;

    private TaintManager taintManager;

    private final MultiMap<JMethod, CSCallSite> method2CSCallSite = Maps.newMultiMap();


    @Override
    public void setContext(InfererContext context) {
        TransInferStrategy.super.setContext(context);
        solver = context.solver();
        taintManager = context.taintManager();

        CallGraph<CSCallSite, CSMethod> callGraph = solver.getCallGraph();
        callGraph.reachableMethods()
                .forEach(csMethod ->
                        method2CSCallSite.putAll(csMethod.getMethod(), callGraph.getCallersOf(csMethod)));
    }

    @Override
    public Set<TaintTransfer> apply(JMethod method, Set<TaintTransfer> transfers) {

        //逻辑：查看该方法的每个调用点，理论上的三条路径，如果在不同的路径上，有不同的传播概率，那么就需要分别处理


        return null;
    }

    boolean hasSameTaint(Invoke invoke, TaintTransfer transfer)
    {
        TransferPoint from = transfer.getFrom();
        TransferPoint to = transfer.getTo();
        //Todo:调用点出的对应的transfer的两个var都有相同的taint了，那么我们就认为你这可以进行自然的taint流动了

        return false;
    }

    boolean isTransferProbabilityDifferent(JMethod method, TaintTransfer transfer) {
        boolean flag = false;

        int cnt = method2CSCallSite.get(method).size();

        method2CSCallSite.get(method).forEach(csCallSite -> {
            Invoke callSite = csCallSite.getCallSite();
            if(hasSameTaint(callSite, transfer))
            {
                //Todo: 对每个调用点都进行检查
            }});

        return false;
    }



    @Override
    public int getPriority() {
        return 0;
    }

    public int getWeight() {
        return 1;
    }


}
