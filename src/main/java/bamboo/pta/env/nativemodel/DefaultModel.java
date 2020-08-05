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

package bamboo.pta.env.nativemodel;

import bamboo.pta.core.ProgramManager;
import bamboo.pta.element.Method;
import bamboo.pta.env.Environment;

// TODO: for correctness, record which methods have been processed?
class DefaultModel implements NativeModel {

    private final MethodModel methodModel;
    private final CallModel callModel;

    DefaultModel(ProgramManager pm, Environment env) {
        methodModel = new MethodModel(pm, env);
        callModel = new CallModel(pm, env);
    }

    @Override
    public void process(Method method) {
        if (method.isNative()) {
            methodModel.process(method);
        } else {
            callModel.process(method);
        }
    }
}
