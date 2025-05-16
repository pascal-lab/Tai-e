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
import pascal.taie.language.annotation.FloatElement;
import pascal.taie.language.annotation.IntElement;
import pascal.taie.language.annotation.LongElement;
import pascal.taie.language.annotation.StringElement;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Modifier;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.util.collection.Pair;
import pascal.taie.util.collection.Sets;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

import static pascal.taie.frontend.newfrontend.TOP.Top;
import static pascal.taie.language.type.BooleanType.BOOLEAN;
import static pascal.taie.language.type.ByteType.BYTE;
import static pascal.taie.language.type.CharType.CHAR;
import static pascal.taie.language.type.DoubleType.DOUBLE;
import static pascal.taie.language.type.FloatType.FLOAT;
import static pascal.taie.language.type.IntType.INT;
import static pascal.taie.language.type.LongType.LONG;
import static pascal.taie.language.type.ShortType.SHORT;

/**
 * Utility functions for frontend
 */
public class Utils {
    public static String getBinaryName(String internalName) {
        return Type.getObjectType(internalName).getClassName();
    }

    static boolean hasAsmModifier(int opcodes, int modifier) {
        return (opcodes & modifier) != 0;
    }

    public static int toAsmModifier(Modifier modifier) {
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

    public static int toAsmModifier(Set<Modifier> modifiers) {
        int res = 0;
        for (Modifier modifier : modifiers) {
            res |= toAsmModifier(modifier);
        }
        return res;
    }

    static final Set<Modifier> PUB = EnumSet.of(Modifier.PUBLIC);
    static final Set<Modifier> PRI = EnumSet.of(Modifier.PRIVATE);
    static final Set<Modifier> PRO = EnumSet.of(Modifier.PROTECTED);
    static final Set<Modifier> STA = EnumSet.of(Modifier.STATIC);
    static final Set<Modifier> PUB_FINAL = EnumSet.of(Modifier.PUBLIC, Modifier.FINAL);
    static final Set<Modifier> PRI_FINAL = EnumSet.of(Modifier.PRIVATE, Modifier.FINAL);
    static final Set<Modifier> PRO_FINAL = EnumSet.of(Modifier.PROTECTED, Modifier.FINAL);

    static final List<Modifier> CLASS_MODIFIERS = List.of(
            Modifier.PUBLIC,    Modifier.FINAL,      Modifier.INTERFACE, Modifier.ABSTRACT,
            Modifier.SYNTHETIC, Modifier.ANNOTATION, Modifier.ENUM);
    static final int[] CLASS_ASM_MODIFIERS =
            CLASS_MODIFIERS.stream().mapToInt(Utils::toAsmModifier).toArray();

    static final List<Modifier> FIELD_MODIFIERS = List.of(
            Modifier.PUBLIC, Modifier.PRIVATE,  Modifier.PROTECTED, Modifier.STATIC,
            Modifier.FINAL,  Modifier.VOLATILE, Modifier.TRANSIENT, Modifier.SYNTHETIC,
            Modifier.ENUM);
    static final int[] FIELD_ASM_MODIFIERS =
            FIELD_MODIFIERS.stream().mapToInt(Utils::toAsmModifier).toArray();

    static final List<Modifier> METHOD_MODIFIERS = List.of(
            Modifier.PUBLIC, Modifier.PRIVATE,      Modifier.PROTECTED, Modifier.STATIC,
            Modifier.FINAL,  Modifier.SYNCHRONIZED, Modifier.BRIDGE,    Modifier.VARARGS,
            Modifier.NATIVE, Modifier.ABSTRACT,     Modifier.STRICTFP,  Modifier.SYNTHETIC);
    static final int[] METHOD_ASM_MODIFIERS =
            METHOD_MODIFIERS.stream().mapToInt(Utils::toAsmModifier).toArray();

    public static Set<Modifier> fromAsmModifiers(int opcodes, int[] asmModifiers,
                                                 List<Modifier> taieModifiers) {
        switch (opcodes) {
            case Opcodes.ACC_PUBLIC:
                return PUB;
            case Opcodes.ACC_PRIVATE:
                return PRI;
            case Opcodes.ACC_PROTECTED:
                return PRO;
            case Opcodes.ACC_STATIC:
                return STA;
            case Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL:
                return PUB_FINAL;
            case Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL:
                return PRI_FINAL;
            case Opcodes.ACC_PROTECTED | Opcodes.ACC_FINAL:
                return PRO_FINAL;
        }
        Set<Modifier> res = EnumSet.noneOf(Modifier.class);
        for (int i = 0; i < taieModifiers.size(); i++) {
            if ((opcodes & asmModifiers[i]) != 0) {
                res.add(taieModifiers.get(i));
            }
        }
        return res;
    }

    public static Set<Modifier> fromAsmClassModifier(int opcodes) {
        return fromAsmModifiers(opcodes, CLASS_ASM_MODIFIERS, CLASS_MODIFIERS);
    }

    public static Set<Modifier> fromAsmFieldModifier(int opcodes) {
        return fromAsmModifiers(opcodes, FIELD_ASM_MODIFIERS, FIELD_MODIFIERS);
    }

    public static Set<Modifier> fromAsmMethodModifier(int opcodes) {
        return fromAsmModifiers(opcodes, METHOD_ASM_MODIFIERS, METHOD_MODIFIERS);
    }

    /**
     * Convert object to tai-e Annotation rep.
     * @param ele object, should be boxed primitive type OR string OR array OR asm type
     */
    public static Element toElement(Object ele) {
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
            return new FloatElement(f);
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
            return toClassElement(c);
        } else {
            throw new IllegalArgumentException();
        }
    }

