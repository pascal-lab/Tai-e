/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.language.natives;

import pascal.taie.ir.IR;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.types.TypeManager;

/**
 * Builds empty IR for every native method.
 */
public class EmptyNativeModel extends AbstractNativeModel {

    public EmptyNativeModel(TypeManager typeManager, ClassHierarchy hierarchy) {
        super(typeManager, hierarchy);
    }

    @Override
    public IR buildNativeIR(JMethod method) {
        return new NativeIRBuilder(method).buildEmpty();
    }
}
