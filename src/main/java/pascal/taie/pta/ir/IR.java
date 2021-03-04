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

package pascal.taie.pta.ir;

import pascal.taie.java.classes.JMethod;

import java.util.Collection;
import java.util.List;

/**
 * Intermediate representation of method body.
 */
public interface IR {

    JMethod getMethod();

    Variable getThis();

    int getParamCount();

    Variable getParam(int i);

    Collection<Variable> getReturnVariables();

    List<Statement> getStatements();

    void addStatement(Statement s);
}
