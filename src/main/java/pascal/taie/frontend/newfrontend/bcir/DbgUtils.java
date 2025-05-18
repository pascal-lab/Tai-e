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

package pascal.taie.frontend.newfrontend.bcir;

import pascal.taie.World;
import pascal.taie.analysis.misc.IRDumper;
import pascal.taie.config.AnalysisConfig;
import pascal.taie.ir.exp.LValue;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.JClass;

import java.util.List;
import java.util.Optional;

/**
 * Some debugging utilities for BCIR.
 * Basically used for interactive debugging.
 * E.g., evaluate expression in IDE.
 */
public class DbgUtils {
    public static List<Stmt> findDef(BytecodeIRBuilder builder, Var v) {
        return builder.getAllStmts().stream()
                .map(stmt -> filterDef(stmt, v))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private static Optional<Stmt> filterDef(Stmt stmt, Var v) {
        Optional<LValue> def = stmt.getDef();
        if (def.isPresent() && def.get().equals(v)) {
            return Optional.of(stmt);
        }
        return Optional.empty();
    }

    public static void dumpTIR(String className) {
        JClass klass = World.get().getClassHierarchy().getClass(className);
        if (klass == null) {
            System.out.println("Class not found: " + className);
            return;
        }
        IRDumper dumper = new IRDumper(AnalysisConfig.of(IRDumper.ID));
        dumper.analyze(klass);
    }
}
