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

import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.IR;
import pascal.taie.language.classes.JMethod;

public interface NativeModel {

    Obj getMainThread();

    Obj getSystemThreadGroup();

    Obj getMainThreadGroup();

    Obj getMainArgs();

    Obj getMainArgsElem();

    IR buildNativeIR(JMethod method);
}
