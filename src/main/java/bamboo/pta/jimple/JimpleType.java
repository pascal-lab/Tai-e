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

package bamboo.pta.jimple;

import bamboo.pta.element.Type;
import soot.ArrayType;
import soot.RefType;
import soot.SootClass;

class JimpleType implements Type {

    private soot.Type sootType;

    /**
     * If this type is array type, then elementType is the type of the
     * elements of the array.
     */
    private Type elementType;

    private SootClass sootClass;

    JimpleType(soot.Type sootType) {
        this.sootType = sootType;
        if (sootType instanceof RefType) {
            this.sootClass = ((RefType) sootType).getSootClass();
        }
    }

    JimpleType(soot.Type arrayType, Type elementType) {
        this.sootType = arrayType;
        this.elementType = elementType;
    }

    SootClass getSootClass() {
        return sootClass;
    }

    soot.Type getSootType() {
        return sootType;
    }

    @Override
    public String getName() {
        return sootType.toString();
    }

    @Override
    public boolean isArray() {
        return sootType instanceof ArrayType;
    }

    @Override
    public Type getElementType() {
        return elementType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JimpleType that = (JimpleType) o;
        return sootType.equals(that.sootType);
    }

    @Override
    public int hashCode() {
        return sootType.hashCode();
    }

    @Override
    public String toString() {
        return sootType.toString();
    }
}
