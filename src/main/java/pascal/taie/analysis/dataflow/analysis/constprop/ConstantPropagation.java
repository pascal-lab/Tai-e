/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.dataflow.analysis.constprop;

import pascal.taie.analysis.dataflow.analysis.AbstractDataflowAnalysis;
import pascal.taie.analysis.dataflow.analysis.AnalysisDriver;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.CFGEdge;
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

    public static final String ID = "const-prop";

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
        public boolean needTransferEdge(CFGEdge<Stmt> edge) {
            if (edgeRefine) {
                return edge.source() instanceof If ||
                        edge.getKind() == CFGEdge.Kind.SWITCH_CASE;
            } else {
                return false;
            }
        }

        @Override
        public CPFact transferEdge(CFGEdge<Stmt> edge, CPFact nodeFact) {
            CFGEdge.Kind kind = edge.getKind();
            if (edge.source() instanceof If) {
                ConditionExp cond = ((If) edge.source()).getCondition();
                ConditionExp.Op op = cond.getOperator();
                if ((kind == CFGEdge.Kind.IF_TRUE && op == ConditionExp.Op.EQ) ||
                        (kind == CFGEdge.Kind.IF_FALSE && op == ConditionExp.Op.NE)) {
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
            } else if (kind == CFGEdge.Kind.SWITCH_CASE) {
                // switch (x) {
                //   case 1: ... <- x must be 1 at this branch
                Var var = ((SwitchStmt) edge.source()).getVar();
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
