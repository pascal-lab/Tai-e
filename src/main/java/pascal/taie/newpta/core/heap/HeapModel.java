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

package pascal.taie.newpta.core.heap;

import pascal.taie.ir.exp.NewExp;
import pascal.taie.java.types.Type;

/**
 * Model for heap objects.
 */
public interface HeapModel {

    /**
     * @return the abstract object for given object expression.
     */
    Obj getObj(NewExp newExp);

    /**
     * @return the constant object for given value.
     */
    <T> Obj getConstantObj(Type type, T value);

    /**
     * @return the mock object (e.g., taint value) which represents
     * given argument.
     */
    <T> Obj getMockObj(T value);
}
