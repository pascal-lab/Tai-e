package pascal.taie.frontend.newfrontend;

import fj.Class;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.StringLiteral;
import pascal.taie.World;
import pascal.taie.frontend.newfrontend.exposed.WorldParaHolder;
import pascal.taie.ir.exp.ArithmeticExp;
import pascal.taie.ir.exp.BitwiseExp;
import pascal.taie.ir.exp.ConditionExp;
import pascal.taie.ir.exp.DoubleLiteral;
import pascal.taie.ir.exp.FloatLiteral;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.LongLiteral;
import pascal.taie.ir.exp.MethodType;
import pascal.taie.ir.exp.ShiftExp;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.MethodNames;
import pascal.taie.language.classes.Signatures;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.VoidType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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

    public static final String META_FACTORY_CLASS = "java.lang.invoke.LambdaMetafactory";
    public static final String META_FACTORY_METHOD = "metafactory";

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

    /**
     * get tai-e representation of a JDT ITypeBinding
     * @param typeBinding binding to be converted, may not be erased
     * @return tai-e representation of {@code typeBinding}
     */
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
            throw new NewFrontendException("JDTTypeToTaieType: " + typeBinding.getName() + " is Not Implemented.");
        }
    }

    public static JClass getTaieClass(ITypeBinding binding) {
        return World.get().getClassHierarchy().getClass(binding.getErasure().getBinaryName());
    }

    public static Literal getRightPrimitiveLiteral(Expression e) {
        var t = e.resolveTypeBinding();
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
            } else if (res instanceof Character c) {
                return IntLiteral.get((int) c);
            } else if (res instanceof Boolean b) {
                return IntLiteral.get(b ? 1 : 0);
            } else {
                throw new NewFrontendException(e + " is not primitive literal, why use this function?");
            }
        }
    }

    public static Literal getStringLiteral(StringLiteral l) {
        return pascal.taie.ir.exp.StringLiteral.get(l.getLiteralValue());
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

    public static Type anyException() {
        JClass thr = WorldParaHolder.getClassHierarchy().getClass(ClassNames.THROWABLE);
        if (thr != null) {
            return thr.getType();
        } else {
            throw new NewFrontendException("can't get [Throwable] Type");
        }
    }

    /**
     * Extract Tai-e MethodType from JDT ITypeBinding
     * @param typeBinding should be a function interface
     * @return Tai-e MethodType correspond to the method in {@code typeBinding}
     */
    public static MethodType extractFuncInterface(ITypeBinding typeBinding) {
        IMethodBinding binding = typeBinding.getFunctionalInterfaceMethod();
        return getMethodType(binding);
    }

    public static MethodType getMethodType(IMethodBinding binding) {
        assert (binding != null);

        Type retType = JDTTypeToTaieType(binding.getReturnType());
        List<Type> paraType = Arrays.stream(binding.getParameterTypes())
                .map(TypeUtils::JDTTypeToTaieType)
                .toList();
        return MethodType.get(paraType, retType);
    }

    public static MethodRef getMetaFactory() {
        var meta = World.get()
                .getClassHierarchy()
                .getJREClass(META_FACTORY_CLASS);
        assert meta != null;
        var method = meta.getDeclaredMethod(META_FACTORY_METHOD);
        assert method != null;
        return method.getRef();
    }

    public static MethodRef getNewStringBuilder() {
        return getInitMethodRef(ClassNames.STRING_BUILDER, new ArrayList<>());
    }

    public static MethodRef getInitMethodRef(String name, List<Type> param) {
        JClass sb = World.get()
                .getClassHierarchy()
                .getJREClass(name);
        assert sb != null;
        JMethod method = sb.getDeclaredMethod(
                Subsignature.get(MethodNames.INIT, param, VoidType.VOID)
        );
        assert method != null;
        return method.getRef();
    }

    public static ClassType getStringBuilder() {
        JClass sb = World.get()
                .getClassHierarchy()
                .getJREClass(ClassNames.STRING_BUILDER);
        assert sb != null;
        return sb.getType();
    }

    public static MethodRef getStringBuilderAppend(Type argType) {
        JClass sb = World.get()
                .getClassHierarchy()
                .getJREClass(ClassNames.STRING_BUILDER);
        assert sb != null;
        List<Type> param = new ArrayList<>();
        if (! (argType instanceof PrimitiveType)) {
            // in fact, it can't be null if no error occur
            argType = Objects.requireNonNull(World.get().getClassHierarchy()
                    .getJREClass(ClassNames.OBJECT)).getType();
        }
        param.add(argType);
        JMethod method = sb.getDeclaredMethod(
                Subsignature.get("append", param, sb.getType()));
        assert method != null;
        return method.getRef();
    }

    public static MethodRef getToString() {
        JClass obj = World.get().getClassHierarchy().getJREClass(ClassNames.OBJECT);
        assert obj != null;
        JMethod method = obj.getDeclaredMethod("toString");
        assert method != null;
        return method.getRef();
    }

    public static MethodRef getSimpleJREMethod(String className, String method) {
        JClass obj = World.get().getClassHierarchy().getJREClass(className);
        assert obj != null;
        JMethod jmethod = obj.getDeclaredMethod(method);
        assert jmethod != null;
        return jmethod.getRef();
    }

    public static MethodRef getJREMethod(String className, String method, List<Type> paramType, Type retType) {
        JClass obj = World.get().getClassHierarchy().getJREClass(className);
        assert obj != null;
        JMethod jmethod = obj.getDeclaredMethod(
                Subsignature.get(method, paramType, retType)
        );
        assert jmethod != null;
        return jmethod.getRef();
    }

    public static String getRefNameOfPrimitive(String t) {
        return switch (t) {
            case JDT_BOOLEAN -> ClassNames.BOOLEAN;
            case JDT_CHAR    -> ClassNames.CHARACTER;
            case JDT_INT     -> ClassNames.INTEGER;
            case JDT_BYTE    -> ClassNames.BYTE;
            case JDT_SHORT   -> ClassNames.SHORT;
            case JDT_LONG    -> ClassNames.LONG;
            case JDT_FLOAT   -> ClassNames.FLOAT;
            case JDT_DOUBLE  -> ClassNames.DOUBLE;
            default -> throw new NewFrontendException(t + " is not primitive, why use this function?");
        };
    }

    public static int getIndexOfPrimitive(PrimitiveType t) {
        return switch (t) {
            case BOOLEAN -> 0;
            case CHAR -> 1;
            case BYTE -> 2;
            case SHORT -> 3;
            case INT -> 4;
            case LONG -> 5;
            case FLOAT -> 6;
            case DOUBLE -> 7;
        };
    }

    public static int getIndexOfPrimitive(Type t) {
        if (t instanceof PrimitiveType p) {
            return getIndexOfPrimitive(p);
        } else {
            return switch (t.getName()) {
                case ClassNames.BOOLEAN -> 0;
                case ClassNames.CHARACTER -> 1;
                case ClassNames.BYTE -> 2;
                case ClassNames.SHORT -> 3;
                case ClassNames.INTEGER -> 4;
                case ClassNames.LONG -> 5;
                case ClassNames.FLOAT -> 6;
                case ClassNames.DOUBLE -> 7;
                default -> throw new NewFrontendException(t + " is not primitive type, why use this function?");
            };
        }
    }

    public static PrimitiveType getPrimitiveByIndex(int i) {
        return switch (i) {
            case 0 -> PrimitiveType.BOOLEAN;
            case 1 -> PrimitiveType.CHAR;
            case 2 -> PrimitiveType.BYTE;
            case 3 -> PrimitiveType.SHORT;
            case 4 -> PrimitiveType.INT;
            case 5 -> PrimitiveType.LONG;
            case 6 -> PrimitiveType.FLOAT;
            case 7 -> PrimitiveType.DOUBLE;
            default -> throw new NewFrontendException(i + " is not legal primitive index");
        };
    }

    public static PrimitiveType getWidenType(PrimitiveType type) {
        if (getIndexOfPrimitive(type) < getIndexOfPrimitive(PrimitiveType.INT)) {
            return PrimitiveType.INT;
        } else {
            return type;
        }
    }

    public static PrimitiveType getPrimitiveByRef(Type t) {
        return getPrimitiveByIndex(getIndexOfPrimitive(t));
    }
}
