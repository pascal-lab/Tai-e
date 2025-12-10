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

package pascal.taie.frontend.java;

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
import pascal.taie.frontend.java.type.FrontendTypeSystem;
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
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.StringReps;
import pascal.taie.util.collection.Pair;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility functions for frontend.
 * <p>
 * Note: Type-related operations have been moved to {@link FrontendTypeSystem}.
 */
public class Utils {

    public static String getBinaryName(String internalName) {
        return Type.getObjectType(internalName).getClassName();
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

    public static Literal fromObject(FrontendTypeSystem typeSystem, Object o) {
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
                return typeSystem.toMethodType(t);
            } else {
                return ClassLiteral.get(typeSystem.fromAsmType(t));
            }
        } else if (o instanceof Handle handle) {
            return fromAsmHandle(typeSystem, handle);
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public static MethodHandle fromAsmHandle(FrontendTypeSystem typeSystem, Handle handle) {
        MethodHandle.Kind kind = toMethodHandleKind(handle.getTag());
        MemberRef ref;
        JClass jClass = typeSystem.toJClass(handle.getOwner());
        if (isFieldKind(kind)) {
            pascal.taie.language.type.Type t =
                    typeSystem.fromAsmType(handle.getDesc());
            ref = FieldRef.get(jClass, handle.getName(), t,
                    kind == MethodHandle.Kind.REF_getStatic ||
                            kind == MethodHandle.Kind.REF_putStatic);
        } else {
            Pair<List<pascal.taie.language.type.Type>, pascal.taie.language.type.Type>
                    mtdType = typeSystem.fromAsmMethodType(handle.getDesc());
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
            } else if (e instanceof NewExp newExp) {
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
}
