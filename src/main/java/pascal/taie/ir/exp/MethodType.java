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

package pascal.taie.ir.exp;

import pascal.taie.World;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.HashUtils;

import java.util.List;

import static pascal.taie.language.classes.StringReps.METHOD_TYPE;
import static pascal.taie.language.classes.StringReps.toDescriptor;
import static pascal.taie.util.collection.CollectionUtils.freeze;

/**
 * Representation of java.lang.invoke.MethodType instances.
 */
public class MethodType implements ReferenceLiteral {

    private final List<Type> paramTypes;

    private final Type returnType;

    private MethodType(List<Type> paramTypes, Type returnType) {
        this.paramTypes = freeze(paramTypes);
        this.returnType = returnType;
    }

    public static MethodType get(List<Type> paramTypes, Type returnType) {
        return new MethodType(paramTypes, returnType);
    }

    public List<Type> getParamTypes() {
        return paramTypes;
    }

    public Type getReturnType() {
        return returnType;
    }

    @Override
    public ClassType getType() {
        return World.getTypeManager().getClassType(METHOD_TYPE);
    }

    @Override
    public <T> T accept(ExpVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MethodType that = (MethodType) o;
        return paramTypes.equals(that.paramTypes) &&
                returnType.equals(that.returnType);
    }

    @Override
    public int hashCode() {
        return HashUtils.hash(paramTypes, returnType);
    }

    @Override
    public String toString() {
        return "MethodType: " + toDescriptor(paramTypes, returnType);
    }
}
