package pascal.taie.frontend.newfrontend;

import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClassLoader;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.language.type.VoidType;
import pascal.taie.util.AnalysisException;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static pascal.taie.util.collection.Maps.newConcurrentMap;
import static pascal.taie.util.collection.Maps.newMap;

/**
 * Temporary Type System for frontend,
 * Copy && Paste from language.type.TypeSystemImpl
 */
public class TempTypeSystem implements TypeSystem {

    private final JClassLoader defaultClassLoader;

    private final Map<JClassLoader, Map<String, ClassType>> classTypes = newConcurrentMap();

    /**
     * This map may be concurrently written during IR construction,
     * thus we use concurrent map to ensure its thread-safety.
     */
    private final ConcurrentMap<Integer, ConcurrentMap<Type, ArrayType>> arrayTypes
            = newConcurrentMap(8);

    // Boxed types
    private final ClassType BOOLEAN;
    private final ClassType BYTE;
    private final ClassType SHORT;
    private final ClassType CHARACTER;
    private final ClassType INTEGER;
    private final ClassType LONG;
    private final ClassType FLOAT;
    private final ClassType DOUBLE;

    public TempTypeSystem(JClassLoader loader) {
        BOOLEAN = getClassType(loader, ClassNames.BOOLEAN);
        BYTE = getClassType(loader, ClassNames.BYTE);
        SHORT = getClassType(loader, ClassNames.SHORT);
        CHARACTER = getClassType(loader, ClassNames.CHARACTER);
        INTEGER = getClassType(loader, ClassNames.INTEGER);
        LONG = getClassType(loader, ClassNames.LONG);
        FLOAT = getClassType(loader, ClassNames.FLOAT);
        DOUBLE = getClassType(loader, ClassNames.DOUBLE);
        defaultClassLoader = loader;
    }

    @Override
    public Type getType(JClassLoader loader, String typeName) {
        try {
            if (typeName.endsWith("[]")) {
                int dim = 0;
                int i = typeName.length() - 1;
                while (i > 0) {
                    if (typeName.charAt(i - 1) == '[' && typeName.charAt(i) == ']') {
                        ++dim;
                        i -= 2;
                    } else {
                        break;
                    }
                }
                return getArrayType(
                        getType(loader, typeName.substring(0, i + 1)),
                        dim);
            } else if (PrimitiveType.isPrimitiveType(typeName)) {
                return PrimitiveType.get(typeName);
            } else {
                return getClassType(loader, typeName);
            }
        } catch (Exception e) {
            throw new AnalysisException("Invalid type name: " + typeName, e);
        }
    }

    @Override
    public Type getType(String typeName) {
        return getType(defaultClassLoader, typeName);
    }

    @Override
    public ClassType getClassType(JClassLoader loader, String className) {
        // FIXME: given a non-exist class name, this method will still return
        //  a ClassType with null JClass. This case should return null.
        return classTypes.computeIfAbsent(loader, l -> newMap())
                .computeIfAbsent(className, name -> new ClassType(loader, name));
    }

    @Override
    public ClassType getClassType(String className) {
        // TODO: add warning for missing class loader
        return getClassType(defaultClassLoader, className);
    }

    @Override
    public ArrayType getArrayType(Type baseType, int dim) {
        assert !(baseType instanceof VoidType)
                && !(baseType instanceof NullType);
        assert dim >= 1;
        return arrayTypes.computeIfAbsent(dim, d -> newConcurrentMap())
                .computeIfAbsent(baseType, t ->
                        new ArrayType(t, dim,
                                dim == 1 ? t : getArrayType(t, dim - 1)));
    }

    @Override
    public ClassType getBoxedType(PrimitiveType type) {
        return switch (type) {
            case BOOLEAN -> BOOLEAN;
            case BYTE -> BYTE;
            case SHORT -> SHORT;
            case CHAR -> CHARACTER;
            case INT -> INTEGER;
            case LONG -> LONG;
            case FLOAT -> FLOAT;
            case DOUBLE -> DOUBLE;
        };
    }

    @Override
    public PrimitiveType getUnboxedType(ClassType type) {
        if (type.equals(BOOLEAN)) {
            return PrimitiveType.BOOLEAN;
        } else if (type.equals(BYTE)) {
            return PrimitiveType.BYTE;
        } else if (type.equals(SHORT)) {
            return PrimitiveType.SHORT;
        } else if (type.equals(CHARACTER)) {
            return PrimitiveType.CHAR;
        } else if (type.equals(INTEGER)) {
            return PrimitiveType.INT;
        } else if (type.equals(LONG)) {
            return PrimitiveType.LONG;
        } else if (type.equals(FLOAT)) {
            return PrimitiveType.FLOAT;
        } else if (type.equals(DOUBLE)) {
            return PrimitiveType.DOUBLE;
        }
        throw new AnalysisException(type + " cannot be unboxed");
    }

    /**
     * This method should never be called
     */
    @Override
    public boolean isSubtype(Type supertype, Type subtype) {
        throw new IllegalStateException();
    }
}
