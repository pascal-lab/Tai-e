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

package pascal.taie.frontend.soot;

import pascal.taie.ir.DefaultNewIR;
import pascal.taie.ir.NewIR;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.java.classes.JMethod;
import pascal.taie.java.types.Type;
import soot.Body;
import soot.Local;
import soot.Value;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static pascal.taie.util.CollectionUtils.freeze;

class MethodIRBuilder {

    private final JMethod method;

    private final Converter converter;

    private VarManager varManager;

    private List<Stmt> stmts;

    MethodIRBuilder(JMethod method, Converter converter) {
        this.method = method;
        this.converter = converter;
    }

    NewIR build() {
        Body body = method.getSootMethod().retrieveActiveBody();
        varManager = new VarManager();
        stmts = new ArrayList<>();
        if (!method.isStatic()) {
            buildThis(body);
        }
        buildParams(body);
        buildStmts(body);
        return new DefaultNewIR(method,
                varManager.getThis(), freeze(varManager.getParams()),
                freeze(varManager.getVars()), freeze(stmts));
    }

    private void buildThis(Body body) {
        varManager.addThis(body.getThisLocal());
    }

    private void buildParams(Body body) {
        body.getParameterLocals().forEach(varManager::addParam);
    }

    private void buildStmts(Body body) {

    }

    /**
     * Shortcut for obtaining and converting the type of soot.Value.
     */
    private Type getType(Value value) {
        return converter.convertType(value.getType());
    }

    private class VarManager {

        private final static String THIS = "#this";

        private final static String PARAM = "#param";

        private final Map<Local, Var> varMap = new LinkedHashMap<>();

        private final List<Var> vars = new ArrayList<>();

        private Var thisVar;

        private final List<Var> params = new ArrayList<>();

        private void addThis(Local thisLocal) {
            thisVar = newVar(THIS, getType(thisLocal));
            varMap.put(thisLocal, thisVar);
        }

        private void addNativeThis(Type thisType) {
            thisVar = newVar(THIS, thisType);
        }

        private void addParam(Local paramLocal) {
            Var param = newVar(paramLocal.getName(), getType(paramLocal));
            params.add(param);
        }

        private void addNativeParam(Type paramType) {
            Var param = newVar(PARAM + params.size(), paramType);
            params.add(param);
        }

        private Var getVar(Local local) {
            return varMap.computeIfAbsent(local, l ->
                    newVar(l.getName(), getType(l)));
        }

        private Var newVar(String name, Type type) {
            Var var = new Var(name, type);
            vars.add(var);
            return var;
        }

        private Var getThis() {
            return thisVar;
        }

        public List<Var> getParams() {
            return params;
        }

        private List<Var> getVars() {
            return vars;
        }
    }
}
