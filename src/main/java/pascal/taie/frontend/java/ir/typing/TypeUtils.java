package pascal.taie.frontend.java.ir.typing;

import java.util.Optional;

import pascal.taie.frontend.java.FrontendTypeSystem;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.Type;

class TypeUtils {
    static Optional<Type> plusOneArray(Type t, FrontendTypeSystem typeSystem) {
        if (t instanceof NullType) {
            return Optional.empty();
        }
        Type baseType;
        int dim;
        if (t instanceof ArrayType at) {
            baseType = at.baseType();
            dim = at.dimensions() + 1;
        } else {
            baseType = t;
            dim = 1;
        }
        return Optional.of(typeSystem.getArrayType(baseType, dim));
    }

    static Optional<Type> subOneArray(Type t) {
        if (t instanceof ArrayType at) {
            return Optional.of(at.elementType());
        } else {
            return Optional.empty();
        }
    }
}
