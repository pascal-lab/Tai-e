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
import pascal.taie.ir.IRBuildHelper;
import pascal.taie.language.classes.JMethod;

/**
 * Builds empty IR for every native method.
 */
public class EmptyNativeModel implements NativeModel {

    @Override
    public IR buildNativeIR(JMethod method) {
        return new IRBuildHelper(method).buildEmpty();
    }
}
