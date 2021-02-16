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

package pascal.taie.pta.core;

import pascal.taie.pta.ir.CallSite;
import pascal.taie.java.classes.JField;
import pascal.taie.java.classes.JMethod;
import pascal.taie.pta.ir.Obj;
import pascal.taie.java.types.Type;
import pascal.taie.pta.env.Environment;

import java.util.Collection;
import java.util.Optional;

public interface ProgramManager {

    // -------------- program entry ----------------
    JMethod getMainMethod();

    Collection<JMethod> getImplicitEntries();

    void buildIRForAllMethods();

    Environment getEnvironment();

    // -------------- type system ----------------
    boolean canAssign(Type from, Type to);

    boolean isSubtype(Type parent, Type child);

    /**
     * Resolves callee by given receiver object and call site.
     */
    JMethod resolveCallee(Obj recvObj, CallSite callSite);

    /**
     * Dispatch callee based on receiver type and target method
     * @param recvType type of receiver object
     * @param target the target method
     * @return the callee
     */
    JMethod dispatch(Type recvType, JMethod target);

    // -------------- program element ----------------
    /**
     * @return the class initializer of given type.
     */
    Optional<JMethod> getClassInitializerOf(Type type);

    /**
     * Returns the type specified by the given type name.
     * This API is supposed to be used when the caller is
     * confident that type with the given name is unique.
     * Ideally, this API should only be used to retrieve
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
     * This API is supposed to be used when the caller is
     * confident that field with the given signature is unique.
     * Ideally, this API should only be used to retrieve
     * the fields in system classes, i.e., java.*.
     * TODO: make return value optional?
     */
    JField getUniqueFieldBySignature(String fieldSig);

    /**
     * Returns the method specified by the given method signature.
     * This API is supposed to be used when the caller is
     * confident that method with the given signature is unique.
     * Ideally, this API should only be used to retrieve
     * the methods in system classes, i.e., java.*.
     * TODO: make return value optional?
     */
    JMethod getUniqueMethodBySignature(String methodSig);
}
