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

package pascal.taie.analysis.dataflow.analysis;

import pascal.taie.analysis.IntraproceduralAnalysis;
import pascal.taie.analysis.dataflow.analysis.constprop.CPFact;
import pascal.taie.analysis.dataflow.analysis.constprop.ConstantPropagation;
import pascal.taie.analysis.dataflow.analysis.constprop.Value;
import pascal.taie.analysis.dataflow.fact.NodeResult;
import pascal.taie.analysis.dataflow.fact.SetFact;
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
import pascal.taie.ir.stmt.SwitchStmt;
import pascal.taie.util.collection.Sets;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

public class DeadCodeDetection extends IntraproceduralAnalysis {

    public static final String ID = "deadcode";

    public DeadCodeDetection(AnalysisConfig config) {
        super(config);
    }

    @Override
    public Set<Stmt> analyze(IR ir) {
        // obtain results of pre-analyses
        CFG<Stmt> cfg = ir.getResult(CFGBuilder.ID);
        NodeResult<Stmt, CPFact> constants =
                ir.getResult(ConstantPropagation.ID);
        NodeResult<Stmt, SetFact<Var>> liveVars =
                ir.getResult(LiveVariableAnalysis.ID);
        // keep statements (dead code) sorted in the result set
        Set<Stmt> deadCode = new TreeSet<>(Comparator.comparing(Stmt::getIndex));
        // initialize graph traversal
        Set<Stmt> visited = Sets.newSet(cfg.getNumberOfNodes());
        Queue<Stmt> queue = new ArrayDeque<>();
        queue.add(cfg.getEntry());
        while (!queue.isEmpty()) {
            Stmt stmt = queue.remove();
            visited.add(stmt);
            if (isDeadAssignment(stmt, liveVars)) {
                // record dead assignment
                deadCode.add(stmt);
            }
            cfg.outEdgesOf(stmt)
                    .filter(edge -> !isDeadBranch(edge, constants))
                    .map(Edge::getTarget)
                    .forEach(succ -> {
                        if (!visited.contains(succ)) {
                            queue.add(succ);
                        }
                    });
        }
        if (visited.size() < cfg.getNumberOfNodes()) {
            // this means that some nodes are not reachable during traversal
            for (Stmt s : ir) {
                if (!visited.contains(s)) {
                    deadCode.add(s);
                }
            }
        }
        return deadCode.isEmpty() ? Collections.emptySet() : deadCode;
    }

    private static boolean isDeadAssignment(
            Stmt stmt, NodeResult<Stmt, SetFact<Var>> liveVars) {
        if (stmt instanceof AssignStmt) {
            AssignStmt<?, ?> assign = (AssignStmt<?, ?>) stmt;
            if (assign.getLValue() instanceof Var) {
                Var lhs = (Var) assign.getLValue();
                return !liveVars.getOutFact(assign).contains(lhs) &&
                        hasNoSideEffect(assign.getRValue());
            }
        }
        return false;
    }

    private static boolean isDeadBranch(
            Edge<Stmt> edge, NodeResult<Stmt, CPFact> constants) {
        Stmt src = edge.getSource();
        if (src instanceof If) {
            If ifStmt = (If) src;
            Value cond = ConstantPropagation.evaluate(
                    ifStmt.getCondition(), constants.getInFact(ifStmt));
            if (cond.isConstant()) {
                int v = cond.getConstant();
                return v == 1 && edge.getKind() == Edge.Kind.IF_FALSE ||
                        v == 0 && edge.getKind() == Edge.Kind.IF_TRUE;
            }
        } else if (src instanceof SwitchStmt) {
            SwitchStmt switchStmt = (SwitchStmt) src;
            Value condV = ConstantPropagation.evaluate(
                    switchStmt.getValue(), constants.getInFact(switchStmt));
            if (condV.isConstant()) {
                int v = condV.getConstant();
                if (edge.isSwitchCase()) {
                    return v != edge.getCaseValue();
                } else { // default case
                    // if any other case matches the case value, then
                    // default case is unreachable (dead)
                    return switchStmt.getCaseValues()
                            .stream()
                            .anyMatch(x -> x == v);
                }
            }
        }
        return false;
    }

    private static boolean hasNoSideEffect(RValue rvalue) {
        // new expression modifies the heap
        if (rvalue instanceof NewExp ||
                // cast may trigger ClassCastException
                rvalue instanceof CastExp ||
                // static field access may trigger class initialization
                // instance field access may trigger NPE
                rvalue instanceof FieldAccess ||
                // array access may trigger NPE
                rvalue instanceof ArrayAccess) {
            return false;
        }
        if (rvalue instanceof ArithmeticExp) {
            ArithmeticExp.Op op = ((ArithmeticExp) rvalue).getOperator();
            // may trigger DivideByZeroException
            return op != ArithmeticExp.Op.DIV && op != ArithmeticExp.Op.REM;
        }
        return true;
    }
}
