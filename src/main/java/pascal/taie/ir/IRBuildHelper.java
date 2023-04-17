/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.ir;

import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Return;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.VoidType;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Helper for building IR for a method from scratch.
 * Given a {@link JMethod}, this helper automatically creates {@code this}
 * variable (for instance method), parameters, and return variable (if method
 * return type is not {@code void}) for the method.
 */
public class IRBuildHelper {

    private static final String THIS = "%this";

    private static final String PARAM = "%param";

    private static final String TEMP = "%temp";

    private static final String RETURN = "%return";

    private final JMethod method;

    private final Var thisVar;

    private final List<Var> params;

    private final Var returnVar;

    private final Set<Var> returnVars;

    private final List<Var> vars;

    /**
     * Counter for indexing all variables.
     */
    private int varCounter = 0;

    /**
     * Counter for naming temporary variables.
     */
    private int tempCounter = 0;

    public IRBuildHelper(JMethod method) {
        this.method = method;
        // build this variable
        vars = new ArrayList<>();
        thisVar = method.isStatic() ? null :
                newVar(THIS, method.getDeclaringClass().getType());
        // build parameters
        params = new ArrayList<>(method.getParamCount());
        for (int i = 0; i < method.getParamCount(); ++i) {
            params.add(newVar(PARAM + i, method.getParamType(i)));
        }
        // build return variable
        Type retType = method.getReturnType();
        if (!retType.equals(VoidType.VOID)) {
            returnVar = newVar(RETURN, retType);
            returnVars = Set.of(returnVar);
        } else {
            returnVar = null;
            returnVars = Set.of();
        }
    }

    /**
     * @return {@code this} variable of the IR being built.
     * @throws AssertionError if the method is not an instance method.
     */
    public Var getThisVar() {
        assert thisVar != null;
        return thisVar;
    }

    public Var getParam(int i) {
        return params.get(i);
    }

    /**
     * @return the return variable of the IR being built.
     */
    public Var getReturnVar() {
        return returnVar;
    }

    /**
     * @return a new temporary variable of given type.
     */
    public Var newTempVar(Type type) {
        return newVar(TEMP + tempCounter++, type);
    }

    /**
     * @return a new return statement of the IR being built.
     */
    public Return newReturn() {
        return returnVar != null ? new Return(returnVar) : new Return();
    }

    /**
     * Builds an emtpy IR which contains only a {@link Return} statement.
     */
    public IR buildEmpty() {
        return build(List.of(newReturn()));
    }

    /**
     * Builds an IR with given {@link Stmt}s. This method sets the indexes
     * of given {@link Stmt}s, so client code does not need to set the indexes.
     *
     * @param stmts statements of the IR being built.
     */
    public IR build(List<Stmt> stmts) {
        int i = 0;
        for (Stmt stmt : stmts) {
            stmt.setIndex(i++);
        }
        return new DefaultIR(method, thisVar, params, returnVars,
                vars, stmts, List.of());
    }

    private Var newVar(String name, Type type) {
        Var var = new Var(method, name, type, varCounter++);
        vars.add(var);
        return var;
    }
}
