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

package pascal.taie.analysis.pta.plugin;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.ir.exp.NullLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.NullType;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

/**
 * Handles {@link AssignLiteral} var = null.
 */
public class NullHandler implements Plugin {

    private static final Descriptor NULL_DESC = () -> "NullObj";

    private final MultiMap<JMethod, Var> nullVars = Maps.newMultiMap();

    private Solver solver;

    private Obj nullObj;

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        nullObj = solver.getHeapModel().getMockObj(
                NULL_DESC, NullLiteral.get(), NullType.NULL, false);
    }

    @Override
    public void onNewStmt(Stmt stmt, JMethod container) {
        if (stmt instanceof AssignLiteral assign &&
                assign.getRValue() instanceof NullLiteral) {
            nullVars.put(container, assign.getLValue());
        }
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        Context ctx = csMethod.getContext();
        nullVars.get(csMethod.getMethod()).forEach(var ->
                solver.addVarPointsTo(ctx, var, nullObj));
    }
}
