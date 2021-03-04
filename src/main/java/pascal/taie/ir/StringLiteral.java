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

package pascal.taie.ir;

import pascal.taie.java.World;
import pascal.taie.java.types.ClassType;

import static pascal.taie.java.classes.StringReps.STRING;

public class StringLiteral implements Literal {

    private static ClassType stringType;

    private final String value;

    private StringLiteral(String value) {
        this.value = value;
    }

    public static StringLiteral get(String value) {
        // TODO: canonicalize?
        return new StringLiteral(value);
    }

    @Override
    public ClassType getType() {
        if (stringType == null) {
            stringType = World.get()
                    .getTypeManager()
                    .getClassType(STRING);
        }
        return stringType;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
