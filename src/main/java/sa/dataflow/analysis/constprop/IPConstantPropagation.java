package sa.dataflow.analysis.constprop;

import sa.callgraph.CallGraph;
import sa.dataflow.analysis.EdgeTransfer;
import sa.dataflow.analysis.IPDataFlowAnalysis;
import sa.dataflow.solver.IPSolver;
import sa.dataflow.solver.IPWorkListSolver;
import sa.icfg.CallEdge;
import sa.icfg.ICFG;
import sa.icfg.JimpleICFG;
import sa.icfg.LocalEdge;
import sa.icfg.ReturnEdge;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Unit;

import java.util.Map;

public class IPConstantPropagation extends SceneTransformer
        implements IPDataFlowAnalysis<FlowMap, SootMethod, Unit> {

    private static final IPConstantPropagation INSTANCE = new IPConstantPropagation();

    public static IPConstantPropagation v() {
        return INSTANCE;
    }

    private ConstantPropagation cp = ConstantPropagation.v();

    @Override
    public boolean isForward() {
        return cp.isForward();
    }

    @Override
    public FlowMap getEntryInitialValue() {
        return cp.getEntryInitialValue();
    }

    @Override
    public FlowMap newInitialValue() {
        return cp.newInitialValue();
    }

    @Override
    public FlowMap meet(FlowMap v1, FlowMap v2) {
        return cp.meet(v1, v2);
    }

    @Override
    public boolean transfer(Unit unit, FlowMap in, FlowMap out) {
        return false;
    }

    @Override
    public boolean transferCallNode(Unit callSite, FlowMap in, FlowMap out) {
        return false;
    }

    private EdgeTransfer<Unit, FlowMap> edgeTransfer = new EdgeTransfer<Unit, FlowMap>() {

        @Override
        public void transferLocalEdge(LocalEdge<Unit> edge, FlowMap nodeOut, FlowMap edgeFlow) {

        }

        @Override
        public void transferCallEdge(CallEdge<Unit> edge, FlowMap callSiteInFlow, FlowMap edgeFlow) {

        }

        @Override
        public void transferReturnEdge(ReturnEdge<Unit> edge, FlowMap returnOutFlow, FlowMap edgeFlow) {

        }
    };

    @Override
    public EdgeTransfer<Unit, FlowMap> getEdgeTransfer() {
        return edgeTransfer;
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        CallGraph<Unit, SootMethod> callGraph = null;
        ICFG<SootMethod, Unit> icfg = new JimpleICFG(callGraph);
        IPSolver<FlowMap, SootMethod, Unit> solver = new IPWorkListSolver<>(this, icfg);
        solver.solve();
        callGraph.getReachableMethods()
                .stream()
                .map(SootMethod::getActiveBody)
                .forEach(b -> cp.outputResult(b, solver.getAfterFlow()));
    }
}
