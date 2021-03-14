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

package pascal.taie.java.natives;

import pascal.taie.ir.NewIR;
import pascal.taie.java.ClassHierarchy;
import pascal.taie.java.TypeManager;
import pascal.taie.java.classes.JMethod;

/**
 * Builds empty IR for every native method.
 */
public class EmptyNativeModel extends AbstractNativeModel {

    public EmptyNativeModel(TypeManager typeManager, ClassHierarchy hierarchy) {
        super(typeManager, hierarchy);
    }

    @Override
    public NewIR buildNativeIR(JMethod method) {
        return new NativeIRBuilder(method).buildEmpty();
    }
}
