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

package pascal.taie.analysis.dataflow.analysis.availexp;

import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.InstanceOfExp;
import pascal.taie.ir.exp.UnaryExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.Hashes;

/**
 * Expression wrapper, which tests equality and computes hashcode by Exp contents.
 *
 * @see AvailableExpression
 */
public class ExpWrapper {

    private final Exp exp;

    ExpWrapper(Exp exp) {
        this.exp = exp;
    }

    public Exp get() {
        return exp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ExpWrapper that = (ExpWrapper) o;
        return equals(exp, that.exp);
    }

    private static boolean equals(Exp exp1, Exp exp2) {
        if (exp1 == exp2) {
            return true;
        }
        if (exp1.getClass() != exp2.getClass()) {
            return false;
        }
        if (exp1 instanceof Var) {
            // In Tai-e IR, Vars are canonicalized, thus we can directly
            // use its equals() to test equality.
            return exp1.equals(exp2);
        } else if (exp1 instanceof BinaryExp) {
            return equals((BinaryExp) exp1, (BinaryExp) exp2);
        } else if (exp1 instanceof CastExp) {
            return equals((CastExp) exp1, (CastExp) exp2);
        } else if (exp1 instanceof InstanceOfExp) {
            return equals((InstanceOfExp) exp1, (InstanceOfExp) exp2);
        } else if (exp1 instanceof UnaryExp) {
            return equals((UnaryExp) exp1, (UnaryExp) exp2);
        }
        throw new AnalysisException(exp1 + " is irrelevant to" +
                " available expression analysis");
    }

    private static boolean equals(BinaryExp binary1, BinaryExp binary2) {
        return binary1.getOperator().equals(binary2.getOperator()) &&
                binary1.getOperand1().equals(binary2.getOperand1()) &&
                binary1.getOperand2().equals(binary2.getOperand2());
    }

    private static boolean equals(CastExp cast1, CastExp cast2) {
        return cast1.getCastType().equals(cast2.getCastType()) &&
                cast1.getValue().equals(cast2.getValue());
    }

    private static boolean equals(InstanceOfExp instanceOf1, InstanceOfExp instanceOf2) {
        return instanceOf1.getCheckedType().equals(instanceOf2.getCheckedType()) &&
                instanceOf1.getValue().equals(instanceOf2.getValue());
    }

    private static boolean equals(UnaryExp unary1, UnaryExp unary2) {
        return unary1.getOperand().equals(unary2.getOperand());
    }

    @Override
    public int hashCode() {
        return hashCode(exp);
    }

    private static int hashCode(Exp exp) {
        if (exp instanceof Var) {
            return exp.hashCode();
        } else if (exp instanceof BinaryExp) {
            return hashCode((BinaryExp) exp);
        } else if (exp instanceof CastExp) {
            return hashCode((CastExp) exp);
        } else if (exp instanceof InstanceOfExp) {
            return hashCode((InstanceOfExp) exp);
        } else if (exp instanceof UnaryExp) {
            return hashCode((UnaryExp) exp);
        }
        throw new AnalysisException(exp + " is irrelevant to" +
                " available expression analysis");
    }

    private static int hashCode(BinaryExp binary) {
        return Hashes.hash(binary.getOperator(),
                binary.getOperand1(), binary.getOperand2());
    }

    private static int hashCode(CastExp cast) {
        return Hashes.hash(cast.getCastType(), cast.getValue());
    }

    private static int hashCode(InstanceOfExp instanceOf) {
        return Hashes.hash(instanceOf.getCheckedType(),
                instanceOf.getValue());
    }

    private static int hashCode(UnaryExp unary) {
        return Hashes.hash(unary.getClass(), unary.getOperand());
    }

    @Override
    public String toString() {
        return exp.toString();
    }
}
