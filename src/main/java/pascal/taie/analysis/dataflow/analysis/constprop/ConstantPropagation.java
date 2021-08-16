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
import pascal.taie.analysis.dataflow.fact.MapFact;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.analysis.graph.cfg.Edge;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.AbstractBinaryExp;
import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.BitwiseExp;
import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.ExpVisitor;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.ShiftExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.DefinitionStmt;
import pascal.taie.ir.stmt.If;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.SwitchStmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.util.AnalysisException;

public class ConstantPropagation extends
        AbstractDataflowAnalysis<Stmt, MapFact<Var, Value>> {

    public static final String ID = "constprop";

    public ConstantPropagation(AnalysisConfig config) {
        super(config);
    }

    @Override
    public boolean isForward() {
        return true;
    }

    @Override
    public MapFact<Var, Value> newBoundaryFact(CFG<Stmt> cfg) {
        return newBoundaryFact(cfg.getMethod());
    }

    public MapFact<Var, Value> newBoundaryFact(JMethod method) {
        // Make conservative assumption about parameters: assign NAC to them
        CPFact entryFact = new CPFact();
        IR ir = method.getIR();
        ir.getParams()
                .stream()
                .filter(this::isInt)
                .forEach(p -> entryFact.update(p, Value.getNAC()));
        // TODO: explicitly initialize all non-param variables as UNDEF?
//        ir.getVars()
//                .stream()
//                .filter(v -> !v.equals(thisVar) && !ir.getParams().contains(v))
//                .forEach(v -> entryFact.update(v, Value.getUndef()));
        return entryFact;
    }

    @Override
    public MapFact<Var, Value> newInitialFact() {
        return new CPFact();
    }

    @Override
    public void mergeInto(MapFact<Var, Value> fact, MapFact<Var, Value> result) {
        fact.forEach((var, value) ->
                result.update(var, meetValue(value, result.get(var))));
    }

    /**
     * Meets two Values.
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
    public boolean transferNode(Stmt stmt, MapFact<Var, Value> in, MapFact<Var, Value> out) {
        if (stmt instanceof DefinitionStmt<?, ?>) {
            Exp lvalue = ((DefinitionStmt<?, ?>) stmt).getLValue();
            if (lvalue instanceof Var) {
                Var lhs = (Var) lvalue;
                Exp rhs = ((DefinitionStmt<?, ?>) stmt).getRValue();
                boolean changed = false;
                for (Var inVar : in.keySet()) {
                    if (!inVar.equals(lhs)) {
                        changed |= out.update(inVar, in.get(inVar));
                    }
                }
                return isInt(rhs) ?
                        out.update(lhs, evaluate(rhs, in)) || changed :
                        changed;
            }
        }
        return out.copyFrom(in);
    }

    /**
     * @return if given expression is of integer type.
     */
    public boolean isInt(Exp exp) {
        return exp.getType().equals(PrimitiveType.INT);
    }

    public static Value evaluate(Exp exp, MapFact<Var, Value> env) {
        return exp.accept(new Evaluator(env));
    }

    private static class Evaluator implements ExpVisitor<Value> {

        private final MapFact<Var, Value> env;

        private Evaluator(MapFact<Var, Value> env) {
            this.env = env;
        }

        @Override
        public Value visit(Var var) {
            return env.get(var);
        }

        @Override
        public Value visit(IntLiteral literal) {
            return Value.makeConstant(literal.getValue());
        }

        /**
         * Evaluator for binary expressions with constant operands.
         */
        @FunctionalInterface
        private interface ConstantEval {
            int eval(BinaryExp.Op op, int i1, int i2);
        }

        @Override
        public Value visit(ArithmeticExp exp) {
            return evaluateBinary(exp, (op, i1, i2) -> {
                switch ((ArithmeticExp.Op) op) {
                    case ADD: return i1 + i2;
                    case SUB: return i1 - i2;
                    case MUL: return i1 * i2;
                    case DIV: return i1 / i2;
                    case REM: return i1 % i2;
                }
                throw new AnalysisException("Unexpected op: " + op);
            });
        }

        @Override
        public Value visit(BitwiseExp exp) {
            return evaluateBinary(exp, (op, i1, i2) -> {
                switch ((BitwiseExp.Op) op) {
                    case OR: return i1 | i2;
                    case AND: return i1 & i2;
                    case XOR: return i1 ^ i2;
                }
                throw new AnalysisException("Unexpected op: " + op);
            });
        }

        @Override
        public Value visit(ConditionExp exp) {
            return evaluateBinary(exp, (op, i1, i2) -> {
                switch ((ConditionExp.Op) op) {
                    case EQ: return  i1 == i2 ? 1 : 0;
                    case NE: return  i1 != i2 ? 1 : 0;
                    case LT: return  i1 < i2 ? 1 : 0;
                    case GT: return  i1 > i2 ? 1 : 0;
                    case LE: return  i1 <= i2 ? 1 : 0;
                    case GE: return  i1 >= i2 ? 1 : 0;
                }
                throw new AnalysisException("Unexpected op: " + op);
            });
        }

        @Override
        public Value visit(ShiftExp exp) {
            return evaluateBinary(exp, (op, i1, i2) -> {
                switch ((ShiftExp.Op) op) {
                    case SHL: return i1 << i2;
                    case SHR: return i2 >> i2;
                    case USHR: return i1 >>> i2;
                }
                throw new AnalysisException("Unexpected op: " + op);
            });
        }

        private Value evaluateBinary(AbstractBinaryExp binary,
                                     ConstantEval constantEval) {
            Value v1 = binary.getValue1().accept(this);
            Value v2 = binary.getValue2().accept(this);
            if (v1.isConstant() && v2.isConstant()) {
                int i1 = v1.getConstant();
                int i2 = v2.getConstant();
                return Value.makeConstant(
                        constantEval.eval(binary.getOperator(), i1, i2));
            } else if (v1.isNAC() || v2.isNAC()) {
                return Value.getNAC();
            }
            return Value.getUndef();
        }

        @Override
        public Value visitDefault(Exp exp) {
            return Value.getNAC();
        }
    }

    @Override
    public boolean hasEdgeTransfer() {
        return true;
    }

    @Override
    public void transferEdge(
            Edge<Stmt> edge, MapFact<Var, Value> nodeFact, MapFact<Var, Value> edgeFact) {
        edgeFact.copyFrom(nodeFact);
        if (edge.getKind() == Edge.Kind.IF_TRUE) {
            ConditionExp cond = ((If) edge.getSource()).getCondition();
            if (cond.getOperator() == ConditionExp.Op.EQ) {
                // if (x == 1) {
                //   ... <- x must be 1 at this branch
                Var v1 = cond.getValue1();
                Value val1 = nodeFact.get(v1);
                Var v2 = cond.getValue2();
                Value val2 = nodeFact.get(v2);
                edgeFact.update(v1, val1.restrictTo(val2));
                edgeFact.update(v2, val2.restrictTo(val1));
            }
        } else if (edge.getKind() == Edge.Kind.SWITCH_CASE) {
            // switch (x) {
            //   case 1: ... <- x must be 1 at this branch
            Var var = ((SwitchStmt) edge.getSource()).getValue();
            int caseValue = edge.getCaseValue();
            edgeFact.update(var, Value.makeConstant(caseValue));
        }
    }
}
