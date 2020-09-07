/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.panda.pta.env.nativemodel;

import pascal.panda.pta.core.ProgramManager;
import pascal.panda.pta.element.Method;
import pascal.panda.pta.statement.Statement;

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

    DefaultNativeModel(ProgramManager pm) {
        methodModel = new MethodModel(pm);
        callModel = new CallModel(pm);
        finalizerModel = new FinalizerModel(pm);
    }

    @Override
    public void process(Method method) {
        methodModel.process(method);
        Statement[] statements = method.getStatements()
                .toArray(new Statement[0]);
        for (Statement s : statements) {
            s.accept(callModel);
            s.accept(finalizerModel);
        }
    }
}