    private static Element toClassElement(Type c) {
        if (c.getDescriptor().equals("V")) {
            // This is due to the abuse of notations in the classfile format.
            // According to the JVM specification section 4.3, "V" is an invalid descriptor.
            // You cannot define a value with type void; void can only be used as the return type in method descriptors.
            // However, in the "class_info_index" defined in JVM spec 4.7.16, "V" is permitted.
            // For example:
            //     @MyAnnotation(type = void.class)
            // This will be compiled to:
            //    RuntimeVisibleAnnotations:
            //        MyAnnotation(
            //          type=class V
            //        )
            // Therefore, we need to handle this case specially, as "V" is not a valid descriptor
            // and cannot be passed to StringReps#toTaieTypeDesc.
            return new ClassElement("void");
        } else {
            return new ClassElement(StringReps.toTaieTypeDesc(c.getDescriptor()));
        }
    }

    /**
     * Check if an asm instruction indices the control flow edge
     */
    public static boolean isCFEdge(AbstractInsnNode node) {
        return node instanceof JumpInsnNode ||
                node instanceof TableSwitchInsnNode ||
                node instanceof LookupSwitchInsnNode ||
                node instanceof LabelNode ||
                isReturn(node) ||
                isThrow(node);
    }

    public static boolean isVarStore(AbstractInsnNode node) {
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

    public static boolean isReturn(AbstractInsnNode node) {
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

    public static boolean isThrow(AbstractInsnNode node) {
        if (node instanceof InsnNode insnNode) {
            return insnNode.getOpcode() == Opcodes.ATHROW;
        }
        return false;
    }

    public static Literal fromObject(FrontendContext context, Object o) {
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
                return context.toMethodType(t);
            } else {
                return ClassLiteral.get(context.fromAsmType(t));
            }
        } else if (o instanceof Handle handle) {
            return fromAsmHandle(context, handle);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static MethodHandle fromAsmHandle(FrontendContext ctx, Handle handle) {
        MethodHandle.Kind kind = toMethodHandleKind(handle.getTag());
        MemberRef ref;
        JClass jClass = ctx.toJClass(handle.getOwner());
        if (isFieldKind(kind)) {
            pascal.taie.language.type.Type t =
                    ctx.fromAsmType(handle.getDesc());
            ref = FieldRef.get(jClass, handle.getName(), t,
                    kind == MethodHandle.Kind.REF_getStatic ||
                            kind == MethodHandle.Kind.REF_putStatic);
        } else {
            Pair<List<pascal.taie.language.type.Type>, pascal.taie.language.type.Type>
                    mtdType = ctx.fromAsmMethodType(handle.getDesc());
            ref = MethodRef.get(jClass, handle.getName(), mtdType.first(), mtdType.second(),
                    kind == MethodHandle.Kind.REF_invokeStatic, handle.isInterface());
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

    public static Stmt getAssignStmt(JMethod method, LValue left, Exp e) {
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
            } else {
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

    public  static pascal.taie.language.type.Type fromAsmFrameType(Object o) {
        if (o instanceof Integer i) {
            return switch (i) {
                case 0 -> Top; // Opcodes.Top
                case 1 -> INT; // Opcodes.INTEGER
                case 2 -> FLOAT; // Opcodes.FLOAT
                case 3 -> DOUBLE; // Opcodes.DOUBLE
                case 4 -> LONG; // Opcodes.LONG
                case 5 -> NullType.NULL; // Opcodes.NULL
                case 6 -> Uninitialized.UNINITIALIZED; // Opcodes.UNINITIALIZED_THIS
                default -> throw new UnsupportedOperationException();
            };
        } else if (o instanceof String s) {
            return FrontendContext.get().fromAsmInternalName(s);
        } else if (o instanceof LabelNode) {
            return Uninitialized.UNINITIALIZED;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    static Set<ReferenceType> minimum(Set<ClassType> in) {
        Set<ClassType> removed = Sets.newHybridSet();
        for (ClassType t1 : in) {
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

    public static Set<ReferenceType> lca(TypeContext tCtx, ReferenceType t1, ReferenceType t2) {
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
            Set<ClassType> upper2 = getArraySupers(tCtx);
            intersection(upper1, upper2);
            return minimum(upper1);
        } else if (t1 instanceof ArrayType && t2 instanceof ClassType) {
            return lca(tCtx, t2, t1);
        } else if (t1 instanceof ArrayType at1 && t2 instanceof ArrayType at2) {
            if (at1.elementType() instanceof PrimitiveType
                    || at2.elementType() instanceof PrimitiveType) {
                return Collections.unmodifiableSet(getArraySupers(tCtx));
            } else {
                ReferenceType r1 = (ReferenceType) at1.elementType();
                ReferenceType r2 = (ReferenceType) at2.elementType();
                return lca(tCtx, r1, r2).stream()
                        .map((t) -> Utils.wrap1(tCtx, t))
                        .collect(Collectors.toSet());
            }
        }
        throw new UnsupportedOperationException();
    }

    static Set<ClassType> getArraySupers(TypeContext tCtx) {
        return Set.of(tCtx.object(), tCtx.cloneable(), tCtx.serializable());
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
    public static Set<ReferenceType> lca(TypeContext tCtx, Set<ReferenceType> types) {
        if (types.size() <= 1) {
            return types;
        } else {
            if (allRefArray(types)) {
                return lca(tCtx, types.stream().map(t -> (ReferenceType) ((ArrayType) t).elementType())
                        .collect(Collectors.toSet()))
                        .stream()
                        .map((t) -> Utils.wrap1(tCtx, t))
                        .collect(Collectors.toSet());
            }

            Set<ClassType> res = null;
            for (ReferenceType t : types) {
                Set<ClassType> current;
                if (t instanceof NullType) {
                    continue;
                } else if (t instanceof ArrayType at) {
                    current = Sets.newSet(getArraySupers(tCtx));
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

    public static ArrayType wrap1(TypeContext tCtx, ReferenceType referenceType) {
        if (referenceType instanceof ArrayType at) {
            return tCtx.typeSystem().getArrayType(at.baseType(), at.dimensions() + 1);
        } else {
            return tCtx.typeSystem().getArrayType(referenceType, 1);
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

    public static boolean isTwoWord(pascal.taie.language.type.Type t) {
        return t == DOUBLE || t == LONG;
    }

    public static boolean canHoldsInt(pascal.taie.language.type.Type t) {
        return t instanceof PrimitiveType p && p.asInt();
    }

    static boolean isSubtype(
            TypeContext tCtx,
            pascal.taie.language.type.Type supertype, pascal.taie.language.type.Type subtype) {
//        ClassHierarchy hierarchy = BuildContext.get().getClassHierarchy();
        ClassType OBJECT = tCtx.object();
        ClassType CLONEABLE = tCtx.cloneable();
        ClassType SERIALIZABLE = tCtx.serializable();

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

    public static boolean isPrimitiveArrayType(pascal.taie.language.type.Type t) {
        return t instanceof ArrayType at && canHoldsInt(at.baseType());
    }

    public static boolean isIntAssignable(pascal.taie.language.type.Type t1, pascal.taie.language.type.Type t2) {
        return canHoldsInt(t2) && canHoldsInt(t1);
    }

    /**
     * @return if <code>t1 := t2</code> is valid
     */
    public static boolean isAssignable(TypeContext tCtx,
                                       pascal.taie.language.type.Type t1,
                                       pascal.taie.language.type.Type t2) {
        if (t1 == t2) {
            return true;
        }
        if (t1 instanceof PrimitiveType) {
            return isIntAssignable(t1, t2);
        } else if (t1 == tCtx.reflectArray() && t2 instanceof ArrayType) {
            return true;
        } else {
            return isSubtype(tCtx, t1, t2);
        }
    }

    static PrimitiveType numericPromotion(PrimitiveType t1, PrimitiveType t2) {
        return toIntType(Math.max(fromIntTypeIndex(t1),  fromIntTypeIndex(t2)));
    }

    static int fromIntTypeIndex(PrimitiveType t) {
        if (t == LONG || t == FLOAT || t == DOUBLE) {
            throw new UnsupportedOperationException();
        } else {
            return getPrimitiveTypeIndex(t);
        }
    }

    static PrimitiveType toIntType(int i) {
        if (i >= 5) {
            throw new UnsupportedOperationException();
        } else {
            return getPrimitiveTypeByIndex(i);
        }
    }

    public static int getPrimitiveTypeIndex(PrimitiveType t) {
        if (t == BOOLEAN) {
            return 0;
        } else if (t == BYTE) {
            return 1;
        } else if (t == CHAR) {
            return 2;
        } else if (t == SHORT) {
            return 3;
        } else if (t == INT) {
            return 4;
        } else if (t == LONG) {
            return 5;
        } else if (t == FLOAT) {
            return 6;
        } else if (t == DOUBLE) {
            return 7;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static PrimitiveType getPrimitiveTypeByIndex(int i) {
        if (i == 0) {
            return BOOLEAN;
        } else if (i == 1) {
            return BYTE;
        } else if (i == 2) {
            return CHAR;
        } else if (i == 3) {
            return SHORT;
        } else if (i == 4) {
            return INT;
        } else if (i == 5) {
            return LONG;
        } else if (i == 6) {
            return FLOAT;
        } else if (i == 7) {
            return DOUBLE;
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
