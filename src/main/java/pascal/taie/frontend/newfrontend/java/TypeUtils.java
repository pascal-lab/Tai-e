package pascal.taie.frontend.newfrontend.java;

import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.StringLiteral;
import pascal.taie.World;
import pascal.taie.frontend.newfrontend.BuildContext;
import pascal.taie.frontend.newfrontend.Utils;
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
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.annotation.AnnotationElement;
import pascal.taie.language.annotation.AnnotationHolder;
import pascal.taie.language.annotation.ArrayElement;
import pascal.taie.language.annotation.BooleanElement;
import pascal.taie.language.annotation.ClassElement;
import pascal.taie.language.annotation.DoubleElement;
import pascal.taie.language.annotation.Element;
import pascal.taie.language.annotation.EnumElement;
import pascal.taie.language.annotation.FloatElement;
import pascal.taie.language.annotation.IntElement;
import pascal.taie.language.annotation.LongElement;
import pascal.taie.language.annotation.StringElement;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.MethodNames;
import pascal.taie.language.classes.Modifier;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.language.type.VoidType;
import pascal.taie.util.collection.SetQueue;
import javax.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pascal.taie.frontend.newfrontend.java.JDTStringReps.getBinaryName;
import static pascal.taie.language.type.BooleanType.BOOLEAN;
import static pascal.taie.language.type.ByteType.BYTE;
import static pascal.taie.language.type.CharType.CHAR;
import static pascal.taie.language.type.DoubleType.DOUBLE;
import static pascal.taie.language.type.FloatType.FLOAT;
import static pascal.taie.language.type.IntType.INT;
import static pascal.taie.language.type.LongType.LONG;
import static pascal.taie.language.type.ShortType.SHORT;

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

    public static final String ITERABLE = "java.lang.Iterable";
    public static final String ITERATOR = "iterator";
    public static final String HAS_NEXT = "hasNext";
    public static final String ITERATOR_TYPE = "java.lang.Iterator";

    public static final String ENUM = "java.lang.Enum";

    public static boolean isObject(JClass jClass) {
        return jClass.getName().equals(ClassNames.OBJECT);
    }

    /**
     * @return if {@code t1 <: t2}
     */
    public static boolean isSubType(ClassType t1, ClassType t2) {
        return BuildContext.get().getTypeSystem().isSubtype(t1, t2);
    }

    public static boolean hasModifier(int modifier, int target) {
        return (modifier & target) != 0;
    }

    public static boolean isSameMethod(IMethodBinding binding1, IMethodBinding binding2) {
        return binding1.getMethodDeclaration() == binding2.getMethodDeclaration();
    }

    public static List<Type> getInitParamTypeWithSyn(List<Type> orig, ITypeBinding declClass) {
        InnerClassDescriptor descriptor = InnerClassManager.get().getInnerClassDesc(declClass);
        if (descriptor == null) {
            return orig;
        }
        List<Type> res = new ArrayList<>(fromJDTTypeList(descriptor.synParaTypes().stream()));
        if (descriptor.getExplicitEnclosedInstance() != null) {
            res.add(JDTTypeToTaieType(descriptor.getExplicitEnclosedInstance()));
        }
        res.addAll(orig);
        return res;
    }

    public static <T> List<T> addList(List<T> l1, List<T> l2) {
        List<T> res = new ArrayList<>();
        res.addAll(l1);
        res.addAll(l2);
        return res;
    }

    public static ClassType getStringType() {
        return getClassByName(ClassNames.STRING);
    }

    public static List<Type> getEnumCtorType() {
        return List.of(getStringType(), INT);
    }
    public static List<Type> getEnumCtorArgType(List<Type> orig) {
        return addList(getEnumCtorType(), orig);
    }

    public static List<String> getAnonymousSynCtorArgName(List<String> orig) {
        return addList(List.of(getAnonymousSynCtorArgName(0), getAnonymousSynCtorArgName(1)),
                orig);
    }

    public static boolean isEnumType(ITypeBinding binding) {
        return binding.isEnum() || (binding.isAnonymous() && binding.getSuperclass().isEnum());
    }

    public static String getAnonymousSynCtorArgName(int index) {
        return "val$" + index;
    }

    public static MethodRef getEnumMethodValueOf() {
        JClass enumKlass = getClassByName(ENUM).getJClass();
        assert enumKlass != null;
        return MethodRef.get(enumKlass,
                ENUM_METHOD_VALUE_OF, List.of(getClassByName(ClassNames.CLASS),
                        getClassByName(ClassNames.STRING)),
                getClassByName(ENUM), true, false);
    }

    public static String ENUM_VALUES = "VALUES";

    public static String ENUM_METHOD_VALUES = "values";

    public static String ENUM_METHOD_VALUE_OF = "valueOf";

    public static MethodRef getArrayClone() {
        return MethodRef.get(getClassByName(ClassNames.ARRAY).getJClass(),
                "clone", List.of(), getClassByName(ClassNames.OBJECT), false, false);
    }
    /**
     * Only handle modifier in source
     */
    public static Set<Modifier> fromJDTModifier(int modifier) {
        // TODO: this set should be mutable
        return Arrays.stream(Modifier.values())
                .filter(m -> hasModifier(modifier, toJDTModifier(m)))
                .collect(Collectors.toSet());
    }

    public static JClass getSuperClass(ITypeBinding binding) {
        ITypeBinding superTypeBinding = binding.getSuperclass();
        if (superTypeBinding != null) {
            return getTaieClass(superTypeBinding);
        } else {
            return BuildContext.get().getClassByName(ClassNames.OBJECT);
        }
    }

    public static boolean isStatic(int modifiers) {
        return hasModifier(modifiers, org.eclipse.jdt.core.dom.Modifier.STATIC);
    }

    public static Set<Modifier> computeModifier(ITypeBinding binding) {
        binding = binding.getErasure();
        Set<Modifier> res = new HashSet<>(fromJDTModifier(binding.getModifiers()));

        if (binding.isInterface()) {
            res.add(Modifier.INTERFACE);
            res.add(Modifier.ABSTRACT);
        }

        if (binding.isAnnotation()) {
            res.add(Modifier.ANNOTATION);
        }

        if (binding.isEnum()) {
            res.add(Modifier.ENUM);
        }

        return res;
    }

    public static int toJDTModifier(Modifier m) {
        return switch (m) {
            case PUBLIC -> org.eclipse.jdt.core.dom.Modifier.PUBLIC;
            case PRIVATE -> org.eclipse.jdt.core.dom.Modifier.PRIVATE;
            case PROTECTED -> org.eclipse.jdt.core.dom.Modifier.PROTECTED;
            case STATIC -> org.eclipse.jdt.core.dom.Modifier.STATIC;
            case FINAL -> org.eclipse.jdt.core.dom.Modifier.FINAL;
            case SYNCHRONIZED -> org.eclipse.jdt.core.dom.Modifier.SYNCHRONIZED;
            case VOLATILE -> org.eclipse.jdt.core.dom.Modifier.VOLATILE;
            case TRANSIENT -> org.eclipse.jdt.core.dom.Modifier.TRANSIENT;
            case NATIVE -> org.eclipse.jdt.core.dom.Modifier.NATIVE;
            // case INTERFACE
            case STRICTFP -> org.eclipse.jdt.core.dom.Modifier.STRICTFP;
            // case BRIDGE
            // case VARARGS
            // case SYNTHETIC
            // case ANNOTATION
            // case ENUM
            // case MANDATED
            default -> 0;
        };
    }

    public static Set<Modifier> copyVisuality(Set<Modifier> modifiers) {
        Set<Modifier> targets = Set.of(Modifier.PUBLIC, Modifier.PROTECTED, Modifier.PRIVATE);
        return modifiers.stream()
                .filter(targets::contains)
                .collect(Collectors.toSet());
    }

    public static String getErasedName(ITypeBinding iTypeBinding) {
        if (iTypeBinding.isPrimitive()) {
            return iTypeBinding.getName();
        } else if (iTypeBinding.isArray()) {
            return getErasedName(iTypeBinding.getComponentType()) + "[]";
        }
        return getBinaryName(iTypeBinding);
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
                case JDT_BYTE -> BYTE;
                case JDT_SHORT -> SHORT;
                case JDT_INT -> INT;
                case JDT_LONG -> LONG;
                case JDT_CHAR -> CHAR;
                case JDT_DOUBLE -> DOUBLE;
                case JDT_FLOAT -> FLOAT;
                case JDT_BOOLEAN -> BOOLEAN;
                case JDT_VOID -> VoidType.VOID;
                default -> throw new NewFrontendException("Primitive Type Illegal: " + typeBinding.getName());
            };
        } else if (typeBinding.isNullType()) {
            return NullType.NULL;
        } else {
            ITypeBinding erased = typeBinding.getErasure();
            TypeSystem tm = BuildContext.get().getTypeSystem();
            if (erased.isClass() || erased.isInterface() || erased.isEnum()) {
                return tm.getType(getBinaryName(erased));
            } else if (erased.isArray()) {
                return tm.getArrayType(JDTTypeToTaieType(erased.getElementType()), erased.getDimensions());
            }
            throw new NewFrontendException("JDTTypeToTaieType: " + typeBinding.getName() + " is Not Implemented.");
        }
    }

    public static @Nullable JClass getTaieClass(ITypeBinding binding) {
        return BuildContext.get().getClassByName(getBinaryName(binding));
    }

    public static Literal getRightPrimitiveLiteral(Expression e) {
        var t = e.resolveTypeBinding();
        var res = e.resolveConstantExpressionValue();
        if (res == null) {
            if (e instanceof NullLiteral) {
                return pascal.taie.ir.exp.NullLiteral.get();
            } else if (e instanceof NumberLiteral numberLiteral) {
                // only used by JavaMethodIRBuilder
                // currently, just literal 0
                String value = numberLiteral.getToken();
                assert Objects.equals(value, "0");
                return IntLiteral.get(0);
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
            } else if (res instanceof String s) {
                return pascal.taie.ir.exp.StringLiteral.get(s);
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
            case ">>"  -> ShiftExp.Op.SHR;
            case "<<"  -> ShiftExp.Op.SHL;
            case ">>>" -> ShiftExp.Op.USHR;
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

    public static ClassType getClassByName(String name) {
       return (ClassType) BuildContext.get().fromAsmInternalName(name);
    }

    public static JClass getJClass(ITypeBinding binding) {
        return BuildContext.get().getClassByName(getBinaryName(binding));
    }

    public static Type anyException() {
        JClass thr = World.get().getClassHierarchy().getClass(ClassNames.THROWABLE);
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
        IMethodBinding binding = typeBinding.getFunctionalInterfaceMethod().getMethodDeclaration();
        return getMethodType(binding);
    }

    static String extractFuncInterfaceName(ITypeBinding typeBinding) {
        IMethodBinding binding = typeBinding.getFunctionalInterfaceMethod().getMethodDeclaration();
        return binding.getName();
    }

    public static MethodType getMethodType(IMethodBinding binding) {
        assert (binding != null);

        Type retType = JDTTypeToTaieType(binding.getReturnType());
        List<Type> paraType = fromJDTTypeList(binding.getParameterTypes());
        return MethodType.get(paraType, retType);
    }

    static MethodType getBoxedMethodType(IMethodBinding binding) {
        assert (binding != null);

        Type retType = boxing(JDTTypeToTaieType(binding.getReturnType()));
        List<Type> paraType = fromJDTTypeList(binding.getParameterTypes())
                .stream().map(TypeUtils::boxing)
                .toList();
        return MethodType.get(paraType, retType);
    }

    static Type boxing(Type t) {
        if (t instanceof PrimitiveType p) {
            return getClassByName(getRefNameOfPrimitive(p.getName()));
        } else {
            return t;
        }
    }

    public static List<Type> fromJDTTypeList(ITypeBinding[] types) {
        return fromJDTTypeList(Arrays.stream(types));
    }

    public static List<Type> fromJDTTypeList(Stream<ITypeBinding> types) {
        return types.map(TypeUtils::JDTTypeToTaieType)
                .toList();
    }

    public static List<ClassType> toClassTypes(ITypeBinding[] types) {
        return fromJDTTypeList(types).stream()
                .map(t -> (ClassType) t)
                .toList();
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
        return Utils.getPrimitiveTypeIndex(t);
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
            case 0 -> BOOLEAN;
            case 1 -> CHAR;
            case 2 -> BYTE;
            case 3 -> SHORT;
            case 4 -> INT;
            case 5 -> LONG;
            case 6 -> FLOAT;
            case 7 -> DOUBLE;
            default -> throw new NewFrontendException(i + " is not legal primitive index");
        };
    }

    public static PrimitiveType getWidenType(PrimitiveType type) {
        if (getIndexOfPrimitive(type) < getIndexOfPrimitive(INT)) {
            return INT;
        } else {
            return type;
        }
    }

    public static PrimitiveType getPrimitiveByRef(Type t) {
        return getPrimitiveByIndex(getIndexOfPrimitive(t));
    }

    public static Type getType(String name) {
        return World.get().getTypeSystem().getType(name);
    }

    public static boolean isSubTypeOf(Type superType, Type subType) {
        return World.get().getTypeSystem().isSubtype(superType, subType);
    }

    public static Optional<Type> getIterableInner(ITypeBinding type) {
        Queue<ITypeBinding> queue = new SetQueue<>();
        ITypeBinding t = null;
        queue.add(type);
        while (! queue.isEmpty()) {
            var now = queue.poll();
            if (now.getErasure().getBinaryName().equals(ITERABLE)) {
                t = now;
                break;
            } else {
                var impls = now.getInterfaces();
                Collections.addAll(queue, impls);
                if (now.getSuperclass() != null) {
                    queue.add(now.getSuperclass());
                }
            }
        }
        if (t == null) {
            return Optional.empty();
        }
        return Optional.of(getType(getErasedName(t.getTypeArguments()[0])));
    }

    public static IMethodBinding searchMethod(ITypeBinding typeBinding, String name) {
        var methods = typeBinding.getDeclaredMethods();
        for (var i : methods) {
            if (i.getName().equals(name)) {
                return i;
            }
        }
        throw new NewFrontendException("There's no such method: " + name + " in " + typeBinding);
    }

    public static ArrayType getArrType(Type eleType) {
        return World.get().getTypeSystem().getArrayType(eleType, 1);
    }

    public static boolean isComputeInt(PrimitiveType t) {
        return getIndexOfPrimitive(t) <= getIndexOfPrimitive(INT);
    }

    public static boolean computeIntWiden(PrimitiveType expType, PrimitiveType target) {
        int t1 = getIndexOfPrimitive(expType);
        int t2 = getIndexOfPrimitive(target);
        int tInt = getIndexOfPrimitive(INT);
        return t2 <= tInt && t1 <= t2;
    }

    public static Literal getLiteral(Literal l, PrimitiveType t) {
        if (isComputeInt(t)) {
            if (l instanceof IntLiteral) {
                return l;
            } else if (l instanceof DoubleLiteral d) {
                return IntLiteral.get((int) d.getValue());
            } else if (l instanceof FloatLiteral f) {
                return IntLiteral.get((int) f.getValue());
            } else if (l instanceof LongLiteral l1) {
                return IntLiteral.get((int) l1.getValue());
            }
        } else if (t.equals(LONG)) {
            if (l instanceof IntLiteral i) {
                return LongLiteral.get(i.getValue());
            } else if (l instanceof DoubleLiteral d) {
                return LongLiteral.get((long) d.getValue());
            } else if (l instanceof FloatLiteral f) {
                return LongLiteral.get((long) f.getValue());
            } else if (l instanceof LongLiteral l1) {
                return l;
            }
        } else if (t.equals(DOUBLE)) {
            if (l instanceof IntLiteral i) {
                return DoubleLiteral.get(i.getValue());
            } else if (l instanceof DoubleLiteral d) {
                return l;
            } else if (l instanceof FloatLiteral f) {
                return DoubleLiteral.get(f.getValue());
            } else if (l instanceof LongLiteral l1) {
                return DoubleLiteral.get(l1.getValue());
            }
        } else if (t.equals(FLOAT)) {
            if (l instanceof IntLiteral i) {
                return FloatLiteral.get(i.getValue());
            } else if (l instanceof DoubleLiteral d) {
                return FloatLiteral.get((float) d.getValue());
            } else if (l instanceof FloatLiteral f) {
                return l;
            } else if (l instanceof LongLiteral l1) {
                return FloatLiteral.get(l1.getValue());
            }
        }

        throw new NewFrontendException(l + " can't be converted to " + t);
    }

    public static AnnotationHolder getAnnotations(IBinding binding) {
        IAnnotationBinding[] annotations = binding.getAnnotations();
        List<Annotation> annotationList = new ArrayList<>();
        for (IAnnotationBinding annotation : annotations) {
            Annotation current = fromJDTAnnotation(annotation);
            annotationList.add(current);
        }

        return AnnotationHolder.make(annotationList);
    }

    /**
     * See JDT doc of {@link IAnnotationBinding}
     */
    public static Annotation fromJDTAnnotation(IAnnotationBinding annotation) {
        String type = annotation.getName();
        Map<String, Element> kvs = new HashMap<>();
        for (IMemberValuePairBinding kv : annotation.getAllMemberValuePairs()) {
            String key = kv.getName();
            Object value = kv.getValue();
            Element ele = fromJDTAnnotationValue(value);
            kvs.put(key, ele);
        }
        return new Annotation(type, kvs);
    }

    /**
     * See JDT doc of {@link IMemberValuePairBinding}
     */
    public static Element fromJDTAnnotationValue(Object obj) {
        if (obj instanceof Integer i) {
            return new IntElement(i);
        } else if (obj instanceof Character c) {
            return new IntElement(c);
        } else if (obj instanceof Byte b) {
            return new IntElement(b);
        } else if (obj instanceof Short s) {
            return new IntElement(s);
        } else if (obj instanceof Float f) {
            return new FloatElement(f);
        } else if (obj instanceof Long l) {
            return new LongElement(l);
        } else if (obj instanceof Double d) {
            return new DoubleElement(d);
        } else if (obj instanceof Boolean b) {
            return new BooleanElement(b);
        } else if (obj instanceof String s) {
            return new StringElement(s);
        } else if (obj instanceof ITypeBinding typeBinding) {
            return new ClassElement(getErasedName(typeBinding));
        } else if (obj instanceof IVariableBinding variableBinding) {
            String type = getErasedName(variableBinding.getType());
            String name = variableBinding.getName();
            return new EnumElement(type, name);
        } else if (obj instanceof IAnnotationBinding annotationBinding) {
            return new AnnotationElement(fromJDTAnnotation(annotationBinding));
        } else if (obj instanceof Object[] arr) {
            return new ArrayElement(
                    Arrays.stream(arr).map(TypeUtils::fromJDTAnnotationValue)
                    .toList());
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
