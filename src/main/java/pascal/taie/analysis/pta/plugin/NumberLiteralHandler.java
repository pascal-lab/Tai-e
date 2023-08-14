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
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.ir.exp.NumberLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Handles {@link AssignLiteral} where the RValue is of {@link NumberLiteral}.
 */
public class NumberLiteralHandler implements Plugin {

    private static final Descriptor NUMBER_DESC = () -> "NumberObj";

    private final Map<JMethod, List<Pair<Var, Number>>> assignMap = Maps.newMap();

    private Solver solver;

    private HeapModel heapModel;

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        heapModel = solver.getHeapModel();
    }

    @Override
    public void onNewStmt(Stmt stmt, JMethod container) {
        if (stmt instanceof AssignLiteral assign &&
                assign.getRValue() instanceof NumberLiteral literal) {
            assignMap.computeIfAbsent(container, __ -> new ArrayList<>())
                    .add(new Pair<>(assign.getLValue(), literal.getNumber()));
        }
    }

    @Override
    public void onNewCSMethod(CSMethod csMethod) {
        var assigns = assignMap.get(csMethod.getMethod());
        if (assigns != null) {
            Context ctx = csMethod.getContext();
            assigns.forEach(pair -> {
                Var lhs = pair.first();
                Number number = pair.second();
                Obj numberObj = heapModel.getMockObj(
                        NUMBER_DESC, number, lhs.getType(), false);
                solver.addVarPointsTo(ctx, lhs, numberObj);
            });
        }
    }
}
