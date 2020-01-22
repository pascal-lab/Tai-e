package sa.dataflow.analysis.deadcode;

import sa.dataflow.analysis.constprop.Value;
import sa.dataflow.lattice.DataFlowTag;
import sa.dataflow.lattice.FlowMap;
import sa.dataflow.lattice.FlowSet;
import soot.Body;
import soot.BodyTransformer;
import soot.Local;
import soot.Unit;
import soot.jimple.AnyNewExpr;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.ConcreteRef;
import soot.jimple.DivExpr;
import soot.jimple.InvokeExpr;
import soot.jimple.RemExpr;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.Pair;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

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
        outputResult(b, findDeadCode(b));
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

    private Set<Unit> findDeadCode(Body b) {
        DirectedGraph<Unit> cfg = new BriefUnitGraph(b);
        Set<Unit> result = new HashSet<>();

        // 1. unreachable branches
        EdgeSet unreachBranches = findUnreachableBranches(b, cfg);

        // 2. control-flow unreachable code
        result.addAll(findControlFlowUnreachableCode(cfg, unreachBranches));

        // 3. dead assignment
        result.addAll(findDeadAssignments(b));

        return result;
    }

    private EdgeSet findUnreachableBranches(Body body, DirectedGraph<Unit> cfg) {
        @SuppressWarnings("unchecked")
        DataFlowTag<Unit, FlowMap<Local, Value>> constantTag =
                (DataFlowTag<Unit, FlowMap<Local, Value>>) body.getTag("ConstantTag");
        Map<Unit, FlowMap<Local, Value>> constantMap = constantTag.getDataFlowMap();
        EdgeSet unreachBranches = new EdgeSet();
        // TODO - finish me
        return unreachBranches;
    }

    /**
     *
     * @param cfg
     * @return
     */
    private Set<Unit> findControlFlowUnreachableCode(
            DirectedGraph<Unit> cfg, EdgeSet filteredEdges) {
        // Initialize graph traversal
        Unit entry = getEntry(cfg);
        Set<Unit> reachable = new HashSet<>();
        Stack<Unit> stack = new Stack<>();
        stack.push(entry);
        // Traverse the CFG to find reachable code
        while (!stack.isEmpty()) {
            Unit unit = stack.pop();
            reachable.add(unit);
            for (Unit succ: cfg.getSuccsOf(unit)) {
                if (!reachable.contains(succ)
                        && !filteredEdges.containsEdge(unit, succ)) {
                    stack.push(succ);
                }
            }
        }
        // Construct unreachable code
        Set<Unit> result = new HashSet<>();
        for (Unit unit : cfg) {
            if (!reachable.contains(unit)) {
                result.add(unit);
            }
        }
        return result;
    }

    /**
     * For assignment x = expr, if x is not live after the assignment and
     * expr has no side-effect, then the assignment is dead and can be eliminated.
     */
    private Set<Unit> findDeadAssignments(Body body) {
        @SuppressWarnings("unchecked")
        DataFlowTag<Unit, FlowSet<Local>> liveVarTag =
                (DataFlowTag<Unit, FlowSet<Local>>) body.getTag("LiveVarTag");
        Map<Unit, FlowSet<Local>> liveVarMap = liveVarTag.getDataFlowMap();
        Set<Unit> deadAssigns = new HashSet<>();
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
        // TODO - check DeadAssignmentEliminator.java
        soot.Value rhs = assign.getRightOp();
        return rhs instanceof InvokeExpr
                || rhs instanceof AnyNewExpr
                || rhs instanceof CastExpr
                || rhs instanceof ConcreteRef
                || rhs instanceof DivExpr || rhs instanceof RemExpr;
    }

    /**
     * Returns the real entry of the given control flow graph.
     * Each CFG should have only one entry. If the CFG has multiple heads
     * (due to some unreachable code), the real entry must appear at the first
     * position in the source code.
     */
    private Unit getEntry(DirectedGraph<Unit> cfg) {
        return cfg.getHeads()
                .stream()
                .min(Comparator.comparingInt(Unit::getJavaSourceStartLineNumber)
                        .thenComparingInt(Unit::getJavaSourceStartColumnNumber))
                .orElse(null);
    }

    private class EdgeSet {

        private Set<Pair<Unit, Unit>> edgeSet = new HashSet<>();

        private void addEdge(Unit from, Unit to) {
            edgeSet.add(new Pair<>(from, to));
        }

        private void removeEdge(Unit from, Unit to) {
            edgeSet.remove(new Pair<>(from, to));
        }

        private boolean containsEdge(Unit from, Unit to) {
            return edgeSet.contains(new Pair<>(from, to));
        }
    }
}
