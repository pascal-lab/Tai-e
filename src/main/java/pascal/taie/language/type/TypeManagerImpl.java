/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.language.type;

import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClassLoader;
import pascal.taie.language.classes.StringReps;
import pascal.taie.util.AnalysisException;

import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import static pascal.taie.util.collection.Maps.newConcurrentMap;
import static pascal.taie.util.collection.Maps.newMap;
import static pascal.taie.util.collection.Maps.newSmallMap;

// TODO: optimize maps (classTypes and arrayTypes)
public class TypeManagerImpl implements TypeManager {

    private final ClassHierarchy hierarchy;

    private final Map<JClassLoader, Map<String, ClassType>> classTypes = newSmallMap();

    /**
     * This map may be concurrently written during IR construction,
     * thus we use concurrent map to ensure its thread-safety.
     */
    private final ConcurrentMap<Integer, ConcurrentMap<Type, ArrayType>> arrayTypes
            = newConcurrentMap(8);

    private final ClassType OBJECT;
    private final ClassType SERIALIZABLE;
    private final ClassType CLONEABLE;

    // Boxed types
    private final ClassType BOOLEAN;
    private final ClassType BYTE;
    private final ClassType SHORT;
    private final ClassType CHARACTER;
    private final ClassType INTEGER;
    private final ClassType LONG;
    private final ClassType FLOAT;
    private final ClassType DOUBLE;
    private final ClassType VOID;

    public TypeManagerImpl(ClassHierarchy hierarchy) {
        this.hierarchy = hierarchy;
        // Initialize special types
        JClassLoader loader = hierarchy.getBootstrapClassLoader();
        OBJECT = getClassType(loader, StringReps.OBJECT);
        SERIALIZABLE = getClassType(loader, StringReps.SERIALIZABLE);
        CLONEABLE = getClassType(loader, StringReps.CLONEABLE);
        BOOLEAN = getClassType(loader, StringReps.BOOLEAN);
        BYTE = getClassType(loader, StringReps.BYTE);
        SHORT = getClassType(loader, StringReps.SHORT);
        CHARACTER = getClassType(loader, StringReps.CHARACTER);
        INTEGER = getClassType(loader, StringReps.INTEGER);
        LONG = getClassType(loader, StringReps.LONG);
        FLOAT = getClassType(loader, StringReps.FLOAT);
        DOUBLE = getClassType(loader, StringReps.DOUBLE);
        VOID = getClassType(loader, StringReps.VOID);
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
        return getClassType(hierarchy.getDefaultClassLoader(), className);
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
    public ClassType getBoxedType(Type type) {
        if (type instanceof PrimitiveType) {
            switch ((PrimitiveType) type) {
                case BOOLEAN:
                    return BOOLEAN;
                case BYTE:
                    return BYTE;
                case SHORT:
                    return SHORT;
                case CHAR:
                    return CHARACTER;
                case INT:
                    return INTEGER;
                case LONG:
                    return LONG;
                case FLOAT:
                    return FLOAT;
                case DOUBLE:
                    return DOUBLE;
            }
        } else if (type instanceof VoidType) {
            return VOID;
        }
        throw new AnalysisException(type + " cannot be boxed");
    }

    @Override
    public Type getUnboxedType(ClassType type) {
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
        } else if (type.equals(VOID)) {
            return VoidType.VOID;
        }
        throw new AnalysisException(type + " cannot be unboxed");
    }

    @Override
    public boolean isSubtype(Type supertype, Type subtype) {
        if (subtype.equals(supertype)) {
            return true;
        } else if (subtype instanceof NullType) {
            return supertype instanceof ReferenceType;
        } else if (subtype instanceof ClassType) {
            if (supertype instanceof ClassType) {
                return hierarchy.isSubclass(
                        ((ClassType) supertype).getJClass(),
                        ((ClassType) subtype).getJClass());
            }
        } else if (subtype instanceof ArrayType) {
            if (supertype instanceof ClassType) {
                // JLS (11 Ed.), Chapter 10, Arrays
                return supertype == OBJECT ||
                        supertype == CLONEABLE ||
                        supertype == SERIALIZABLE;
            } else if (supertype instanceof ArrayType) {
                ArrayType superArray = (ArrayType) supertype;
                ArrayType subArray = (ArrayType) subtype;
                Type superBase = superArray.getBaseType();
                Type subBase = subArray.getBaseType();
                if (superArray.getDimensions() == subArray.getDimensions()) {
                    if (subBase.equals(superBase)) {
                        return true;
                    } else if (superBase instanceof ClassType &&
                            subBase instanceof ClassType) {
                        return hierarchy.isSubclass(
                                ((ClassType) superBase).getJClass(),
                                ((ClassType) subBase).getJClass());
                    }
                } else if (superArray.getDimensions() < subArray.getDimensions()) {
                    return superBase == OBJECT ||
                            superBase == CLONEABLE ||
                            superBase == SERIALIZABLE;
                }
            }
        }
        return false;
    }
}
