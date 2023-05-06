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

package pascal.taie.analysis.exception;

import pascal.taie.World;
import pascal.taie.ir.IR;
import pascal.taie.ir.proginfo.ExceptionEntry;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Intra-procedural catch analysis for computing the exceptions thrown by
 * each Stmt will be caught by which Stmts, or not caught at all.
 */
public class CatchAnalysis {

    /**
     * Analyzes the exceptions thrown by each Stmt in given IR may be caught
     * by which (catch) Stmts, and which exceptions are not caught in the IR.
     */
    public static CatchResult analyze(IR ir, ThrowResult throwResult) {
        Map<Stmt, List<ExceptionEntry>> catchers = getPotentialCatchers(ir);
        TypeSystem typeSystem = World.get().getTypeSystem();
        CatchResult result = new CatchResult();
        ir.forEach(stmt -> {
            Collection<ClassType> implicit = throwResult.mayThrowImplicitly(stmt);
            Collection<ClassType> explicit;
            if (stmt instanceof Throw) {
                explicit = throwResult.mayThrowExplicitly((Throw) stmt);
            } else if (stmt instanceof Invoke) {
                explicit = throwResult.mayThrowExplicitly((Invoke) stmt);
            } else {
                explicit = List.of();
            }
            for (ExceptionEntry entry : catchers.getOrDefault(stmt, List.of())) {
                Set<ClassType> uncaughtImplicit = Sets.newHybridSet();
                implicit.forEach(t -> {
                    if (typeSystem.isSubtype(entry.catchType(), t)) {
                        result.addCaughtImplicit(stmt, entry.handler(), t);
                    } else {
                        uncaughtImplicit.add(t);
                    }
                });
                implicit = uncaughtImplicit;

                Set<ClassType> uncaughtExplicit = Sets.newHybridSet();
                explicit.forEach(t -> {
                    if (typeSystem.isSubtype(entry.catchType(), t)) {
                        result.addCaughtExplicit(stmt, entry.handler(), t);
                    } else {
                        uncaughtExplicit.add(t);
                    }
                });
                explicit = uncaughtExplicit;
            }
            implicit.forEach(e -> result.addUncaughtImplicit(stmt, e));
            explicit.forEach(e -> result.addUncaughtExplicit(stmt, e));
        });
        return result;
    }

    /**
     * @return a map from each Stmt to a list of exception entries which
     * may catch the exceptions thrown by the Stmt.
     */
    public static Map<Stmt, List<ExceptionEntry>> getPotentialCatchers(IR ir) {
        Map<Stmt, List<ExceptionEntry>> catchers = Maps.newLinkedHashMap();
        ir.getExceptionEntries().forEach(entry -> {
            for (int i = entry.start().getIndex(); i < entry.end().getIndex(); ++i) {
                Stmt stmt = ir.getStmt(i);
                catchers.computeIfAbsent(stmt, __ -> new ArrayList<>())
                        .add(entry);
            }
        });
        return catchers;
    }
}
