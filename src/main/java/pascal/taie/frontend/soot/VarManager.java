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

import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.Literal;
import pascal.taie.ir.exp.NullLiteral;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.java.types.Type;
import soot.Local;
import soot.Value;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class VarManager {

    private final static String THIS = "#this";

    private final static String PARAM = "#param";

    private final static String STRING_CONSTANT = "#stringconstant";

    private final static String CLASS_CONSTANT = "#classconstant";

    private final static String NULL_CONSTANT = "#nullconstant";

    private final Converter converter;

    private final Map<Local, Var> varMap = new LinkedHashMap<>();

    private final List<Var> vars = new ArrayList<>();

    private Var thisVar;

    private final List<Var> params = new ArrayList<>();

    /**
     * Counter for temporary constant variables.
     */
    private int counter = 0;

    VarManager(Converter converter) {
        this.converter = converter;
    }

    void addThis(Local thisLocal) {
        thisVar = newVar(THIS, getType(thisLocal));
        varMap.put(thisLocal, thisVar);
    }

    private void addNativeThis(Type thisType) {
        thisVar = newVar(THIS, thisType);
        // TODO: add to varMap?
    }

    void addParam(Local paramLocal) {
        params.add(getVar(paramLocal));
    }

    private void addNativeParam(Type paramType) {
        Var param = newVar(PARAM + params.size(), paramType);
        params.add(param);
        // TODO: add to varMap?
    }

    Var getVar(Local local) {
        return varMap.computeIfAbsent(local, l ->
                newVar(l.getName(), getType(l)));
    }

    /**
     * @return a new temporary variable that holds given literal value.
     */
    Var newConstantVar(Literal literal) {
        String varName;
        if (literal instanceof StringLiteral) {
            varName = STRING_CONSTANT + counter++;
        } else if (literal instanceof ClassLiteral) {
            varName = CLASS_CONSTANT + counter++;
        } else if (literal instanceof NullLiteral) {
            varName = NULL_CONSTANT + counter++;
        } else {
            varName = "#" + literal.getType().getName() +
                    "constant" + counter++;
        }
        return newVar(varName, literal.getType());
    }

    Var getThis() {
        return thisVar;
    }

    List<Var> getParams() {
        return params;
    }

    List<Var> getVars() {
        return vars;
    }

    private Var newVar(String name, Type type) {
        Var var = new Var(name, type);
        vars.add(var);
        return var;
    }

    /**
     * Shortcut: obtain Jimple Value's Type and convert to Tai-e Type.
     */
    private Type getType(Value value) {
        return converter.convertType(value.getType());
    }
}
