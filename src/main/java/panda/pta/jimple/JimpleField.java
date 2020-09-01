/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package panda.pta.jimple;

import panda.pta.element.Field;
import soot.SootField;

class JimpleField implements Field {

    private final SootField field;

    private final JimpleType classType;

    private final JimpleType fieldType;

    public JimpleField(SootField field, JimpleType classType, JimpleType fieldType) {
        this.field = field;
        this.classType = classType;
        this.fieldType = fieldType;
    }

    @Override
    public boolean isInstance() {
        return !field.isStatic();
    }

    @Override
    public boolean isStatic() {
        return field.isStatic();
    }

    @Override
    public JimpleType getClassType() {
        return classType;
    }

    @Override
    public String getName() {
        return field.getName();
    }

    @Override
    public JimpleType getFieldType() {
        return fieldType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JimpleField that = (JimpleField) o;
        return field.equals(that.field);
    }

    @Override
    public int hashCode() {
        return field.hashCode();
    }

    @Override
    public String toString() {
        return field.toString();
    }
}
