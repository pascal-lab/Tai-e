/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.dataflow.analysis.deadcode;

import pascal.taie.dataflow.analysis.constprop.ConstantPropagation;
import pascal.taie.dataflow.analysis.constprop.FlowMap;
import pascal.taie.dataflow.analysis.constprop.Value;
import pascal.taie.dataflow.lattice.DataFlowTag;
import pascal.taie.dataflow.lattice.FlowSet;
import pascal.taie.util.SootUtils;
import soot.Body;
import soot.BodyTransformer;
import soot.BriefUnitPrinter;
import soot.Local;
import soot.Unit;
import soot.jimple.AnyNewExpr;
import soot.jimple.AssignStmt;
import soot.jimple.CastExpr;
import soot.jimple.ConcreteRef;
import soot.jimple.DivExpr;
import soot.jimple.IfStmt;
import soot.jimple.InvokeExpr;
import soot.jimple.RemExpr;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.scalar.Pair;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * The analysis that detects dead code. This transformer must be executed after
 * ConstantPropagation and LiveVariableAnalysis.
 */
public class DeadCodeDetection extends BodyTransformer {

    private static final DeadCodeDetection INSTANCE = new DeadCodeDetection();
    private static boolean isOutput = true;

    private DeadCodeDetection() {
    }

    public static DeadCodeDetection v() {
        return INSTANCE;
    }

    public static void setOutput(boolean isOutput) {
        DeadCodeDetection.isOutput = isOutput;
    }

    // ---------- analysis for dead code detection ----------
    private Set<Unit> findDeadCode(Body b) {
        DirectedGraph<Unit> cfg = new BriefUnitGraph(b);
        Set<Unit> deadCode = new HashSet<>();

        // 1. unreachable branches
        EdgeSet unreachableBranches = findUnreachableBranches(b);

        // 2. unreachable code
        deadCode.addAll(findUnreachableCode(cfg, unreachableBranches));

        // 3. dead assignment
        deadCode.addAll(findDeadAssignments(b));

        return deadCode;
    }

    private EdgeSet findUnreachableBranches(Body body) {
        @SuppressWarnings("unchecked")
        DataFlowTag<Unit, FlowMap> constantTag =
                (DataFlowTag<Unit, FlowMap>) body.getTag("ConstantTag");
        // Obtain the constant propagation results for this method
        Map<Unit, FlowMap> constantMap = constantTag.getDataFlowMap();
        EdgeSet unreachableBranches = new EdgeSet();
        for (Unit unit : body.getUnits()) {
            if (unit instanceof IfStmt) {
                IfStmt ifStmt = (IfStmt) unit;
                // Obtain the first statement of true and false branch
                Unit trueBranch = ifStmt.getTarget();
                Unit falseBranch = body.getUnits().getSuccOf(ifStmt);
                // Evaluate condition value
                // Note that in Jimple IR, the condition *must be* binary expression
                Value cond = ConstantPropagation.v()
                        .computeValue(ifStmt.getCondition(),
                                constantMap.get(ifStmt));
                if (cond.isConstant()) { // Condition is constant
                    if (cond.getConstant() == 1) { // Always true, false branch is unreachable
                        unreachableBranches.addEdge(ifStmt, falseBranch);
                    } else { // Always false, true branch is unreachable
                        unreachableBranches.addEdge(ifStmt, trueBranch);
                    }
                }
            }
        }
        return unreachableBranches;
    }

    private Set<Unit> findUnreachableCode(DirectedGraph<Unit> cfg,
                                          EdgeSet filteredEdges) {
        // Initialize graph traversal
        Unit entry = getEntry(cfg);
        Set<Unit> reachable = new HashSet<>();
        Queue<Unit> queue = new LinkedList<>();
        queue.add(entry);
        // Traverse the CFG to find reachable code
        while (!queue.isEmpty()) {
            Unit unit = queue.remove();
            reachable.add(unit);
            for (Unit succ : cfg.getSuccsOf(unit)) {
                if (!reachable.contains(succ)
                        && !filteredEdges.containsEdge(unit, succ)) {
                    queue.add(succ);
                }
            }
        }
        // Collect unreachable code
        Set<Unit> unreachable = new HashSet<>();
        for (Unit unit : cfg) {
            if (!reachable.contains(unit)) {
                unreachable.add(unit);
            }
        }
        return unreachable;
    }

    /**
     * For assignment x = expr, if x is not live after the assignment and
     * expr has no side-effect, then the assignment is dead and can be eliminated.
     */
    private Set<Unit> findDeadAssignments(Body body) {
        @SuppressWarnings("unchecked")
        DataFlowTag<Unit, FlowSet<Local>> liveVarTag =
                (DataFlowTag<Unit, FlowSet<Local>>) body.getTag("LiveVarTag");
        // Obtain the live variable analysis results for this method.
        Map<Unit, FlowSet<Local>> liveVarMap = liveVarTag.getDataFlowMap();
        Set<Unit> deadAssigns = new HashSet<>();
        for (Unit unit : body.getUnits()) {
            if (unit instanceof AssignStmt) {
                AssignStmt assign = (AssignStmt) unit;
                //noinspection SuspiciousMethodCalls
                if (!liveVarMap.get(unit).contains(assign.getLeftOp())
                        && !mayHaveSideEffect(assign)) {
                    deadAssigns.add(assign);
                }
            }
        }
        return deadAssigns;
    }

    private boolean mayHaveSideEffect(AssignStmt assign) {
        soot.Value rhs = assign.getRightOp();
        return rhs instanceof InvokeExpr // invocation may have any side-effects
                || rhs instanceof AnyNewExpr // new expression modifies the heap
                || rhs instanceof CastExpr // cast may trigger ClassCastException
                || rhs instanceof ConcreteRef // static field ref may trigger class initialization
                // instance field/array ref may trigger null pointer exception
                || rhs instanceof DivExpr || rhs instanceof RemExpr; // may trigger DivideByZeroException
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

    // ---------- Body transformer ----------
    @Override
    protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
        Set<Unit> deadCode = findDeadCode(b);
        if (ResultChecker.isAvailable()) {
            ResultChecker.get().compare(b, deadCode);
        }
        if (isOutput) {
            outputResult(b, deadCode);
        }
    }

    private synchronized void outputResult(Body body, Set<Unit> deadCode) {
        System.out.println("------ " + body.getMethod() + " [dead code] -----");
        BriefUnitPrinter up = new BriefUnitPrinter(body);
        body.getUnits()
                .stream()
                .filter(deadCode::contains)
                .forEach(u -> System.out.println(SootUtils.unitToString(up, u)));
        System.out.println();
    }

    /**
     * Represents a set of control-flow edges.
     */
    private static class EdgeSet {

        private final Set<Pair<Unit, Unit>> edgeSet = new HashSet<>();

        private void addEdge(Unit from, Unit to) {
            edgeSet.add(new Pair<>(from, to));
        }

        private boolean containsEdge(Unit from, Unit to) {
            return edgeSet.contains(new Pair<>(from, to));
        }
    }

}
