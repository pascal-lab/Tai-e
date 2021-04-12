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
import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.types.ClassType;
import pascal.taie.language.types.TypeManager;
import pascal.taie.util.collection.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.Collections.emptyList;
import static pascal.taie.util.collection.CollectionUtils.addToMapSet;
import static pascal.taie.util.collection.CollectionUtils.newHybridMap;
import static pascal.taie.util.collection.CollectionUtils.newHybridSet;

/**
 * Intra-procedural catch analysis for computing the exceptions thrown by
 * each Stmt will be caught by which Stmts, or not caught at all.
 */
public class CatchAnalysis {

    public static Result analyze(IR ir, ThrowAnalysis throwAnalysis) {
        Map<Stmt, List<ExceptionEntry>> catchers = getPotentialCatchers(ir);
        TypeManager typeManager = World.getTypeManager();
        Result result = new Result();
        ir.getStmts().forEach(stmt -> {
            Collection<ClassType> exceptionTypes = throwAnalysis.mayThrow(stmt);
            for (ExceptionEntry entry : catchers.getOrDefault(stmt, emptyList())) {
                Set<ClassType> uncaught = newHybridSet();
                exceptionTypes.forEach(t -> {
                    if (typeManager.isSubtype(entry.getCatchType(), t)) {
                        result.addCaughtException(stmt, entry.getHandler(), t);
                    } else {
                        uncaught.add(t);
                    }
                });
                exceptionTypes = uncaught;
            }
            exceptionTypes.forEach(e -> result.addUncaughtException(stmt, e));
        });
        return result;
    }

    public static Map<Stmt, List<ExceptionEntry>> getPotentialCatchers(IR ir) {
        Map<Stmt, List<ExceptionEntry>> catchers = new LinkedHashMap<>();
        ir.getExceptionEntries().forEach(entry -> {
            for (int i = entry.getStart().getIndex(); i < entry.getEnd().getIndex(); ++i) {
                Stmt stmt = ir.getStmt(i);
                catchers.computeIfAbsent(stmt, s -> new ArrayList<>())
                        .add(entry);
            }
        });
        return catchers;
    }

    public static class Result {

        private final Map<Stmt, Map<Stmt, Set<ClassType>>> caughtExceptions = newHybridMap();

        private final Map<Stmt, Set<ClassType>> uncaughtExceptions = newHybridMap();

        private void addCaughtException(Stmt stmt, Catch catcher, ClassType exceptionType) {
            addToMapSet(caughtExceptions.computeIfAbsent(stmt, s -> newHybridMap()),
                    catcher, exceptionType);
        }

        private void addUncaughtException(Stmt stmt, ClassType exceptionType) {
            addToMapSet(uncaughtExceptions, stmt, exceptionType);
        }

        public Stream<Pair<Stmt, Set<ClassType>>> caughtExceptionsOf(Stmt stmt) {
            return caughtExceptions.getOrDefault(stmt, Collections.emptyMap())
                    .entrySet()
                    .stream()
                    .map(e -> new Pair<>(e.getKey(), e.getValue()));
        }

        public Stream<ClassType> uncaughtExceptionsOf(Stmt stmt) {
            return uncaughtExceptions.getOrDefault(
                    stmt, Collections.emptySet()).stream();
        }
    }
}
