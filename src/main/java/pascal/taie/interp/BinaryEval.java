package pascal.taie.interp;

import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.BitwiseExp;
import pascal.taie.ir.exp.ComparisonExp;
import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.ShiftExp;
import pascal.taie.language.type.PrimitiveType;

import java.util.function.BiFunction;

public class BinaryEval {
    public static JValue evalBinary(BinaryExp.Op op, JValue v1, JValue v2) {
        if (op instanceof ComparisonExp.Op) {
            if (v1 instanceof JPrimitive l1 && v2 instanceof JPrimitive l2) {
                return JPrimitive.getBoolean(l1.equals(l2));
            } else if (v1 instanceof JObject o1 && v2 instanceof JObject o2) {
                if (v1 instanceof JVMObject vmo1 && v2 instanceof JVMObject vmo2) {
                    return JPrimitive.getBoolean(vmo1.toJVMObj() == vmo2.toJVMObj());
                }
                return JPrimitive.getBoolean(o1 == o2);
            } else if (v1 instanceof JArray arr1 && v2 instanceof JArray arr2) {
                return JPrimitive.getBoolean(arr1 == arr2);
            } else {
                throw new InterpreterException();
            }
        }

        JPrimitive l1 = (JPrimitive) v1;
        JPrimitive l2 = (JPrimitive) v2;
        if (op instanceof ArithmeticExp.Op op1) {
            return evalArithmetic(op1, l1.value, l2.value);
        } else if (op instanceof ConditionExp.Op op1) {
            Integer i1 = JValue.getInt(v1);
            Integer i2 = JValue.getInt(v2);
            return JPrimitive.getBoolean(switch (op1) {
                case EQ -> i1.equals(i2);
                case GE -> i1 >= i2;
                case GT -> i1 > i2;
                case LE -> i1 <= i2;
                case LT -> i1 < i2;
                case NE -> ! i1.equals(i2);
            });
        } else if (op instanceof ShiftExp.Op op1) {
            if (l1.value instanceof Long) {
                long ll1 = JValue.getLong(v1);
                int i2 = JValue.getInt(v2);
                return JPrimitive.get(switch (op1) {
                    case SHL -> ll1 << i2;
                    case SHR -> ll1 >> i2;
                    case USHR -> ll1 >>> i2;
                });
            } else {
                int i1 = JValue.getInt(v1);
                int i2 = JValue.getInt(v2);
                return JPrimitive.get(switch (op1) {
                    case SHL -> i1 << i2;
                    case SHR -> i1 >> i2;
                    case USHR -> i1 >>> i2;
                });
            }
        } else if (op instanceof BitwiseExp.Op op1) {
            if (l1.value instanceof Long) {
                long ll1 = JValue.getLong(v1);
                long ll2 = JValue.getLong(v2);
                return JPrimitive.get(switch (op1) {
                    case OR -> ll1 | ll2;
                    case AND -> ll1 & ll2;
                    case XOR -> ll1 ^ ll2;
                });
            } else {
                int i1 = JValue.getInt(v1);
                int i2 = JValue.getInt(v2);
                return JPrimitive.get(switch (op1) {
                    case OR -> i1 | i2;
                    case AND -> i1 & i2;
                    case XOR -> i1 ^ i2;
                });
            }
        } else {
            throw new InterpreterException();
        }
    }

    public static JValue evalArithmetic(ArithmeticExp.Op op, Object v1, Object v2) {
        if (v1 instanceof Integer l1 && v2 instanceof Integer l2) {
            return new JPrimitive(switch (op) {
                case ADD -> l1 + l2;
                case DIV -> l1 / l2;
                case MUL -> l1 * l2;
                case REM -> l1 % l2;
                case SUB -> l1 - l2;
            });
        } else if (v1 instanceof Long l1 && v2 instanceof Long l2) {
            return new JPrimitive(switch (op) {
                case ADD -> l1 + l2;
                case SUB -> l1 - l2;
                case MUL -> l1 * l2;
                case DIV -> l1 / l2;
                case REM -> l1 % l2;
            });
        } else if (v1 instanceof Float f1 && v2 instanceof Float f2) {
            return new JPrimitive(switch (op) {
                case ADD -> f1 + f2;
                case SUB -> f1 - f2;
                case MUL -> f1 * f2;
                case DIV -> f1 / f2;
                case REM -> f1 % f2;
            });
        } else if (v1 instanceof Double f1 && v2 instanceof Double f2) {
            return new JPrimitive(switch (op) {
                case ADD -> f1 + f2;
                case SUB -> f1 - f2;
                case MUL -> f1 * f2;
                case DIV -> f1 / f2;
                case REM -> f1 % f2;
            });
        } else {
            throw new InterpreterException();
        }
    }
}
