package pascal.taie.frontend.newfrontend;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import pascal.taie.language.classes.Modifier;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.VoidType;

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
     * Get a Tai-e type from Primitive Asm Type
     * @param descriptor asm Type, should be a primitive type
     * @return the corresponding Tai-e Type
     */
    static pascal.taie.language.type.Type fromPrimitiveAsmType(String descriptor) {
        Type t = Type.getType(descriptor);

        if (t.getSort() == Type.VOID) {
            return VoidType.VOID;
        }
        else if (t.getSort() < Type.ARRAY) {
            return PrimitiveType.get(StringReps.toTaieTypeDesc(descriptor));
        } else {
            throw new IllegalArgumentException();
        }
    }
}
