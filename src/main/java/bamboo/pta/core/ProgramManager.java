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

package bamboo.pta.core;

import bamboo.pta.element.CallSite;
import bamboo.pta.element.Method;
import bamboo.pta.element.Type;

import java.util.Collection;

public interface ProgramManager {

    Method getMainMethod();

    Collection<Method> getImplicitEntries();

    // -------------- type system ----------------
    boolean canAssign(Type from, Type to);

    Method resolveInterfaceOrVirtualCall(Type recvType, Method method);

    Method resolveSpecialCall(CallSite callSite, Method container);

    /**
     * Returns the type specified by the given type name.
     * This method is supposed to be used when the caller is
     * confident that type with the given name is unique.
     * Ideally, this method should only be used to retrieve
     * system classes, i.e., java.*.
     */
    Type getUniqueTypeByName(String typeName);
}
