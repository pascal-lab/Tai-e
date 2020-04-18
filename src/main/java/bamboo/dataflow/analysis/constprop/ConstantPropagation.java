/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.dataflow.analysis.constprop;

import bamboo.dataflow.analysis.DataFlowAnalysis;
import bamboo.dataflow.lattice.DataFlowTag;
import bamboo.dataflow.solver.Solver;
import bamboo.dataflow.solver.SolverFactory;
import bamboo.util.JimpleUtils;
import soot.Body;
import soot.BodyTransformer;
import soot.BriefUnitPrinter;
import soot.Local;
import soot.Unit;
import soot.jimple.AddExpr;
import soot.jimple.BinopExpr;
import soot.jimple.DefinitionStmt;
import soot.jimple.DivExpr;
import soot.jimple.EqExpr;
import soot.jimple.GeExpr;
import soot.jimple.GtExpr;
import soot.jimple.IntConstant;
import soot.jimple.LeExpr;
import soot.jimple.LtExpr;
import soot.jimple.MulExpr;
import soot.jimple.NeExpr;
import soot.jimple.SubExpr;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;

import java.util.Map;
import java.util.stream.Stream;

public class ConstantPropagation extends BodyTransformer
        implements DataFlowAnalysis<FlowMap, Unit> {

    private static final ConstantPropagation INSTANCE = new ConstantPropagation();

    public static ConstantPropagation v() {
        return INSTANCE;
    }

    private static boolean isOutput = true;

    public static void setOutput(boolean isOutput) {
        ConstantPropagation.isOutput = isOutput;
    }

    private ConstantPropagation() {}

    // ---------- Data-flow analysis for constant propagation ----------
    @Override
    public boolean isForward() {
        return true;
    }

    @Override
    public FlowMap getEntryInitialFlow(Unit entry) {
        return newInitialFlow();
    }

    @Override
    public FlowMap newInitialFlow() {
        return new FlowMap();
    }

    @Override
    public FlowMap meet(FlowMap m1, FlowMap m2) {
        FlowMap result = newInitialFlow();
        Stream.concat(m1.keySet().stream(), m2.keySet().stream())
                .distinct()
                .forEach(k -> result.put(k, meetValue(m1.get(k), m2.get(k))));
        return result;
    }

    /**
     * Meets two Values.
     */
    Value meetValue(Value v1, Value v2) {
        if (v1.isUndef() && v2.isConstant()) {
            return v2;
        } else if (v1.isConstant() && v2.isUndef()) {
            return v1;
        } else if (v1.isNAC() || v2.isNAC()) {
            return Value.getNAC();
        } else if (v1.equals(v2)) {
            return v1;
        } else {
            return Value.getNAC();
        }
    }

                                                                                                                                @Override
    public boolean transfer(Unit node, FlowMap in, FlowMap out) {
        boolean changed = false;
        if (node instanceof DefinitionStmt) {
            Local lhs = (Local) ((DefinitionStmt) node).getLeftOp();
            soot.Value rhs = ((DefinitionStmt) node).getRightOp();
            for (Local inLocal : in.keySet()) {
                if (!inLocal.equals(lhs)) {
                    changed |= out.update(inLocal, in.get(inLocal));
                }
            }
            changed |= out.update(lhs, computeValue(rhs, in));
        } else {
            changed = out.copyFrom(in);
        }
        return changed;
    }

    /**
     * Computes value of a RHS expression
     * @param rhs the RHS expression
     * @param in in-flow of the statement
     * @return the value of the RHS expression
     */
    public Value computeValue(soot.Value rhs, FlowMap in) {
        if (rhs instanceof Local) {
            return in.get(rhs);
        } else if (rhs instanceof IntConstant) {
            int value = ((IntConstant) rhs).value;
            return Value.makeConstant(value);
        } else if (rhs instanceof BinopExpr) {
            BinopExpr expr = (BinopExpr) rhs;
            Value op1 = computeValue(expr.getOp1(), in);
            Value op2 = computeValue(expr.getOp2(), in);
            if (op1.isConstant() && op2.isConstant()) {
                int i1 = op1.getConstant();
                int i2 = op2.getConstant();
                int res;
                if (expr instanceof AddExpr) {
                    res = i1 + i2;
                } else if (expr instanceof SubExpr) {
                    res = i1 - i2;
                } else if (expr instanceof MulExpr) {
                    res = i1 * i2;
                } else if (expr instanceof DivExpr) {
                    res = i1 / i2;
                }
                // for boolean expression
                else if (expr instanceof EqExpr) {
                    res = i1 == i2 ? 1 : 0;
                } else if (expr instanceof NeExpr) {
                    res = i1 != i2 ? 1 : 0;
                } else if (expr instanceof GeExpr) {
                    res = i1 >= i2 ? 1 : 0;
                } else if (expr instanceof GtExpr) {
                    res = i1 > i2 ? 1 : 0;
                } else if (expr instanceof LeExpr) {
                    res = i1 <= i2 ? 1 : 0;
                } else if (expr instanceof LtExpr) {
                    res = i1 < i2 ? 1 : 0;
                }
                else {
                    throw new UnsupportedOperationException(expr + " is not supported");
                }
                return Value.makeConstant(res);
            } else if (op1.isNAC() || op2.isNAC()) {
                return Value.getNAC();
            } else {
                return Value.getUndef();
            }
        } else {
            // Returns NAC for other non-supported expressions
            return Value.getNAC();
        }
    }

    // ---------- Body transformer ----------
    @Override
    protected void internalTransform(Body b, String phaseName, Map<String, String> options) {
        DirectedGraph<Unit> cfg = new BriefUnitGraph(b);
        Solver<FlowMap, Unit> solver = SolverFactory.v().newSolver(this, cfg);
        solver.solve();
        b.addTag(new DataFlowTag<>("ConstantTag", solver.getAfterFlow()));
        if (ResultChecker.isAvailable()) {
            ResultChecker.get().compare(b, solver.getAfterFlow());
        }
        if (isOutput) {
            outputResult(b, solver.getAfterFlow());
        }
    }

    synchronized void outputResult(Body body, Map<Unit, FlowMap> result) {
        System.out.println("------ " + body.getMethod() + " [constant propagation] -----");
        BriefUnitPrinter up = new BriefUnitPrinter(body);
        body.getUnits().forEach(u ->
                System.out.println(JimpleUtils.unitToString(up, u)
                        + ": " + result.get(u)));
    }
}
