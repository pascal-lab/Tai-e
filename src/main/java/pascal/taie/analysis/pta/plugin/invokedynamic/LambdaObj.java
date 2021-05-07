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

package pascal.taie.analysis.pta.plugin.invokedynamic;

import pascal.taie.analysis.pta.core.heap.MockObj;
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

/**
 * Representation of lambda functional objects.
 */
public class LambdaObj extends MockObj {

    public LambdaObj(Type type, InvokeDynamic invokeDynamic, JMethod container) {
        super(type, invokeDynamic, container);
    }

    @Override
    public InvokeDynamic getAllocation() {
        return (InvokeDynamic) super.getAllocation();
    }
}
