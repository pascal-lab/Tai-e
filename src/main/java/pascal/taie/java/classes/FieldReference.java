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

public class FieldReference extends MemberReference {

    private final Type type;

    /**
     * Whether this field reference has been resolved.
     */
    private boolean isResolved = false;

    /**
     * Cache the resolved field for this reference to avoid redundant
     * field resolution.
     */
    private JField field;

    public FieldReference(JClass declaringClass, String name, Type type) {
        super(declaringClass, name);
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public boolean isResolved() {
        return isResolved;
    }

    public void setResolved(boolean resolved) {
        isResolved = resolved;
    }

    public JField getField() {
        return field;
    }

    public void setField(JField field) {
        this.field = field;
    }
}
