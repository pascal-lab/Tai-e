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

public interface NativeCallModel {

    static NativeCallModel getDefaultModel(
            ProgramManager pm, Environment env) {
        return new DefaultCallModel(pm, env);
    }

    static NativeCallModel getDummyModel() {
        return (m) -> {};
    }

    void process(Method container);
}
