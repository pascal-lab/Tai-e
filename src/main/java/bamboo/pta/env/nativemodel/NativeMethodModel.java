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

/**
 * This class models native method by adding proper Statements
 * inside the method body.
 */
public interface NativeMethodModel {

    static NativeMethodModel getDefaultModel(
            ProgramManager pm, Environment env) {
        return new DefaultMethodModel(pm, env);
    }

    static NativeMethodModel getDummyModel() {
        return (m) -> {};
    }

    void process(Method method);
}
