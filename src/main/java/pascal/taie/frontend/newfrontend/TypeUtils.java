package pascal.taie.frontend.newfrontend;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.NullLiteral;
import pascal.taie.frontend.newfrontend.exposed.WorldParaHolder;
import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.BitwiseExp;
import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.DoubleLiteral;
import pascal.taie.ir.exp.FloatLiteral;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.LongLiteral;
import pascal.taie.ir.exp.ShiftExp;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.VoidType;

public final class TypeUtils {

    public static final String JDT_INT = "int";
    public static final String JDT_BYTE = "byte";
    public static final String JDT_SHORT = "short";
    public static final String JDT_LONG = "long";
    public static final String JDT_FLOAT = "float";
    public static final String JDT_DOUBLE = "double";
    public static final String JDT_CHAR = "char";
    public static final String JDT_BOOLEAN = "boolean";
    public static final String JDT_VOID = "void";

    public static String getErasedName(ITypeBinding iTypeBinding) {
        if (iTypeBinding.isPrimitive()) {
            return iTypeBinding.getName();
        } else if (iTypeBinding.isArray()) {
            return getErasedName(iTypeBinding.getComponentType()) + "[]";
        }
        return iTypeBinding.getErasure().getBinaryName();
    }

    /**
     * get Erased Signature of a method,
     * equal to {@code SootMethod::getSubSignature}
     */
    public static String getErasedSignature(
            ITypeBinding[] paramType,
            ITypeBinding returnType,
            String methodName)
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(getErasedName(returnType));
        stringBuilder.append(" ");
        stringBuilder.append(methodName);
        stringBuilder.append("(");
        if (paramType.length > 0) {
            stringBuilder.append(getErasedName(paramType[0]));
            for (var i = 1; i < paramType.length; ++i) {
                stringBuilder.append(",");
                stringBuilder.append(getErasedName(paramType[i]));
            }
        }
        stringBuilder.append(")");
        return stringBuilder.toString();
    }

    /**
     * <p>check if {@code signature} is a subsignature of {@code iMethodBinding}</p>
     * <p>{@code signature} need to be erased</p>
     */
    public static boolean isSubSignature(
            String signature, IMethodBinding iMethodBinding)
    {
        var signature2 = getErasedSignature(iMethodBinding.getParameterTypes(),
                iMethodBinding.getReturnType(),
                iMethodBinding.getName());
        return signature.equals(signature2);
    }

    public static Type JDTTypeToTaieType(ITypeBinding typeBinding)  {
        if (typeBinding.isPrimitive()) {
            return switch (typeBinding.getName()) {
                case JDT_BYTE -> PrimitiveType.BYTE;
                case JDT_SHORT -> PrimitiveType.SHORT;
                case JDT_INT -> PrimitiveType.INT;
                case JDT_LONG -> PrimitiveType.LONG;
                case JDT_CHAR -> PrimitiveType.CHAR;
                case JDT_DOUBLE -> PrimitiveType.DOUBLE;
                case JDT_FLOAT -> PrimitiveType.FLOAT;
                case JDT_BOOLEAN -> PrimitiveType.BOOLEAN;
                case JDT_VOID -> VoidType.VOID;
                default -> throw new NewFrontendException("Primitive Type Illegal: " + typeBinding.getName());
            };
        } else if (typeBinding.isNullType()) {
            return NullType.NULL;
        } else {
            var erased = typeBinding.getErasure();
            var tm = WorldParaHolder.getTypeSystem();
            var loader = WorldParaHolder.getClassLoader();
            if (erased.isClass() || erased.isInterface()) {
                return tm.getType(loader, erased.getBinaryName());
            } else if (erased.isArray()) {
                return tm.getArrayType(JDTTypeToTaieType(erased.getElementType()), erased.getDimensions());
            }
            throw new NewFrontendException("JDTTypeToTaieType:" + typeBinding.getName() + "is Not Implemented.");
        }
    }

    public static Literal getRightPrimitiveLiteral(Expression e) {
        var res = e.resolveConstantExpressionValue();
        if (res == null) {
            if (e instanceof NullLiteral) {
                return pascal.taie.ir.exp.NullLiteral.get();
            } else {
                throw new NewFrontendException(e + " is not literal, why use this function?");
            }
        } else {
            if (res instanceof Integer i) {
                return IntLiteral.get(i);
            } else if (res instanceof Long l) {
                return LongLiteral.get(l);
            } else if (res instanceof Float f) {
                return FloatLiteral.get(f);
            } else if (res instanceof Double d) {
                return DoubleLiteral.get(d);
            } else {
                throw new NewFrontendException(e + " is not primitive literal, why use this function?");
            }
        }
    }

    public static ArithmeticExp.Op getArithmeticOp(InfixExpression.Operator op) {
        return switch (op.toString()) {
            case "+" -> ArithmeticExp.Op.ADD;
            case "-" -> ArithmeticExp.Op.SUB;
            case "/" -> ArithmeticExp.Op.DIV;
            case "*" -> ArithmeticExp.Op.MUL;
            case "%" -> ArithmeticExp.Op.REM;
            default -> throw new NewFrontendException(op + " is not arithmetic Op, why use this function?");
        };
    }

    public static ShiftExp.Op getShiftOp(InfixExpression.Operator op) {
        return switch (op.toString()) {
            case ">>" -> ShiftExp.Op.SHR;
            case "<<" -> ShiftExp.Op.SHL;
            case ">>>"-> ShiftExp.Op.USHR;
            default -> throw new NewFrontendException(op + " is not shift Op, why use this function?");
        };
    }

    public static BitwiseExp.Op getBitwiseOp(InfixExpression.Operator op) {
        return switch (op.toString()) {
            case "|" -> BitwiseExp.Op.OR;
            case "&" -> BitwiseExp.Op.AND;
            case "^" -> BitwiseExp.Op.XOR;
            default -> throw new NewFrontendException(op + " is not Bitwise Op, why use this function?");
        };
    }

    public static ConditionExp.Op getConditionOp(InfixExpression.Operator op) {
        return switch (op.toString()) {
            case ">" -> ConditionExp.Op.GT;
            case ">=" -> ConditionExp.Op.GE;
            case "==" -> ConditionExp.Op.EQ;
            case "<=" -> ConditionExp.Op.LE;
            case "<" -> ConditionExp.Op.LT;
            case "!=" -> ConditionExp.Op.NE;
            default -> throw new NewFrontendException(op + " is not Condition OP, why use this function?");
        };
    }
}
