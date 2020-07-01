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

package bamboo.pta.element;

import bamboo.pta.statement.Call;

import java.util.List;

public interface CallSite {

    boolean isInterface();

    boolean isVirtual();

    boolean isSpecial();

    boolean isStatic();

    /**
     * @return the call statements containing this call site.
     */
    Call getCall();

    Method getMethod();

    Variable getReceiver();

    List<Variable> getArguments();

    Method getContainerMethod();
}
