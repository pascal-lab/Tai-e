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

import pascal.taie.ir.exp.ObjectExp;

/**
 * Model for heap objects.
 */
public interface HeapModel {

    /**
     * @return the abstract object for given object expression.
     */
    Obj getObj(ObjectExp exp);

    /**
     * @return the mock object which represents given argument.
     */
    <T> Obj getMockObj(T arg);
    
    // Special objects managed/created by Java runtime environment
    Obj getMainThread();

    Obj getSystemThreadGroup();

    Obj getMainThreadGroup();

    Obj getMainArgs();

    Obj getMainArgsElem();
}
