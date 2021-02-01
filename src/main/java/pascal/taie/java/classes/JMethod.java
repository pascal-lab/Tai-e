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

import java.util.Set;

public class JMethod extends ClassMember {

    private final Descriptor descriptor;

    public JMethod(JClass declaringClass, String name, Set<Modifier> modifiers,
                   Descriptor descriptor) {
        super(declaringClass, name, modifiers);
        this.descriptor = descriptor;
    }

    public Descriptor getDescriptor() {
        return descriptor;
    }

    public boolean isAbstract() {
        return Modifier.hasAbstract(modifiers);
    }

    public boolean isNative() {
        return Modifier.hasNative(modifiers);
    }
}
