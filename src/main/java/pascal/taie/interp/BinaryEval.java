package pascal.taie.interp;

import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.Literal;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class BinaryEval {
    public static JValue evalBinary(BinaryExp.Op op, JValue v1, JValue v2) {
        JLiteral l1 = (JLiteral) v1;
        JLiteral l2 = (JLiteral) v2;
        if (op instanceof ArithmeticExp.Op op1) {
            return evalArithmetic(op1, l1.value, l2.value);
        } else {
            throw new IllegalStateException("halt");
        }
    }
    public static JValue evalArithmetic(ArithmeticExp.Op op, Literal v1, Literal v2) {
        if (v1 instanceof IntLiteral l1 && v2 instanceof IntLiteral l2) {
            return JLiteral.get(IntLiteral.get(
                    getFunc(op).apply(l1.getValue(), l2.getValue())));
        }
        throw new IllegalStateException("halt");
    }

    private static BiFunction<Integer, Integer, Integer> getFunc(BinaryExp.Op op) {
        if (op instanceof ArithmeticExp.Op op1) {
            switch (op1) {
                case ADD -> { return Integer::sum; }
                case DIV -> { return (a, b) -> a / b; }
                case MUL -> { return (a, b) -> a * b; }
                case REM -> { return (a, b) -> a % b; }
                case SUB -> { return (a, b) -> a - b; }
            }
        }
        throw new IllegalStateException("halt");
    }
}
