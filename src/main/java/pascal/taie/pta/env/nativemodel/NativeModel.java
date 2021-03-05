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

package pascal.taie.pta.env.nativemodel;

import pascal.taie.java.ClassHierarchy;
import pascal.taie.java.TypeManager;
import pascal.taie.java.World;
import pascal.taie.pta.ir.IR;

public interface NativeModel {

    static NativeModel getDefaultModel(
            ClassHierarchy hierarchy, TypeManager typeManager) {
        return new DefaultNativeModel(hierarchy, typeManager);
    }

    static NativeModel getDummyModel() {
        return (ir) -> {};
    }

    void process(IR ir);
}
