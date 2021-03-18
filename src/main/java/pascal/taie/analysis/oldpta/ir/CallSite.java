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

package pascal.taie.analysis.oldpta.ir;

import pascal.taie.analysis.callgraph.CallKind;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.classes.JMethod;

public interface CallSite {

    /**
     * @return call kind of this call site.
     */
    CallKind getKind();

    /**
     * Set the call statement containing this call site.
     */
    void setCall(Call call);

    /**
     * @return the call statement containing this call site.
     */
    Call getCall();

    MethodRef getMethodRef();

    Variable getReceiver();

    /**
     * @return number of arguments of this call site.
     */
    int getArgCount();

    /**
     * @return the i-th argument of this call site. The return value is
     * present only if the argument is non-null and of reference type.
     */
    Variable getArg(int i);

    JMethod getContainerMethod();
}
