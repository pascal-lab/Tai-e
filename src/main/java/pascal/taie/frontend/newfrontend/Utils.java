package pascal.taie.frontend.newfrontend;

import org.apache.commons.lang3.NotImplementedException;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.DoubleLiteral;
import pascal.taie.ir.exp.FloatLiteral;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.LongLiteral;
import pascal.taie.ir.exp.MethodHandle;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.ir.proginfo.MemberRef;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.annotation.ArrayElement;
import pascal.taie.language.annotation.BooleanElement;
import pascal.taie.language.annotation.ClassElement;
import pascal.taie.language.annotation.DoubleElement;
import pascal.taie.language.annotation.Element;
import pascal.taie.language.annotation.IntElement;
import pascal.taie.language.annotation.LongElement;
import pascal.taie.language.annotation.StringElement;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.Modifier;
import pascal.taie.util.collection.Pair;

import java.util.Arrays;
import java.util.List;
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
        return Arrays.stream(Modifier.values())
                .filter(i -> hasAsmModifier(opcodes, toAsmModifier(i)))
                .collect(Collectors.toSet());
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
        } else if (ele instanceof Object[] a) {
            return new ArrayElement(
                    Arrays.stream(a).map(Utils::toElement).toList());
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

    static  boolean isThrow(AbstractInsnNode node) {
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
            throw new NotImplementedException();
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

    static String getThrowable() {
        return "java/lang/Throwable";
    }
}
