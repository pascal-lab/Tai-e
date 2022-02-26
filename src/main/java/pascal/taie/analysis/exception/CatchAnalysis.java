/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
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
import pascal.taie.util.collection.Sets;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
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
        Map<Stmt, List<ExceptionEntry>> catchers = new LinkedHashMap<>();
        ir.getExceptionEntries().forEach(entry -> {
            for (int i = entry.start().getIndex(); i < entry.end().getIndex(); ++i) {
                Stmt stmt = ir.getStmt(i);
                catchers.computeIfAbsent(stmt, unused -> new ArrayList<>())
                        .add(entry);
            }
        });
        return catchers;
    }
}
