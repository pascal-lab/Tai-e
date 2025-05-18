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

package pascal.taie.frontend.newfrontend.report;

import pascal.taie.ir.IR;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.PhiStmt;
import pascal.taie.util.collection.Sets;

import java.util.Set;

/**
 * This class is used to report the typing property of the IR.
 * It is used to check the number of join variables in the IR.
 */
public class TypingPropertyReporter {
    public static long getJoinVars(IR ir) {
        Set<Var> joinVars = Sets.newSet();
        Set<Var> visitedVars = Sets.newSet();
        for (var stmt : ir.getStmts()) {
            var def = stmt.getDef();
            // NON-SSA, no effect to SSA
            if (def.isPresent()) {
                LValue lv = def.get();
                if (lv instanceof Var v) {
                    if (visitedVars.contains(v)) {
                        joinVars.add(v);
                    }
                    visitedVars.add(v);
                }
            }
            // SSA
            if (stmt instanceof PhiStmt phi) {
                joinVars.add(phi.getLValue());
            }
        }
        return joinVars.size();
    }
}
