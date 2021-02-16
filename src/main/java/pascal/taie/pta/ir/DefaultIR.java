/*
 * Tai-e - A Program Analysis Framework for Java
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
import java.util.Set;

/**
 * Default implementation of IR.
 */
public class DefaultIR implements IR {

    private final JMethod method;

    private Variable thisVar;

    private List<Variable> parameters;

    private Set<Variable> returnVars;

    private List<Statement> statements;

    public DefaultIR(JMethod method) {
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
    public int getParameterCount() {
        return parameters.size();
    }

    @Override
    public Variable getParameter(int i) {
        return parameters.get(i);
    }

    public void setParameters(List<Variable> parameters) {
        this.parameters = parameters;
    }

    @Override
    public Collection<Variable> getReturnVariables() {
        return returnVars;
    }

    public void setReturnVars(Set<Variable> returnVars) {
        this.returnVars = returnVars;
    }

    @Override
    public List<Statement> getStatements() {
        return statements;
    }

    public void setStatements(List<Statement> statements) {
        this.statements = statements;
    }
}
