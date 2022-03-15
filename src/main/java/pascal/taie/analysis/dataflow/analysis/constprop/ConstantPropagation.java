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

package pascal.taie.analysis.dataflow.analysis.constprop;

import pascal.taie.analysis.dataflow.analysis.AbstractDataflowAnalysis;
import pascal.taie.analysis.dataflow.analysis.AnalysisDriver;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.Edge;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.Exps;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.DefinitionStmt;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.SwitchStmt;

/**
 * Implementation of constant propagation for int values.
 */
public class ConstantPropagation extends AnalysisDriver<Stmt, CPFact> {

    public static final String ID = "constprop";

    public ConstantPropagation(AnalysisConfig config) {
        super(config);
    }

    @Override
    protected Analysis makeAnalysis(CFG<Stmt> cfg) {
        return new Analysis(cfg, getOptions().getBoolean("edge-refine"));
    }

    public static class Analysis extends AbstractDataflowAnalysis<Stmt, CPFact> {

        /**
         * Whether enable refinement on lattice value via edge transfer.
         */
        private final boolean edgeRefine;

        public Analysis(CFG<Stmt> cfg, boolean edgeRefine) {
            super(cfg);
            this.edgeRefine = edgeRefine;
        }

        @Override
        public boolean isForward() {
            return true;
        }

        @Override
        public CPFact newBoundaryFact() {
            return newBoundaryFact(cfg.getIR());
        }

        public CPFact newBoundaryFact(IR ir) {
            // make conservative assumption about parameters: assign NAC to them
            CPFact entryFact = newInitialFact();
            ir.getParams()
                    .stream()
                    .filter(Exps::holdsInt)
                    .forEach(p -> entryFact.update(p, Value.getNAC()));
            return entryFact;
        }

        @Override
        public CPFact newInitialFact() {
            return new CPFact();
        }

        @Override
        public void meetInto(CPFact fact, CPFact target) {
            fact.forEach((var, value) ->
                    target.update(var, meetValue(value, target.get(var))));
        }

        /**
         * Meets two Values.
         * This method computes the greatest lower bound of two Values.
         */
        public Value meetValue(Value v1, Value v2) {
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
        public boolean transferNode(Stmt stmt, CPFact in, CPFact out) {
            if (stmt instanceof DefinitionStmt) {
                Exp lvalue = ((DefinitionStmt<?, ?>) stmt).getLValue();
                if (lvalue instanceof Var lhs) {
                    Exp rhs = ((DefinitionStmt<?, ?>) stmt).getRValue();
                    boolean changed = false;
                    for (Var inVar : in.keySet()) {
                        if (!inVar.equals(lhs)) {
                            changed |= out.update(inVar, in.get(inVar));
                        }
                    }
                    return Exps.holdsInt(lhs) ?
                            out.update(lhs, Evaluator.evaluate(rhs, in)) || changed :
                            changed;
                }
            }
            return out.copyFrom(in);
        }

        @Override
        public boolean needTransferEdge(Edge<Stmt> edge) {
            if (edgeRefine) {
                return edge.getSource() instanceof If ||
                        edge.getKind() == Edge.Kind.SWITCH_CASE;
            } else {
                return false;
            }
        }

        @Override
        public CPFact transferEdge(Edge<Stmt> edge, CPFact nodeFact) {
            Edge.Kind kind = edge.getKind();
            if (edge.getSource() instanceof If) {
                ConditionExp cond = ((If) edge.getSource()).getCondition();
                ConditionExp.Op op = cond.getOperator();
                if ((kind == Edge.Kind.IF_TRUE && op == ConditionExp.Op.EQ) ||
                        (kind == Edge.Kind.IF_FALSE && op == ConditionExp.Op.NE)) {
                    // if (v1 == v2) {
                    //   ... <- v1 must equal to v2 at this branch
                    // if (v1 != v2) { ... } else {
                    //   ... <- v1 must equal to v2 at this branch
                    Var v1 = cond.getOperand1();
                    Value val1 = nodeFact.get(v1);
                    Var v2 = cond.getOperand2();
                    Value val2 = nodeFact.get(v2);
                    CPFact result = nodeFact.copy();
                    Value joined = joinValue(val1, val2);
                    result.update(v1, joined);
                    result.update(v2, joined);
                    return result;
                }
            } else if (kind == Edge.Kind.SWITCH_CASE) {
                // switch (x) {
                //   case 1: ... <- x must be 1 at this branch
                Var var = ((SwitchStmt) edge.getSource()).getVar();
                Value val = nodeFact.get(var);
                int caseValue = edge.getCaseValue();
                CPFact result = nodeFact.copy();
                result.update(var, joinValue(val, Value.makeConstant(caseValue)));
                return result;
            }
            return nodeFact;
        }
    }

    /**
     * Joins two Values.
     * This method computes the least upper bound of two Values.
     */
    private static Value joinValue(Value v1, Value v2) {
        if (v1.isNAC() && v2.isConstant()) {
            return v2;
        } else if (v1.isConstant() && v2.isNAC()) {
            return v1;
        } else if (v1.isUndef() || v2.isUndef()) {
            return Value.getUndef();
        } else if (v1.equals(v2)) {
            return v1;
        } else {
            return Value.getUndef();
        }
    }
}
