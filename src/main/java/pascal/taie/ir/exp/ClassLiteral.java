/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.ir.exp;

import pascal.taie.java.World;
import pascal.taie.java.types.ClassType;
import pascal.taie.java.types.Type;

import static pascal.taie.java.classes.StringReps.CLASS;

public class ClassLiteral implements ReferenceLiteral<Type> {

    /**
     * The type represented by this class object.
     */
    private final Type value;

    private ClassLiteral(Type value) {
        this.value = value;
    }

    public static ClassLiteral get(Type value) {
        return new ClassLiteral(value);
    }

    @Override
    public ClassType getType() {
        // TODO: cache Class type in a static field? Doing so
        //  requires to reset the field when resetting World.
        return World.getTypeManager().getClassType(CLASS);
    }

    @Override
    public Type getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value.getName() + ".class";
    }
}
