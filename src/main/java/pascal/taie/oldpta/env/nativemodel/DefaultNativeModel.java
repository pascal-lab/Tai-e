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

package pascal.taie.oldpta.env.nativemodel;

import pascal.taie.java.ClassHierarchy;
import pascal.taie.java.TypeManager;
import pascal.taie.oldpta.ir.PTAIR;
import pascal.taie.oldpta.ir.Statement;

/**
 * Default modeling of native code.
 * Note that current modeling is not suitable for flow-sensitive analysis.
 *
 * TODO: for correctness, record which methods have been processed?
 */
class DefaultNativeModel implements NativeModel {

    private final MethodModel methodModel;

    private final CallModel callModel;

    private final FinalizerModel finalizerModel;

    DefaultNativeModel(ClassHierarchy hierarchy, TypeManager typeManager) {
        methodModel = new MethodModel(hierarchy, typeManager);
        callModel = new CallModel(hierarchy, typeManager);
        finalizerModel = new FinalizerModel(hierarchy);
    }

    @Override
    public void process(PTAIR ir) {
        methodModel.process(ir);
        // Statements may be changed by native model, thus we process on a copy
        Statement[] statements = ir.getStatements()
                .toArray(new Statement[0]);
        for (Statement s : statements) {
            callModel.process(s, ir);
            finalizerModel.process(s, ir);
        }
    }
}
