/*
 * Tai'e - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai'e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.pta.env.nativemodel;

import pascal.taie.pta.core.ProgramManager;
import pascal.taie.pta.element.Method;

public interface NativeModel {

    static NativeModel getDefaultModel(ProgramManager pm) {
        return new DefaultNativeModel(pm);
    }

    static NativeModel getDummyModel() {
        return (method) -> {};
    }

    void process(Method method);
}
