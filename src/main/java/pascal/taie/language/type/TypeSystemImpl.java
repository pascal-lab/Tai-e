/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.language.type;

import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClassLoader;
import pascal.taie.util.AnalysisException;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static pascal.taie.util.collection.Maps.newConcurrentMap;
import static pascal.taie.util.collection.Maps.newMap;
import static pascal.taie.util.collection.Maps.newSmallMap;

// TODO: optimize maps (classTypes and arrayTypes)
public class TypeSystemImpl implements TypeSystem {

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

    /**
     * Maps a primitive type to its boxed type.
     */
    private final Map<PrimitiveType, ClassType> boxedMap;

    /**
     * Maps a boxed class type to its unboxed primitive type.
     */
    private final Map<ClassType, PrimitiveType> unboxedMap;

    /**
     * Maps a primitive type name to the corresponding type.
     */
    private final Map<String, PrimitiveType> primitiveTypes;

    public TypeSystemImpl(ClassHierarchy hierarchy) {
        this.hierarchy = hierarchy;
        // Initialize special types
        JClassLoader loader = hierarchy.getBootstrapClassLoader();
        OBJECT = getClassType(loader, ClassNames.OBJECT);
        SERIALIZABLE = getClassType(loader, ClassNames.SERIALIZABLE);
        CLONEABLE = getClassType(loader, ClassNames.CLONEABLE);
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
            } else {
                return getClassType(loader, typeName);
            }
        } catch (Exception e) {
            throw new AnalysisException("Invalid type name: " + typeName, e);
        }
    }

    @Override
    public Type getType(String typeName) {
        return getType(hierarchy.getDefaultClassLoader(), typeName);
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
            } else if (supertype instanceof ArrayType superArray) {
                ArrayType subArray = (ArrayType) subtype;
                Type superBase = superArray.baseType();
                Type subBase = subArray.baseType();
                if (superArray.dimensions() == subArray.dimensions()) {
                    if (subBase.equals(superBase)) {
                        return true;
                    } else if (superBase instanceof ClassType &&
                            subBase instanceof ClassType) {
                        return hierarchy.isSubclass(
                                ((ClassType) superBase).getJClass(),
                                ((ClassType) subBase).getJClass());
                    }
                } else if (superArray.dimensions() < subArray.dimensions()) {
                    return superBase == OBJECT ||
                            superBase == CLONEABLE ||
                            superBase == SERIALIZABLE;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isPrimitiveType(String typeName) {
        return primitiveTypes.containsKey(typeName);
    }
}
