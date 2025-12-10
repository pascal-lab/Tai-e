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

import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClassLoader;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.collection.Maps;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Abstract base class for TypeSystem implementations.
 */
public abstract class AbstractTypeSystem implements TypeSystem {

    protected final JClassLoader defaultClassLoader;

    /**
     * Maps from fully-qualified class name to the class type.
     * Note that currently, different classes with duplicate names (loaded by
     * different class loaders at runtime) are not supported in Tai-e type system,
     * so we only have one map from class names to the class types.
     * If we want to support duplicate class names in Tai-e, this map should be
     * extended with class loaders, like {@code Map<JClassLoader, Map<String, ClassType>>}.
     */
    protected final Map<String, ClassType> classTypes;

    /**
     * This map may be concurrently written during IR construction,
     * thus we use concurrent map to ensure its thread-safety.
     */
    private final ConcurrentMap<Integer, ConcurrentMap<Type, ArrayType>> arrayTypes;

    protected final ClassType objectType;

    protected final ClassType serializableType;

    protected final ClassType cloneableType;

    private final ClassType stringType;

    private final ClassType arrayType;

    private final ClassType classType;

    private final ClassType throwableType;

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

    protected AbstractTypeSystem(
            JClassLoader defaultClassLoader,
            Map<String, ClassType> classTypes,
            ConcurrentMap<Integer, ConcurrentMap<Type, ArrayType>> arrayTypes) {
        this.defaultClassLoader = defaultClassLoader;
        this.classTypes = classTypes;
        this.arrayTypes = arrayTypes;
        // Initialize commonly-used types
        objectType = getClassType(ClassNames.OBJECT);
        serializableType = getClassType(ClassNames.SERIALIZABLE);
        cloneableType = getClassType(ClassNames.CLONEABLE);
        stringType = getClassType(ClassNames.STRING);
        arrayType = getClassType(ClassNames.ARRAY);
        classType = getClassType(ClassNames.CLASS);
        throwableType = getClassType(ClassNames.THROWABLE);
        boxedMap = Map.of(
                BooleanType.BOOLEAN, getClassType(ClassNames.BOOLEAN),
                ByteType.BYTE, getClassType(ClassNames.BYTE),
                ShortType.SHORT, getClassType(ClassNames.SHORT),
                CharType.CHAR, getClassType(ClassNames.CHARACTER),
                IntType.INT, getClassType(ClassNames.INTEGER),
                LongType.LONG, getClassType(ClassNames.LONG),
                FloatType.FLOAT, getClassType(ClassNames.FLOAT),
                DoubleType.DOUBLE, getClassType(ClassNames.DOUBLE));
        unboxedMap = boxedMap.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
        primitiveTypes = boxedMap.keySet()
                .stream()
                .collect(Collectors.toMap(PrimitiveType::getName, t -> t));
    }

    protected AbstractTypeSystem(JClassLoader defaultClassLoader) {
        this(defaultClassLoader, Maps.newSmallMap(), Maps.newConcurrentMap(8));
    }

    @Override
    public ClassType objectType() {
        return objectType;
    }

    @Override
    public ClassType serializableType() {
        return serializableType;
    }

    @Override
    public ClassType cloneableType() {
        return cloneableType;
    }

    @Override
    public ClassType stringType() {
        return stringType;
    }

    @Override
    public ClassType arrayType() {
        return arrayType;
    }

    @Override
    public ClassType classType() {
        return classType;
    }

    @Override
    public ClassType throwableType() {
        return throwableType;
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
        return classTypes.computeIfAbsent(className,
                name -> new ClassType(loader, name));
    }

    @Override
    public ClassType getClassType(String className) {
        return getClassType(defaultClassLoader, className);
    }

    @Override
    public ArrayType getArrayType(Type baseType, int dim) {
        assert !(baseType instanceof VoidType)
                && !(baseType instanceof NullType);
        assert dim >= 1;
        return arrayTypes.computeIfAbsent(dim, d -> Maps.newConcurrentMap())
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
    public boolean isPrimitiveType(String typeName) {
        return primitiveTypes.containsKey(typeName);
    }
}
