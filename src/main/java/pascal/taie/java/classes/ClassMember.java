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

public abstract class ClassMember {

    protected JClass declaringClass;

    protected String name;

    protected Set<Modifier> modifiers;

    // TODO: annotations, source location

    protected ClassMember(JClass declaringClass, String name,
                          Set<Modifier> modifiers) {
        this.declaringClass = declaringClass;
        this.name = name;
        this.modifiers = modifiers;
    }

    public JClass getDeclaringClass() {
        return declaringClass;
    }

    protected void setDeclaringClass(JClass declaringClass) {
        this.declaringClass = declaringClass;
    }

    public String getName() {
        return name;
    }

    public boolean isPublic() {
        return Modifier.hasPublic(modifiers);
    }

    public boolean isProtected() {
        return Modifier.hasProtected(modifiers);
    }

    public boolean isPrivate() {
        return Modifier.hasPrivate(modifiers);
    }

    public boolean isStatic() {
        return Modifier.hasStatic(modifiers);
    }
}
