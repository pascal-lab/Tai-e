package pascal.taie.dumpjvm;

import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.VoidType;

public class BinaryUtils {
    public static String computeDescriptor(Type type) {
        if (type instanceof ClassType) {
            return "L" + type.getName().replace('.', '/') + ";";
        } else if (type instanceof PrimitiveType) {
            return switch (type.getName()) {
                case "int" -> "I";
                case "long" -> "J";
                case "short" -> "S";
                case "byte" -> "B";
                case "char" -> "C";
                case "float" -> "F";
                case "double" -> "D";
                case "boolean" -> "Z";
                default -> throw new IllegalArgumentException("Unknown primitive type: " + type);
            };
        } else if (type instanceof VoidType) {
            return "V";
        } else if (type instanceof ArrayType arrayType) {
            return "[" + computeDescriptor(arrayType.elementType());
        } else {
            throw new IllegalArgumentException("Unknown type: " + type);
        }
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
