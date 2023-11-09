package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import pascal.taie.World;
import pascal.taie.ir.exp.ArrayAccess;
import pascal.taie.ir.exp.BinaryExp;
import pascal.taie.ir.exp.CastExp;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.DoubleLiteral;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.FieldAccess;
import pascal.taie.ir.exp.FloatLiteral;
import pascal.taie.ir.exp.InstanceOfExp;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.LongLiteral;
import pascal.taie.ir.exp.MethodHandle;
import pascal.taie.ir.exp.NewExp;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.ir.exp.UnaryExp;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.ir.proginfo.MemberRef;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.ir.stmt.Binary;
import pascal.taie.ir.stmt.Cast;
import pascal.taie.ir.stmt.Copy;
import pascal.taie.ir.stmt.InstanceOf;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.LoadArray;
import pascal.taie.ir.stmt.LoadField;
import pascal.taie.ir.stmt.New;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.StoreArray;
import pascal.taie.ir.stmt.StoreField;
import pascal.taie.ir.stmt.Unary;
import pascal.taie.language.annotation.ArrayElement;
import pascal.taie.language.annotation.BooleanElement;
import pascal.taie.language.annotation.ClassElement;
import pascal.taie.language.annotation.DoubleElement;
import pascal.taie.language.annotation.Element;
import pascal.taie.language.annotation.IntElement;
import pascal.taie.language.annotation.LongElement;
import pascal.taie.language.annotation.StringElement;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Modifier;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Sets;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class Utils {
    static String getBinaryName(String internalName) {
        return Type.getObjectType(internalName).getClassName();
    }

    static boolean hasAsmModifier(int opcodes, int modifier) {
        return (opcodes & modifier) != 0;
    }

    static int toAsmModifier(Modifier modifier) {
        return switch (modifier) {
            case PUBLIC -> Opcodes.ACC_PUBLIC;
            case PRIVATE -> Opcodes.ACC_PRIVATE;
            case PROTECTED -> Opcodes.ACC_PROTECTED;
            case STATIC -> Opcodes.ACC_STATIC;
            case FINAL -> Opcodes.ACC_FINAL;
            case SYNCHRONIZED -> Opcodes.ACC_SYNCHRONIZED;
            case VOLATILE -> Opcodes.ACC_VOLATILE;
            case TRANSIENT -> Opcodes.ACC_TRANSIENT;
            case NATIVE -> Opcodes.ACC_NATIVE;
            case INTERFACE -> Opcodes.ACC_INTERFACE;
            case ABSTRACT -> Opcodes.ACC_ABSTRACT;
            case STRICTFP -> Opcodes.ACC_STRICT;
            case BRIDGE -> Opcodes.ACC_BRIDGE;
            case VARARGS -> Opcodes.ACC_VARARGS;
            case SYNTHETIC -> Opcodes.ACC_SYNTHETIC;
            case ANNOTATION -> Opcodes.ACC_ANNOTATION;
            case ENUM -> Opcodes.ACC_ENUM;
            case MANDATED -> Opcodes.ACC_MANDATED;
        };
    }

    static Set<Modifier> fromAsmModifier(int opcodes) {
        Set<Modifier> res = new HashSet<>();
        if (hasAsmModifier(opcodes, Opcodes.ACC_PUBLIC)) {
            res.add(Modifier.PUBLIC);
        }
        if (hasAsmModifier(opcodes, Opcodes.ACC_PRIVATE)) {
            res.add(Modifier.PRIVATE);
        }
        if (hasAsmModifier(opcodes, Opcodes.ACC_PROTECTED)) {
            res.add(Modifier.PROTECTED);
        }
        if (hasAsmModifier(opcodes, Opcodes.ACC_STATIC)) {
            res.add(Modifier.STATIC);
        }
        if (hasAsmModifier(opcodes, Opcodes.ACC_FINAL)) {
            res.add(Modifier.FINAL);
        }
        if (hasAsmModifier(opcodes, Opcodes.ACC_SYNCHRONIZED)) {
            res.add(Modifier.SYNCHRONIZED);
        }
        if (hasAsmModifier(opcodes, Opcodes.ACC_VOLATILE)) {
            res.add(Modifier.VOLATILE);
        }
        if (hasAsmModifier(opcodes, Opcodes.ACC_TRANSIENT)) {
            res.add(Modifier.TRANSIENT);
        }
        if (hasAsmModifier(opcodes, Opcodes.ACC_NATIVE)) {
            res.add(Modifier.NATIVE);
        }
        if (hasAsmModifier(opcodes, Opcodes.ACC_INTERFACE)) {
            res.add(Modifier.INTERFACE);
        }
        if (hasAsmModifier(opcodes, Opcodes.ACC_ABSTRACT)) {
            res.add(Modifier.ABSTRACT);
        }
        if (hasAsmModifier(opcodes, Opcodes.ACC_STRICT)) {
            res.add(Modifier.STRICTFP);
        }
        if (hasAsmModifier(opcodes, Opcodes.ACC_BRIDGE)) {
            res.add(Modifier.BRIDGE);
        }
        if (hasAsmModifier(opcodes, Opcodes.ACC_VARARGS)) {
            res.add(Modifier.VARARGS);
        }
        if (hasAsmModifier(opcodes, Opcodes.ACC_SYNTHETIC)) {
            res.add(Modifier.SYNTHETIC);
        }
        if (hasAsmModifier(opcodes, Opcodes.ACC_ANNOTATION)) {
            res.add(Modifier.ANNOTATION);
        }
        if (hasAsmModifier(opcodes, Opcodes.ACC_ENUM)) {
            res.add(Modifier.ENUM);
        }
        if (hasAsmModifier(opcodes, Opcodes.ACC_MANDATED)) {
            res.add(Modifier.MANDATED);
        }
        return res;
    }

    /**
     * Convert object to tai-e Annotation rep.
     * @param ele object, should be boxed primitive type OR string OR array OR asm type
     */
    static Element toElement(Object ele) {
        if (ele instanceof Boolean b) {
            return new BooleanElement(b);
        } else if (ele instanceof Character c) {
            return new IntElement(c);
        } else if (ele instanceof Short s) {
            return new IntElement(s);
        } else if (ele instanceof Integer i) {
            return new IntElement(i);
        } else if (ele instanceof Long l) {
            return new LongElement(l);
        } else if (ele instanceof Float f) {
            return new DoubleElement(f);
        } else if (ele instanceof Double d) {
            return new DoubleElement(d);
        } else if (ele instanceof String s) {
            return new StringElement(s);
        } else if (ele.getClass().isArray()) {
            List<Element> elements = new ArrayList<>();
            for (int i = 0; i < Array.getLength(ele); ++i) {
                elements.add(toElement(Array.get(ele, i)));
            }
            return new ArrayElement(elements);
        } else if (ele instanceof Type c) {
            // TODO: Does ClassElement really hold asm descriptor ?
            return new ClassElement(c.getDescriptor());
        } else {
            throw new IllegalArgumentException();
        }
    }

    /**
     * Check if an asm instruction indices the control flow edge
     */
    static boolean isCFEdge(AbstractInsnNode node) {
        return node instanceof JumpInsnNode ||
                node instanceof TableSwitchInsnNode ||
                node instanceof LookupSwitchInsnNode ||
                node instanceof LabelNode ||
                isReturn(node) ||
                isThrow(node);
    }

    static boolean isVarStore(AbstractInsnNode node) {
        if (node instanceof VarInsnNode varInsnNode) {
            int op = varInsnNode.getOpcode();
            return op == Opcodes.ISTORE ||
                    op == Opcodes.LSTORE ||
                    op == Opcodes.FSTORE ||
                    op == Opcodes.DSTORE ||
                    op == Opcodes.ASTORE;
        } else {
            return false;
        }
    }

    static boolean isReturn(AbstractInsnNode node) {
        if (node instanceof InsnNode insnNode) {
            int op = insnNode.getOpcode();
            return op == Opcodes.ARETURN ||
                    op == Opcodes.IRETURN ||
                    op == Opcodes.LRETURN ||
                    op == Opcodes.FRETURN ||
                    op == Opcodes.DRETURN ||
                    op == Opcodes.RETURN;
        } else {
            return false;
        }
    }

    static boolean isThrow(AbstractInsnNode node) {
        if (node instanceof InsnNode insnNode) {
            return insnNode.getOpcode() == Opcodes.ATHROW;
        }
        return false;
    }

    static Literal fromObject(Object o) {
        // TODO: handle MethodType / ConstantDynamic
        if (o instanceof Integer i) {
            return IntLiteral.get(i);
        } else if (o instanceof Long l) {
            return LongLiteral.get(l);
        } else if (o instanceof Float f) {
            return FloatLiteral.get(f);
        } else if (o instanceof Double d) {
            return DoubleLiteral.get(d);
        } else if (o instanceof String s) {
            return StringLiteral.get(s);
        } else if (o instanceof Type t) {
            if (t.getSort() == Type.METHOD) {
                return BuildContext.get().toMethodType(t);
            } else {
                return ClassLiteral.get(BuildContext.get().fromAsmType(t));
            }
        } else if (o instanceof Handle handle) {
            return fromAsmHandle(handle);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static MethodHandle fromAsmHandle(Handle handle) {
        MethodHandle.Kind kind = toMethodHandleKind(handle.getTag());
        MemberRef ref;
        JClass jClass = BuildContext.get().toJClass(handle.getOwner());
        if (isFieldKind(kind)) {
            pascal.taie.language.type.Type t =
                    BuildContext.get().fromAsmType(handle.getDesc());
            ref = FieldRef.get(jClass, handle.getName(), t,
                    kind == MethodHandle.Kind.REF_getStatic ||
                            kind == MethodHandle.Kind.REF_putStatic);
        } else {
            Pair<List<pascal.taie.language.type.Type>, pascal.taie.language.type.Type>
                    mtdType = BuildContext.get().fromAsmMethodType(handle.getDesc());
            ref = MethodRef.get(jClass, handle.getName(), mtdType.first(), mtdType.second(),
                    kind == MethodHandle.Kind.REF_invokeStatic);
        }
        return MethodHandle.get(kind, ref);
    }

    static boolean isFieldKind(MethodHandle.Kind kind) {
        return switch (kind) {
            case REF_getField, REF_getStatic, REF_putField, REF_putStatic -> true;
            default -> false;
        };
    }

    static MethodHandle.Kind toMethodHandleKind(int opcode) {
        return switch (opcode) {
            case Opcodes.H_GETFIELD -> MethodHandle.Kind.REF_getField;
            case Opcodes.H_GETSTATIC -> MethodHandle.Kind.REF_getStatic;
            case Opcodes.H_PUTFIELD -> MethodHandle.Kind.REF_putField;
            case Opcodes.H_PUTSTATIC -> MethodHandle.Kind.REF_putStatic;
            case Opcodes.H_INVOKEVIRTUAL -> MethodHandle.Kind.REF_invokeVirtual;
            case Opcodes.H_INVOKESTATIC -> MethodHandle.Kind.REF_invokeStatic;
            case Opcodes.H_INVOKESPECIAL -> MethodHandle.Kind.REF_invokeSpecial;
            case Opcodes.H_NEWINVOKESPECIAL -> MethodHandle.Kind.REF_newInvokeSpecial;
            case Opcodes.H_INVOKEINTERFACE -> MethodHandle.Kind.REF_invokeInterface;
            default -> throw new IllegalArgumentException();
        };
    }

    static Stmt getAssignStmt(JMethod method, LValue left, Exp e) {
        if (left instanceof Var v) {
            if (e instanceof BinaryExp binaryExp) {
                return new Binary(v, binaryExp);
            } else if (e instanceof Literal l) {
                return new AssignLiteral(v, l);
            } else if (e instanceof CastExp cast) {
                return new Cast(v, cast);
            } else if (e instanceof UnaryExp unaryExp) {
                return new Unary(v, unaryExp);
            } else if (e instanceof Var v1) {
                return new Copy(v, v1);
            } else if (e instanceof FieldAccess fieldAccess) {
                return new LoadField(v, fieldAccess);
            } else if (e instanceof InvokeExp invokeExp) {
                return new Invoke(method, invokeExp, v);
            } else if (e instanceof NewExp newExp)  {
                return new New(method, v, newExp);
            } else if (e instanceof ArrayAccess access) {
                return new LoadArray(v, access);
            } else if (e instanceof InstanceOfExp instanceOfExp) {
                return new InstanceOf(v, instanceOfExp);
            }
            else {
                throw new UnsupportedOperationException();
            }
        } else if (left instanceof ArrayAccess arrayAccess) {
            assert e instanceof Var;
            return new StoreArray(arrayAccess, (Var) e);
        } else if (left instanceof FieldAccess fieldAccess) {
            assert e instanceof Var;
            return new StoreField(fieldAccess, (Var) e);
        }
        throw new UnsupportedOperationException();
    }

    static pascal.taie.language.type.Type fromAsmFrameType(Object o) {
        if (o instanceof Integer i) {
            return switch (i) {
                case 0 -> Top.Top; // Opcodes.Top
                case 1 -> PrimitiveType.INT; // Opcodes.INTEGER
                case 2 -> PrimitiveType.FLOAT; // Opcodes.FLOAT
                case 3 -> PrimitiveType.DOUBLE; // Opcodes.DOUBLE
                case 4 -> PrimitiveType.LONG; // Opcodes.LONG
                case 5 -> NullType.NULL; // Opcodes.NULL
                case 6 -> Uninitialized.UNINITIALIZED; // Opcodes.UNINITIALIZED_THIS
                default -> throw new UnsupportedOperationException();
            };
        } else if (o instanceof String s) {
            return BuildContext.get().fromAsmInternalName(s);
        } else if (o instanceof LabelNode) {
            return Uninitialized.UNINITIALIZED;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static String getThrowable() {
        return "java/lang/Throwable";
    }


    private static ClassType object;
    private static ClassType serializable;
    private static ClassType cloneable;
    private static ClassType string;
    private static ClassType reflectArray;
    private static ClassType klass;

    static {
        World.registerResetCallback(() -> {
            object = null;
            serializable = null;
            cloneable = null;
            string = null;
            reflectArray = null;
            klass = null;
        });
    }

    static synchronized ClassType getObject() {
        if (object == null) {
            object = getClassType(ClassNames.OBJECT);
        }
        return object;
    }

    static synchronized ClassType getSerializable() {
        if (serializable == null) {
            serializable = getClassType(ClassNames.SERIALIZABLE);
        }
        return serializable;
    }

    static synchronized ClassType getCloneable() {
        if (cloneable == null) {
            cloneable = getClassType(ClassNames.CLONEABLE);
        }
        return cloneable;
    }

    static synchronized ClassType getString() {
        if (string == null) {
            string = getClassType(ClassNames.STRING);
        }
        return string;
    }

    static synchronized ClassType getReflectArray() {
        if (reflectArray == null) {
            reflectArray = getClassType(ClassNames.ARRAY);
        }
        return reflectArray;
    }

    static synchronized  ClassType getKlass() {
        if (klass == null) {
            klass = getClassType(ClassNames.CLASS);
        }
        return klass;
    }

    private static ClassType getClassType(String s) {
        return BuildContext.get().getTypeSystem().getClassType(s);
    }

    static Set<ReferenceType> minimum(Set<ClassType> in) {
        Set<ClassType> removed = Sets.newHybridSet();
        for (ClassType t1: in) {
            if (removed.contains(t1)) {
                continue;
            } else {
                Set<ClassType> upper = upperClosure(t1);
                upper.remove(t1);
                removed.addAll(upper);
            }
        }
        in.removeAll(removed);
        return Collections.unmodifiableSet(in);
    }

    public static Set<ReferenceType> lca(ReferenceType t1, ReferenceType t2) {
        assert ! (t1 != t2 && t1.equals(t2));
        if (t1 == t2) {
            return Set.of(t1);
        } else if (t1 instanceof NullType) {
            return Set.of(t2);
        } else if (t2 instanceof NullType) {
            return Set.of(t1);
        } else if (t1 instanceof ClassType ct1 && t2 instanceof ClassType ct2) {
            Set<ClassType> upper1 = upperClosure(ct1);
            Set<ClassType> upper2 = upperClosure(ct2);
            if (upper2.contains(ct1)) {
                return Set.of(ct1);
            } else if (upper1.contains(ct2)) {
                return Set.of(ct2);
            } else {
                intersection(upper1, upper2);
                return minimum(upper1);
            }
        } else if (t1 instanceof ClassType ct1 && t2 instanceof ArrayType at2) {
            Set<ClassType> upper1 = upperClosure(ct1);
            Set<ClassType> upper2 = getArraySupers();
            intersection(upper1, upper2);
            return minimum(upper1);
        } else if (t1 instanceof ArrayType && t2 instanceof ClassType) {
            return lca(t2, t1);
        } else if (t1 instanceof ArrayType at1 && t2 instanceof ArrayType at2) {
            if (at1.elementType() instanceof PrimitiveType
                    || at2.elementType() instanceof PrimitiveType) {
                return Collections.unmodifiableSet(getArraySupers());
            } else {
                ReferenceType r1 = (ReferenceType) at1.elementType();
                ReferenceType r2 = (ReferenceType) at2.elementType();
                return lca(r1, r2).stream()
                        .map(Utils::wrap1)
                        .collect(Collectors.toSet());
            }
        }
        throw new UnsupportedOperationException();
    }

    static Set<ClassType> getArraySupers() {
        return Set.of(getObject(), getCloneable(), getSerializable());
    }

    /**
     * s1 <- s1 /\ s2
     */
    static <T> void intersection(Set<T> s1, Set<T> s2) {
        s1.removeIf(t -> ! s2.contains(t));
    }

    /**
     * @param types null and uninitialized type should be removed
     */
    static Set<ReferenceType> lca(Set<ReferenceType> types) {
        if (types.size() <= 1) {
            return types;
        } else {
            if (allRefArray(types)) {
                return lca(types.stream().map(t -> (ReferenceType) ((ArrayType) t).elementType())
                        .collect(Collectors.toSet()))
                        .stream()
                        .map(Utils::wrap1)
                        .collect(Collectors.toSet());
            }

            Set<ClassType> res = null;
            for (ReferenceType t : types) {
                Set<ClassType> current;
                if (t instanceof NullType) {
                    continue;
                } else if (t instanceof ArrayType at) {
                    current = new HashSet<>(getArraySupers());
                } else {
                    current = upperClosure((ClassType) t);
                }
                if (res == null) {
                    res = current;
                } else {
                    intersection(res, current);
                }
            }

            assert res != null;
            return minimum(res);
        }
    }

    static boolean allRefArray(Set<ReferenceType> types) {
        return types.stream().allMatch(t -> t instanceof ArrayType arrayType
                && arrayType.elementType() instanceof ReferenceType);
    }

    static ArrayType wrap1(ReferenceType referenceType) {
        TypeSystem ts = BuildContext.get().getTypeSystem();
        if (referenceType instanceof ArrayType at) {
            return ts.getArrayType(at.baseType(), at.dimensions() + 1);
        } else {
            return ts.getArrayType(referenceType, 1);
        }
    }

    public static Set<ClassType> upperClosure(ClassType type) {
        Queue<JClass> workList = new LinkedList<>();
        workList.add(type.getJClass());
        Set<ClassType> res = Sets.newHybridSet();

        while (! workList.isEmpty()) {
            JClass now = workList.poll();
            assert now != null;
            if (! res.contains(now.getType())) {
                workList.addAll(getAllDirectSuperType(now));
                res.add(now.getType());
            }
        }

        return res;
    }

    static List<JClass> getAllDirectSuperType(JClass type) {
        List<JClass> res = new ArrayList<>(type.getInterfaces());
        if (type.getSuperClass() != null) {
            res.add(type.getSuperClass());
        }
        return res;
    }

    static boolean isTwoWord(pascal.taie.language.type.Type t) {
        return t == PrimitiveType.DOUBLE || t == PrimitiveType.LONG;
    }

    static boolean canHoldsInt(pascal.taie.language.type.Type t) {
        return t instanceof PrimitiveType p && p.asInt();
    }

    static boolean isSubtype(pascal.taie.language.type.Type supertype, pascal.taie.language.type.Type subtype) {
        ClassHierarchy hierarchy = BuildContext.get().getClassHierarchy();
        ClassType OBJECT = Utils.getObject();
        ClassType CLONEABLE = Utils.getCloneable();
        ClassType SERIALIZABLE = Utils.getSerializable();

        assert subtype != null;
        if (subtype == supertype) {
            return true;
        } else if (subtype instanceof NullType) {
            return supertype instanceof ReferenceType;
        } else if (subtype instanceof ClassType) {
            if (supertype instanceof ClassType) {
                return isSubclass((ClassType) supertype, (ClassType) subtype);
            }
        } else if (subtype instanceof ArrayType) {
            if (supertype instanceof ClassType) {
                // JLS (11 Ed.), Chapter 10, Arrays
                return supertype == OBJECT ||
                        supertype == CLONEABLE ||
                        supertype == SERIALIZABLE;
            } else if (supertype instanceof ArrayType superArray) {
                ArrayType subArray = (ArrayType) subtype;
                pascal.taie.language.type.Type superBase = superArray.baseType();
                pascal.taie.language.type.Type subBase = subArray.baseType();
                if (superArray.dimensions() == subArray.dimensions()) {
                    if (subBase.equals(superBase)) {
                        return true;
                    } else if (superBase instanceof ClassType &&
                            subBase instanceof ClassType) {
                        return isSubclass((ClassType) superBase, (ClassType) subBase);
                    }
                } else if (superArray.dimensions() < subArray.dimensions()) {
                    return superBase == OBJECT ||
                            superBase == CLONEABLE ||
                            superBase == SERIALIZABLE;
                }
            }
        }
        return false;
    }

    private static boolean isSubclass(ClassType supertype, ClassType subtype) {
        // disable type checking for phantom
        if (World.get().getOptions().isAllowPhantom()) {
            if (supertype.getJClass().isPhantom() || subtype.getJClass().isPhantom()) {
                return true;
            }
        }
        JClass subClass = subtype.getJClass();
        assert subClass != null;
        JClass superClass = supertype.getJClass();
        if (subClass.getName().equals(ClassNames.OBJECT)) {
            return subClass == superClass;
        }
        if (!subClass.isInterface() && !superClass.isInterface()) {
            return isSubclassOnly(subClass, superClass);
        } else if (subClass.isInterface() && !superClass.isInterface()) {
            return subClass.getSuperClass() == superClass;
        } else {
            return isSubInterfaces(subClass, superClass);
        }
    }

    private static boolean isSubclassOnly(JClass subClass, JClass superClass) {
        assert !subClass.isInterface() && !superClass.isInterface();
        JClass realSuperClass = subClass.getSuperClass();
        if (realSuperClass == null) {
            return false;
        }
        return realSuperClass == superClass || isSubclassOnly(realSuperClass, superClass);
    }

    private static boolean isSubInterfaces(JClass subClass, JClass superClass) {
        assert superClass.isInterface();
        JClass realSuperClass = subClass.getSuperClass();
        if (realSuperClass == null) {
            return false;
        }
        if (isSubInterfaces(realSuperClass, superClass)) {
            return true;
        }
        for (JClass i : subClass.getInterfaces()) {
            if (i == superClass || isSubInterfaces(i, superClass)) {
                return true;
            }
        }
        return false;
    }

    static boolean isPrimitiveArrayType(pascal.taie.language.type.Type t) {
        return t instanceof ArrayType at && canHoldsInt(at.baseType());
    }

    static boolean isIntAssignable(pascal.taie.language.type.Type t1, pascal.taie.language.type.Type t2) {
        return canHoldsInt(t2) && canHoldsInt(t1);
    }

    /**
     * @return if <code>t1 <- t2</code> is valid
     */
    public static boolean isAssignable(pascal.taie.language.type.Type t1, pascal.taie.language.type.Type t2) {
        if (t1 == t2) {
            return true;
        }
        if (t1 instanceof PrimitiveType) {
            return isIntAssignable(t1, t2);
        } else if (t1 == getReflectArray() && t2 instanceof ArrayType) {
            return true;
        } else {
            return isSubtype(t1, t2);
        }
    }

    static PrimitiveType numericPromotion(PrimitiveType t1, PrimitiveType t2) {
        return toIntType( Math.max( fromIntTypeIndex(t1),  fromIntTypeIndex(t2) ) );
    }

    static int fromIntTypeIndex(PrimitiveType t) {
        return switch (t) {
            case BOOLEAN -> 0;
            case BYTE -> 1;
            case CHAR -> 2;
            case SHORT -> 3;
            case INT -> 4;
            default -> throw new UnsupportedOperationException();
        };
    }

    static PrimitiveType toIntType(int i) {
        return switch (i) {
            case 0 -> PrimitiveType.BOOLEAN;
            case 1 -> PrimitiveType.BYTE;
            case 2 -> PrimitiveType.CHAR;
            case 3 -> PrimitiveType.SHORT;
            case 4 -> PrimitiveType.INT;
            default -> throw new UnsupportedOperationException();
        };
    }
}
