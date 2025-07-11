package pascal.taie.dumpjvm;

import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.VoidType;

import java.util.Optional;

public class BinaryUtils {
    public static String computeDescriptor(Type type) {
        if (type instanceof ClassType) {
            return "L" + type.getName().replace('.', '/') + ";";
        } else if (type instanceof PrimitiveType) {
            return computePrimitive(type.getName()).orElseThrow();
        } else if (type instanceof VoidType) {
            return "V";
        } else if (type instanceof ArrayType arrayType) {
            return "[" + computeDescriptor(arrayType.elementType());
        } else {
            throw new IllegalArgumentException("Unknown type: " + type);
        }
    }

    public static Optional<String> computePrimitive(String type) {
        return switch (type) {
            case "int"     -> Optional.of("I");
            case "long"    -> Optional.of("J");
            case "short"   -> Optional.of("S");
            case "byte"    -> Optional.of("B");
            case "char"    -> Optional.of("C");
            case "float"   -> Optional.of("F");
            case "double"  -> Optional.of("D");
            case "boolean" -> Optional.of("Z");
            default -> Optional.empty();
        };
    }

    public static String computeDescriptor(JMethod method) {
        StringBuilder sb = new StringBuilder("(");
        for (int i = 0; i < method.getParamCount(); ++i) {
            sb.append(computeDescriptor(method.getParamType(i)));
        }
        sb.append(")");
        sb.append(computeDescriptor(method.getReturnType()));
        return sb.toString();
    }

}
