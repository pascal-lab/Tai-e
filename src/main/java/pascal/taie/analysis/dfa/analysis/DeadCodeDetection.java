/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.analysis.dfa.analysis;

import pascal.taie.analysis.IntraproceduralAnalysis;
import pascal.taie.analysis.dfa.analysis.constprop.ConstantPropagation;
import pascal.taie.analysis.dfa.analysis.constprop.Value;
import pascal.taie.analysis.dfa.fact.DataflowResult;
import pascal.taie.analysis.dfa.fact.MapFact;
import pascal.taie.analysis.dfa.fact.SetFact;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.CFGBuilder;
import pascal.taie.analysis.graph.cfg.Edge;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.ArrayAccess;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.FieldAccess;
import pascal.taie.ir.exp.NewExp;
import pascal.taie.ir.exp.RValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignStmt;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.util.collection.SetUtils;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class DeadCodeDetection extends IntraproceduralAnalysis {

    public static final String ID = "deadcode";

    public DeadCodeDetection(AnalysisConfig config) {
        super(config);
    }

    @Override
    public Set<Stmt> analyze(IR ir) {
        Set<Stmt> deadCode = new TreeSet<>(Comparator.comparing(Stmt::getIndex));
        // 1. unreachable branches
        Set<Edge<Stmt>> unreachableBranches = findUnreachableBranches(ir);
        // 2. unreachable code
        deadCode.addAll(findUnreachableCode(ir, unreachableBranches));
        // 3. dead assignment
        deadCode.addAll(findDeadAssignments(ir));
        return deadCode;
    }

    private Set<Edge<Stmt>> findUnreachableBranches(IR ir) {
        CFG<Stmt> cfg = ir.getResult(CFGBuilder.ID);
        DataflowResult<Stmt, MapFact<Var, Value>> constants =
                ir.getResult(ConstantPropagation.ID);
        Set<Edge<Stmt>> unreachableBranches = SetUtils.newHybridSet();
        ir.getStmts().forEach(stmt -> {
            if (stmt instanceof If) {
                If ifStmt = (If) stmt;
                Value cond = ConstantPropagation.evaluate(
                        ifStmt.getCondition(), constants.getInFact(ifStmt));
                if (cond.isConstant()) {
                    int v = cond.getConstant();
                    cfg.outEdgesOf(ifStmt).forEach(edge -> {
                        if (v == 1 && edge.getKind() == Edge.Kind.IF_FALSE ||
                                v == 0 && edge.getKind() == Edge.Kind.IF_TRUE) {
                            unreachableBranches.add(edge);
                        }
                    });
                }
            }
        });
        return unreachableBranches;
    }

    private Set<Stmt> findUnreachableCode(IR ir, Set<Edge<Stmt>> filteredEdges) {
        CFG<Stmt> cfg = ir.getResult(CFGBuilder.ID);
        // Initialize graph traversal
        Stmt entry = cfg.getEntry();
        Set<Stmt> reachable = SetUtils.newSet();
        Queue<Stmt> queue = new LinkedList<>();
        queue.add(entry);
        // Traverse the CFG to find reachable code
        while (!queue.isEmpty()) {
            Stmt stmt = queue.remove();
            reachable.add(stmt);
            cfg.outEdgesOf(stmt).forEach(outEdge -> {
                if (!reachable.contains(outEdge.getTarget()) &&
                        !filteredEdges.contains(outEdge)) {
                    queue.add(outEdge.getTarget());
                }
            });
        }
        // Return unreachable code
        // Some unreachable code is naturally absent in cfg, thus here
        // we need to iterative ir to collect all unreachable code.
        return ir.getStmts()
                .stream()
                .filter(Predicate.not(reachable::contains))
                .collect(Collectors.toUnmodifiableSet());
    }

    /**
     * For assignment x = expr, if x is not live after the assignment and
     * expr has no side-effect, then the assignment is dead and can be eliminated.
     */
    private Set<Stmt> findDeadAssignments(IR ir) {
        // Obtain the live variable analysis results for this method
        DataflowResult<Stmt, SetFact<Var>> liveVars =
                ir.getResult(LiveVariableAnalysis.ID);
        Set<Stmt> deadAssigns = SetUtils.newSet();
        ir.getStmts().forEach(stmt -> {
            if (stmt instanceof AssignStmt<?, ?>) {
                AssignStmt<?, ?> assign = (AssignStmt<?, ?>) stmt;
                if (!liveVars.getOutFact(assign).contains(assign.getLValue()) &&
                        !mayHaveSideEffect(assign.getRValue())) {
                    deadAssigns.add(assign);
                }
            }
        });
        return deadAssigns;
    }

    private boolean mayHaveSideEffect(RValue rvalue) {
        // new expression modifies the heap
        if (rvalue instanceof NewExp ||
                // cast may trigger ClassCastException
                rvalue instanceof CastExp ||
                // static field access may trigger class initialization
                // instance field access may trigger NPE
                rvalue instanceof FieldAccess ||
                // array access may trigger NPE
                rvalue instanceof ArrayAccess) {
            return true;
        }
        if (rvalue instanceof ArithmeticExp) {
            ArithmeticExp.Op op = ((ArithmeticExp) rvalue).getOperator();
            // may trigger DivideByZeroException
            return op == ArithmeticExp.Op.DIV || op == ArithmeticExp.Op.REM;
        }
        return false;
    }
}
