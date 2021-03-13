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

package pascal.taie.java.natives;

import pascal.taie.ir.DefaultNewIR;
import pascal.taie.ir.NewIR;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.java.classes.JMethod;
import pascal.taie.java.types.Type;
import pascal.taie.java.types.VoidType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static java.util.Collections.singletonList;
import static pascal.taie.util.CollectionUtils.freeze;

class NativeIRBuilder {

    private final static String THIS = "%native-this";

    private final static String PARAM = "%native-param";

    private final static String TEMP = "%native-temp";

    private final static String RETURN = "%native-ret";

    private final JMethod method;

    private Var thisVar;

    private List<Var> params;

    private int tempCounter = 0;

    private Var returnVar;

    private List<Var> vars = new ArrayList<>();

    NativeIRBuilder(JMethod method) {
        this.method = method;
        buildVars();
    }

    Var getThisVar() {
        return thisVar;
    }

    Var getParam(int i) {
        return params.get(i);
    }

    Var getReturnVar() {
        return returnVar;
    }

    Var newTempVar(Type type) {
        return newVar(TEMP + tempCounter++, type);
    }

    /**
     * Build an IR with empty body which contains only a return statement.
     */
    NewIR buildEmpty() {
        Return ret = returnVar != null ? new Return(returnVar) : new Return();
        return build(singletonList(ret));
    }

    NewIR build(List<Stmt> stmts) {
        return new DefaultNewIR(method, thisVar, freeze(params),
                singletonList(returnVar), freeze(vars),
                freeze(stmts), Collections.emptyList());
    }

    private void buildVars() {
        vars = new ArrayList<>();
        if (!method.isStatic()) {
            thisVar = newVar(THIS, method.getDeclaringClass().getType());
        }
        int counter = 1;
        params = new ArrayList<>(method.getParamCount());
        for (Type paramType : method.getParamTypes()) {
            params.add(newVar(PARAM + counter++, paramType));
        }
        Type retType = method.getReturnType();
        if (!retType.equals(VoidType.VOID)) {
            returnVar = newVar(RETURN, retType);
        }
    }

    private Var newVar(String name, Type type) {
        Var var = new Var(method, name, type);
        vars.add(var);
        return var;
    }
}
