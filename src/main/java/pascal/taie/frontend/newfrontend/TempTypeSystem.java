package pascal.taie.frontend.newfrontend;

import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClassLoader;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.BooleanType;
import pascal.taie.language.type.ByteType;
import pascal.taie.language.type.CharType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.DoubleType;
import pascal.taie.language.type.FloatType;
import pascal.taie.language.type.IntType;
import pascal.taie.language.type.LongType;
import pascal.taie.language.type.NullType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.ShortType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.language.type.VoidType;
import pascal.taie.util.AnalysisException;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static pascal.taie.util.collection.Maps.newConcurrentMap;

/**
 * Temporary Type System for frontend,
 * Copy and Paste from language.type.TypeSystemImpl
 */
public class TempTypeSystem implements TypeSystem {

    private final JClassLoader defaultClassLoader;

    private final Map<JClassLoader, Map<String, ClassType>> classTypes = newConcurrentMap();

    private final Map<String, ClassType> defaultClassTypes = newConcurrentMap();

    private final Map<String, ClassType> internalNameClassTypes = newConcurrentMap();

    /**
     * This map may be concurrently written during IR construction,
     * thus we use concurrent map to ensure its thread-safety.
     */
    private final ConcurrentMap<Integer, ConcurrentMap<Type, ArrayType>> arrayTypes
            = newConcurrentMap(8);


    private final Map<PrimitiveType, ClassType> boxedMap;

    private final Map<ClassType, PrimitiveType> unboxedMap;

    private final Map<String, PrimitiveType> primitiveTypes;

    public TempTypeSystem(JClassLoader loader) {
        defaultClassLoader = loader;
        boxedMap = Map.of(
                BooleanType.BOOLEAN, getClassType(loader, ClassNames.BOOLEAN),
                ByteType.BYTE, getClassType(loader, ClassNames.BYTE),
                ShortType.SHORT, getClassType(loader, ClassNames.SHORT),
                CharType.CHAR, getClassType(loader, ClassNames.CHARACTER),
                IntType.INT, getClassType(loader, ClassNames.INTEGER),
                LongType.LONG, getClassType(loader, ClassNames.LONG),
                FloatType.FLOAT, getClassType(loader, ClassNames.FLOAT),
                DoubleType.DOUBLE, getClassType(loader, ClassNames.DOUBLE));
        unboxedMap = boxedMap.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        primitiveTypes = boxedMap.keySet()
                .stream()
                .collect(Collectors.toMap(PrimitiveType::getName, t -> t));
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
            } else if (isPrimitiveType(typeName)) {
                return getPrimitiveType(typeName);
            } else if (typeName.equals(VoidType.VOID.getName())) {
                return VoidType.VOID;
            } else {
                return getClassType(loader, typeName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new AnalysisException("Invalid type name: " + typeName, e);
        }
    }

    @Override
    public Type getType(String typeName) {
        return getType(defaultClassLoader, typeName);
    }

    @Override
    public ClassType getClassType(JClassLoader loader, String className) {
//        assert loader == defaultClassLoader;
//        if (loader == defaultClassLoader) {
//            return defaultClassTypes.computeIfAbsent(className,
//                    name -> new ClassType(loader, name));
//        }
//        return classTypes.computeIfAbsent(loader, l -> newConcurrentMap())
//                .computeIfAbsent(className, name -> new ClassType(loader, name));
        return getClassTypeByInternalName(className.replace('.', '/'));
    }

    public ClassType getClassTypeByInternalName(String internalName) {
        return internalNameClassTypes.computeIfAbsent(internalName,
                name -> new ClassType(defaultClassLoader, name.replace('/', '.')));
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
    public PrimitiveType getPrimitiveType(String typeName) {
        return Objects.requireNonNull(primitiveTypes.get(typeName),
                typeName + " is not a primitive type");
    }

    @Override
    public ClassType getBoxedType(PrimitiveType type) {
        return boxedMap.get(type);
    }

    @Override
    public PrimitiveType getUnboxedType(ClassType type) {
        return Objects.requireNonNull(unboxedMap.get(type),
                type + " cannot be unboxed");
    }


    /**
     * This method should never be called
     */
    @Override
    public boolean isSubtype(Type supertype, Type subtype) {
        return Utils.isSubtype(supertype, subtype);
    }

    @Override
    public boolean isPrimitiveType(String typeName) {
        return false;
    }
}
