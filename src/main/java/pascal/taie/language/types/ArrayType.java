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

package pascal.taie.language.types;

public class ArrayType implements ReferenceType {

    private final Type baseType;

    private final int dimensions;

    private final Type elementType;

    public ArrayType(Type baseType, int dimensions, Type elementType) {
        this.baseType = baseType;
        this.dimensions = dimensions;
        this.elementType = elementType;
    }

    public Type getBaseType() {
        return baseType;
    }

    public int getDimensions() {
        return dimensions;
    }

    public Type getElementType() {
        return elementType;
    }

    @Override
    public String getName() {
        return elementType + "[]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayType arrayType = (ArrayType) o;
        return dimensions == arrayType.dimensions
                && baseType.equals(arrayType.baseType);
    }

    @Override
    public int hashCode() {
        return baseType.hashCode() * 31 + dimensions * 17; // magic prime ...
    }

    @Override
    public String toString() {
        return getName();
    }
}
