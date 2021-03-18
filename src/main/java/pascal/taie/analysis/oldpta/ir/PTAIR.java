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

package pascal.taie.analysis.oldpta.ir;

import pascal.taie.language.classes.JMethod;

import java.util.Collection;
import java.util.List;

/**
 * Intermediate representation of method body.
 */
public interface PTAIR {

    JMethod getMethod();

    Variable getThis();

    int getParamCount();

    Variable getParam(int i);

    Collection<Variable> getReturnVariables();

    List<Statement> getStatements();

    void addStatement(Statement s);
}
