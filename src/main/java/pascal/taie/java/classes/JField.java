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

package pascal.taie.java.classes;

import pascal.taie.java.types.Type;

import java.util.Set;

public class JField extends ClassMember {

    private final Type type;

    public JField(JClass declaringClass, String name, Set<Modifier> modifiers,
                  Type type) {
        super(declaringClass, name, modifiers);
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    // TODO: more modifiers
}
