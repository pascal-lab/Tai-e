package sa.dataflow.analysis.deadcode;

import sa.dataflow.lattice.DataFlowTag;
import sa.dataflow.lattice.FlowSet;
import soot.Body;
import soot.BodyTransformer;
import soot.Local;
import soot.Unit;
import soot.jimple.AnyNewExpr;
import soot.jimple.AssignStmt;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The analysis that detects dead code. This transformer must be executed after
 * ConstantPropagation and LiveVariableAnalysis.
 */
public class DeadCodeElimination extends BodyTransformer {

    private static final DeadCodeElimination INSTANCE = new DeadCodeElimination();

    public static DeadCodeElimination v() {
        return INSTANCE;
    }

    @Override
    protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
        outputResult(b, detectDeadCode(b));
    }

    private synchronized void outputResult(Body body, Set<Unit> deadCode) {
        System.out.println("------ " + body.getMethod() + " [dead code] -----");
        body.getUnits()
                .stream()
                .filter(deadCode::contains)
                .forEach(u ->
                        System.out.println("L" + u.getJavaSourceStartLineNumber()
                                + "{" + u + "}"));
    }

    private Set<Unit> detectDeadCode(Body b) {
        DirectedGraph<Unit> cfg = new BriefUnitGraph(b);
        Set<Unit> result = new HashSet<>();

        // 1. unconditional unreachable code

        // 2. control-flow unreachable code

        // 3. dead assignment
        result.addAll(detectDeadAssignments(b));

        return result;
    }

    /**
     *
     * @param cfg
     * @return
     */
    private Set<Unit> detectControlFlowUnreachableCode(DirectedGraph<Unit> cfg) {
        throw new UnsupportedOperationException();
    }

    /**
     * For assignment x = expr, if x is not live after the assignment and
     * expr has no side-effect, then the assignment is dead and can be eliminated.
     */
    private Set<Unit> detectDeadAssignments(Body body) {
        Set<Unit> deadAssigns = new HashSet<>();
        @SuppressWarnings("unchecked")
        DataFlowTag<Unit, FlowSet<Local>> liveVarTag =
                (DataFlowTag<Unit, FlowSet<Local>>) body.getTag("LiveVarTag");
        Map<Unit, FlowSet<Local>> liveVarMap = liveVarTag.getDataFlowMap();
        for (Unit unit : body.getUnits()) {
            if (unit instanceof AssignStmt) {
                AssignStmt assign = (AssignStmt) unit;
                if (!liveVarMap.get(unit).contains(assign.getLeftOp())
                        && !hasSideEffect(assign)) {
                    deadAssigns.add(assign);
                }
            }
        }
        return deadAssigns;
    }

    private boolean hasSideEffect(AssignStmt assign) {
        return assign.containsInvokeExpr()
                || assign.getRightOp() instanceof AnyNewExpr;
    }

}
