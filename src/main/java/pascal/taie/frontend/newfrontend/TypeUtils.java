package pascal.taie.frontend.newfrontend;

import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;

public final class TypeUtils {

    public static final String JDT_INT = "int";
    public static final String JDT_BYTE = "byte";
    public static final String JDT_SHORT = "short";
    public static final String JDT_LONG = "long";
    public static final String JDT_FLOAT = "float";
    public static final String JDT_DOUBLE = "double";
    public static final String JDT_CHAR = "char";
    public static final String JDT_BOOLEAN = "boolean";

    public static String getErasedName(ITypeBinding iTypeBinding) {
        if (iTypeBinding.isPrimitive()) {
            return iTypeBinding.getName();
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
                default -> throw new NewFrontendException("Primitive Type Illegal: " + typeBinding.getName());
            };
        } else {
            throw new NewFrontendException("JDTTypeToTaieType: Not Implement.");
        }
    }
}
