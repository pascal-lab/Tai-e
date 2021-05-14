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

package pascal.taie.analysis.dfa.analysis.constprop;

import pascal.taie.analysis.dfa.analysis.AbstractDataflowAnalysis;
import pascal.taie.analysis.dfa.fact.MapFact;
import pascal.taie.analysis.graph.cfg.CFG;
import pascal.taie.config.AnalysisConfig;
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
import pascal.taie.ir.stmt.Stmt;
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
    public MapFact<Var, Value> getEntryInitialFact(CFG<Stmt> cfg) {
        // Make conservative assumption about parameters: assign NAC to them
        CPFact entryFact = new CPFact();
        cfg.getIR().getParams().forEach(p ->
                entryFact.update(p, Value.getNAC()));
        Var thisVar = cfg.getIR().getThis();
        if (thisVar != null) {
            entryFact.update(thisVar, Value.getNAC());
        }
        return entryFact;
    }

    @Override
    public MapFact<Var, Value> newInitialFact() {
        return new CPFact();
    }

    @Override
    public MapFact<Var, Value> copyFact(MapFact<Var, Value> fact) {
        return fact.duplicate();
    }

    @Override
    public void mergeInto(MapFact<Var, Value> fact, MapFact<Var, Value> result) {
        fact.forEach((var, value) ->
                result.update(var, meetValue(value, result.get(var))));
    }

    /**
     * Meets two Values.
     */
    private static Value meetValue(Value v1, Value v2) {
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
                return out.update(lhs, evaluate(rhs, in)) || changed;
            }
        }
        return out.copyFrom(in);
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

        @Override
        public Value visit(ArithmeticExp exp) {
            return evaluateBinary(exp);
        }

        @Override
        public Value visit(BitwiseExp exp) {
            return evaluateBinary(exp);
        }

        @Override
        public Value visit(ConditionExp exp) {
            return evaluateBinary(exp);
        }

        @Override
        public Value visit(ShiftExp exp) {
            return evaluateBinary(exp);
        }

        private Value evaluateBinary(AbstractBinaryExp binary) {
            Value v1 = binary.getValue1().accept(this);
            Value v2 = binary.getValue2().accept(this);
            if (v1.isConstant() && v2.isConstant()) {
                int i1 = v1.getConstant();
                int i2 = v2.getConstant();
                return Value.makeConstant(evaluate(binary.getOperator(), i1, i2));
            } else if (v1.isNAC() || v2.isNAC()) {
                return Value.getNAC();
            }
            return Value.getUndef();
        }

        private int evaluate(BinaryExp.Op op, int i1, int i2) {
            if (op instanceof ArithmeticExp.Op) {
                switch ((ArithmeticExp.Op) op) {
                    case ADD: return i1 + i2;
                    case SUB: return i1 - i2;
                    case MUL: return i1 * i2;
                    case DIV: return i1 / i2;
                    case REM: return i1 % i2;
                }
            } else if (op instanceof ConditionExp.Op) {
                switch ((ConditionExp.Op) op) {
                    case EQ: return  i1 == i2 ? 1 : 0;
                    case NE: return  i1 != i2 ? 1 : 0;
                    case LT: return  i1 < i2 ? 1 : 0;
                    case GT: return  i1 > i2 ? 1 : 0;
                    case LE: return  i1 <= i2 ? 1 : 0;
                    case GE: return  i1 >= i2 ? 1 : 0;
                }
            }
            else if (op instanceof ShiftExp.Op) {
                switch ((ShiftExp.Op) op) {
                    case SHL: return i1 << i2;
                    case SHR: return i2 >> i2;
                    case USHR: return i1 >>> i2;
                }
            } else if (op instanceof BitwiseExp.Op) {
                switch ((BitwiseExp.Op) op) {
                    case OR: return i1 | i2;
                    case AND: return i1 & i2;
                    case XOR: return i1 ^ i2;
                }
            }
            throw new AnalysisException("Unexpected binary operator: " + op);
        }

        @Override
        public Value visitDefault(Exp exp) {
            return Value.getNAC();
        }
    }
}
