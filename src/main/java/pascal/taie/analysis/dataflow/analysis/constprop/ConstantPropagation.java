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
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.Edge;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.BitwiseExp;
import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.ShiftExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.DefinitionStmt;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.SwitchStmt;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.util.AnalysisException;

/**
 * Implementation of constant propagation for int values.
 */
public class ConstantPropagation extends
        AbstractDataflowAnalysis<Stmt, CPFact> {

    public static final String ID = "constprop";

    /**
     * Whether enable refinement on lattice value via edge transfer.
     */
    private final boolean edgeRefine;

    public ConstantPropagation(AnalysisConfig config) {
        super(config);
        edgeRefine = getOptions().getBooleanOrDefault("edge-refine", true);
    }

    @Override
    public boolean isForward() {
        return true;
    }

    @Override
    public CPFact newBoundaryFact(CFG<Stmt> cfg) {
        return newBoundaryFact(cfg.getIR());
    }

    public CPFact newBoundaryFact(IR ir) {
        // make conservative assumption about parameters: assign NAC to them
        CPFact entryFact = new CPFact();
        ir.getParams()
                .stream()
                .filter(ConstantPropagation::canHoldInt)
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
                return canHoldInt(lhs) ?
                        out.update(lhs, evaluate(rhs, in)) || changed :
                        changed;
            }
        }
        return out.copyFrom(in);
    }

    /**
     * @return true if the given variable can hold integer value, otherwise false.
     */
    public static boolean canHoldInt(Var var) {
        Type type = var.getType();
        if (type instanceof PrimitiveType) {
            switch ((PrimitiveType) type) {
                case BYTE:
                case SHORT:
                case INT:
                case CHAR:
                case BOOLEAN:
                    return true;
            }
        }
        return false;
    }

    /**
     * Evaluates the {@link Value} of given expression.
     *
     * @param exp the expression to be evaluated
     * @param in  IN fact of the statement
     * @return the resulting {@link Value}
     */
    public static Value evaluate(Exp exp, CPFact in) {
        if (exp instanceof IntLiteral) {
            return Value.makeConstant(((IntLiteral) exp).getValue());
        } else if (exp instanceof Var var) {
            // treat the values of non-int variables as NAC
            return canHoldInt(var) ? in.get(var) : Value.getNAC();
        } else if (exp instanceof BinaryExp binary) {
            BinaryExp.Op op = binary.getOperator();
            Value v1 = evaluate(binary.getOperand1(), in);
            Value v2 = evaluate(binary.getOperand2(), in);
            // handle division-by-zero by returning UNDEF
            if ((op == ArithmeticExp.Op.DIV || op == ArithmeticExp.Op.REM) &&
                    v2.isConstant() && v2.getConstant() == 0) {
                return Value.getUndef();
            }
            if (v1.isConstant() && v2.isConstant()) {
                int i1 = v1.getConstant();
                int i2 = v2.getConstant();
                return Value.makeConstant(evaluate(op, i1, i2));
            } else if (v1.isNAC() || v2.isNAC()) {
                return Value.getNAC();
            }
            return Value.getUndef();
        }
        // return NAC for other cases
        return Value.getNAC();
    }

    private static int evaluate(BinaryExp.Op op, int i1, int i2) {
        if (op instanceof ArithmeticExp.Op) {
            return switch ((ArithmeticExp.Op) op) {
                case ADD -> i1 + i2;
                case SUB -> i1 - i2;
                case MUL -> i1 * i2;
                case DIV -> i1 / i2;
                case REM -> i1 % i2;
            };
        } else if (op instanceof BitwiseExp.Op) {
            return switch ((BitwiseExp.Op) op) {
                case OR -> i1 | i2;
                case AND -> i1 & i2;
                case XOR -> i1 ^ i2;
            };
        } else if (op instanceof ConditionExp.Op) {
            return switch ((ConditionExp.Op) op) {
                case EQ -> i1 == i2 ? 1 : 0;
                case NE -> i1 != i2 ? 1 : 0;
                case LT -> i1 < i2 ? 1 : 0;
                case GT -> i1 > i2 ? 1 : 0;
                case LE -> i1 <= i2 ? 1 : 0;
                case GE -> i1 >= i2 ? 1 : 0;
            };
        } else if (op instanceof ShiftExp.Op) {
            return switch ((ShiftExp.Op) op) {
                case SHL -> i1 << i2;
                case SHR -> i1 >> i2;
                case USHR -> i1 >>> i2;
            };
        }
        throw new AnalysisException("Unexpected op: " + op);
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

    /**
     * Joins two Values.
     * This method computes the least upper bound of two Values.
     */
    private Value joinValue(Value v1, Value v2) {
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
