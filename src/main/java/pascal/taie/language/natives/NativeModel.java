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

package pascal.taie.language.natives;

import pascal.taie.ir.IR;
import pascal.taie.language.classes.JMethod;
import pascal.taie.analysis.pta.core.heap.Obj;

public interface NativeModel {

    Obj getMainThread();

    Obj getSystemThreadGroup();

    Obj getMainThreadGroup();

    Obj getMainArgs();

    Obj getMainArgsElem();

    IR buildNativeIR(JMethod method);
}
