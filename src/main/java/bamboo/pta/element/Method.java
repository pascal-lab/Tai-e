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

import java.util.List;
import java.util.Set;

public interface Method {

    boolean isInstance();

    boolean isStatic();

    boolean isNative();

    Type getClassType();

    String getName();

    Variable getThis();

    List<Variable> getParameters();

    Set<Variable> getReturnVariables();

    Set<Statement> getStatements();
}
