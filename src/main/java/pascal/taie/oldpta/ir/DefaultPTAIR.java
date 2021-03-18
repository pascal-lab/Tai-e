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

package pascal.taie.oldpta.ir;

import pascal.taie.language.classes.JMethod;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static pascal.taie.util.CollectionUtils.newHybridSet;

/**
 * Default implementation of IR.
 */
public class DefaultPTAIR implements PTAIR {

    private final JMethod method;

    private Variable thisVar;

    private List<Variable> params = Collections.emptyList();

    private Set<Variable> returnVars = Collections.emptySet();

    private List<Statement> ptaStatements = Collections.emptyList();

    public DefaultPTAIR(JMethod method) {
        this.method = method;
    }

    @Override
    public JMethod getMethod() {
        return method;
    }

    @Override
    public Variable getThis() {
        return thisVar;
    }

    public void setThis(Variable thisVar) {
        this.thisVar = thisVar;
    }

    @Override
    public int getParamCount() {
        return params.size();
    }

    @Override
    public Variable getParam(int i) {
        return params.get(i);
    }

    public void setParams(List<Variable> params) {
        this.params = params;
    }

    @Override
    public Collection<Variable> getReturnVariables() {
        return returnVars;
    }

    public void setReturnVars(Set<Variable> returnVars) {
        this.returnVars = returnVars;
    }

    public void addReturnVar(Variable returnVar) {
        if (returnVars.isEmpty()) {
            returnVars = newHybridSet();
        }
        returnVars.add(returnVar);
    }

    @Override
    public List<Statement> getStatements() {
        return ptaStatements;
    }

    @Override
    public void addStatement(Statement s) {
        if (ptaStatements.isEmpty()) {
            ptaStatements = new ArrayList<>();
        }
        ptaStatements.add(s);
    }
}
