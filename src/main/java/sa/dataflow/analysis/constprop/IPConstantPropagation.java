package sa.dataflow.analysis.constprop;

import sa.callgraph.CallGraph;
import sa.callgraph.cha.CHACallGraphBuilder;
import sa.dataflow.analysis.EdgeTransfer;
import sa.dataflow.analysis.IPDataFlowAnalysis;
import sa.dataflow.solver.IPSolver;
import sa.dataflow.solver.IPWorkListSolver;
import sa.icfg.CallEdge;
import sa.icfg.ICFG;
import sa.icfg.JimpleICFG;
import sa.icfg.LocalEdge;
import sa.icfg.ReturnEdge;
import soot.Local;
import soot.SceneTransformer;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.AssignStmt;
import soot.jimple.DefinitionStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.ReturnStmt;
import soot.jimple.Stmt;

import java.util.List;
import java.util.Map;

public class IPConstantPropagation extends SceneTransformer
        implements IPDataFlowAnalysis<FlowMap, SootMethod, Unit> {

    private ICFG<SootMethod, Unit> icfg;

    private ConstantPropagation cp;

    public IPConstantPropagation() {
        cp = ConstantPropagation.v();
    }

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
        return cp.transfer(unit, in, out);
    }

    @Override
    public boolean transferCallNode(Unit callSite, FlowMap in, FlowMap out) {
        boolean changed = false;
        if (callSite instanceof AssignStmt) {
            // The information of LHS comes from the return edges
            Local lhs = (Local) ((AssignStmt) callSite).getLeftOp();
            for (Local inLocal : in.keySet()) {
                if (!inLocal.equals(lhs)) {
                    changed |= out.update(inLocal, in.get(inLocal));
                }
            }
        } else {
            changed = out.copyFrom(in);
        }
        return changed;
    }

    private EdgeTransfer<Unit, FlowMap> edgeTransfer = new EdgeTransfer<Unit, FlowMap>() {

        @Override
        public void transferLocalEdge(LocalEdge<Unit> edge, FlowMap nodeOut, FlowMap edgeFlow) {
            // Set edge flow to node out-flow
            edgeFlow.copyFrom(nodeOut);
        }

        @Override
        public void transferCallEdge(CallEdge<Unit> edge, FlowMap callSiteInFlow, FlowMap edgeFlow) {
            // Passing arguments at call site to parameters of the callee
            InvokeExpr invoke = ((Stmt) edge.getSource()).getInvokeExpr();
            Unit entry = edge.getTarget();
            SootMethod callee = icfg.getContainingMethodOf(entry);
            List<soot.Value> args = invoke.getArgs();
            List<soot.Value> params = callee.getActiveBody().getParameterRefs();
            for (int i = 0; i < args.size(); ++i) {
                soot.Value arg = args.get(i);
                soot.Value param = params.get(i);
                Value argValue = cp.toValue(callSiteInFlow, param.getType(), arg);
                if (param instanceof Local) {
                    edgeFlow.update((Local) param, argValue);
                }
            }
            // TODO - handle this variable properly
            edgeFlow.update(callee.getActiveBody().getThisLocal(), Value.getNAC());
        }

        @Override
        public void transferReturnEdge(ReturnEdge<Unit> edge, FlowMap returnOutFlow, FlowMap edgeFlow) {
            // Passing return value to the LHS of the call statement
            Unit callSite = edge.getCallSite();
            Unit exit = edge.getSource();
            // TODO - consider exceptional exit?
            if (exit instanceof ReturnStmt &&
                    callSite instanceof AssignStmt) {
                Local ret = (Local) ((ReturnStmt) exit).getOp();
                Value value = returnOutFlow.get(ret);
                Local lhs = (Local) ((AssignStmt) callSite).getLeftOp();
                edgeFlow.update(lhs, value);
            }
        }
    };

    @Override
    public EdgeTransfer<Unit, FlowMap> getEdgeTransfer() {
        return edgeTransfer;
    }

    private void setICFG(ICFG<SootMethod, Unit> icfg) {
        this.icfg = icfg;
    }

    @Override
    protected void internalTransform(String phaseName, Map<String, String> options) {
        CallGraph<Unit, SootMethod> callGraph = CHACallGraphBuilder.getRecentCallGraph();
        ICFG<SootMethod, Unit> icfg = new JimpleICFG(callGraph);
        setICFG(icfg);
        IPSolver<FlowMap, SootMethod, Unit> solver = new IPWorkListSolver<>(this, icfg);
        solver.solve();
        callGraph.getReachableMethods()
                .stream()
                .map(SootMethod::getActiveBody)
                .forEach(b -> cp.outputResult(b, solver.getAfterFlow()));
    }
}
