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
import bamboo.pta.element.Field;
import bamboo.pta.element.Method;
import bamboo.pta.element.Type;
import bamboo.pta.env.Environment;

import java.util.Collection;
import java.util.Optional;

public interface ProgramManager {

    Method getMainMethod();

    Collection<Method> getImplicitEntries();

    Environment getEnvironment();

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
     * TODO: make return value optional?
     */
    Type getUniqueTypeByName(String typeName);

    /**
     * Tries to get an unique class by name. The class may NOT
     * exist in the class path.
     */
    Optional<Type> tryGetUniqueTypeByName(String typeName);

    /**
     * Returns the field specified by the given field signature.
     * This method is supposed to be used when the caller is
     * confident that field with the given signature is unique.
     * Ideally, this method should only be used to retrieve
     * the fields in system classes, i.e., java.*.
     * TODO: make return value optional?
     */
    Field getUniqueFieldBySignature(String fieldSig);

    /**
     * Returns the method specified by the given method signature.
     * This method is supposed to be used when the caller is
     * confident that field with the given signature is unique.
     * Ideally, this method should only be used to retrieve
     * the methods in system classes, i.e., java.*.
     * TODO: make return value optional?
     */
    Method getUniqueMethodBySignature(String methodSig);
}
