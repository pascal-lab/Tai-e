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
import pascal.taie.java.classes.JMethod;
import pascal.taie.java.types.Type;
import pascal.taie.java.types.VoidType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static pascal.taie.util.CollectionUtils.freeze;

class NativeIRBuilder {

    private final static String THIS = "%native-this";

    private final static String PARAM = "%native-param";

    private final static String RETURN = "%native-ret";

    private final JMethod method;

    private List<Var> vars = new ArrayList<>();

    NativeIRBuilder(JMethod method) {
        this.method = method;
    }

    /**
     * Build an empty method method (variables are absent but
     * without any statements) for the method.
     */
    NewIR buildEmpty() {
        vars = new ArrayList<>();
        Var thisVar = null;
        if (!method.isStatic()) {
            thisVar = newVar(THIS, method.getDeclaringClass().getType());
        }
        int counter = 1;
        List<Var> params = new ArrayList<>(method.getParamCount());
        for (Type paramType : method.getParamTypes()) {
            params.add(newVar(PARAM + counter++, paramType));
        }
        List<Var> returnVars;
        Type retType = method.getReturnType();
        if (retType.equals(VoidType.VOID)) {
            returnVars = Collections.emptyList();
        } else {
            returnVars = Collections.singletonList(newVar(RETURN, retType));
        }
        return new DefaultNewIR(method, thisVar, freeze(params),
                returnVars, freeze(vars),
                Collections.emptyList(), Collections.emptyList());
    }

    private Var newVar(String name, Type type) {
        Var var = new Var(method, name, type);
        vars.add(var);
        return var;
    }
}
