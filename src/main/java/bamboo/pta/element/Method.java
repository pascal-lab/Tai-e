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

import bamboo.pta.statement.Statement;

import java.util.Optional;
import java.util.Set;

public interface Method {

    boolean isInstance();

    boolean isStatic();

    boolean isNative();

    Type getClassType();

    String getName();

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

    Set<Statement> getStatements();
}
