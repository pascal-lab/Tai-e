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

package pascal.taie.pta.element;

import pascal.taie.pta.statement.Statement;

import java.util.Optional;
import java.util.Set;

public interface Method {

    boolean isInstance();

    boolean isStatic();

    boolean isNative();

    /**
     * Declaring class of this method.
     */
    Type getClassType();

    String getName();

    String getSignature();

    Variable getThis();

    /**
     * @return number of parameters of this method.
     */
    int getParamCount();

    /**
     * @return the i-th parameter of this method. The return value is
     * present only if the parameter is of reference type.
     */
    Optional<Variable> getParam(int i);

    Set<Variable> getReturnVariables();

    void addStatement(Statement statement);

    Set<Statement> getStatements();
}
