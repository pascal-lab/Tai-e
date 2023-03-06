package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;
import pascal.taie.language.annotation.*;
import pascal.taie.language.classes.Modifier;

import java.util.Arrays;
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
                node instanceof LabelNode;
    }
}
