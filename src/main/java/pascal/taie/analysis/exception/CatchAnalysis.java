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
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.language.types.ClassType;
import pascal.taie.language.types.TypeManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.Collections.emptyList;
import static pascal.taie.util.collection.CollectionUtils.addToMapSet;
import static pascal.taie.util.collection.CollectionUtils.newHybridMap;
import static pascal.taie.util.collection.CollectionUtils.newHybridSet;
import static pascal.taie.util.collection.CollectionUtils.newMap;
import static pascal.taie.util.collection.CollectionUtils.newSet;

/**
 * Intra-procedural catch analysis for computing the exceptions thrown by
 * each Stmt will be caught by which Stmts, or not caught at all.
 */
public class CatchAnalysis {

    /**
     * Analyze the exceptions thrown by each Stmt in given IR may be caught
     * by which (catch) Stmts, and which exceptions are not caught in the IR.
     */
    public static Result analyze(IR ir, ThrowAnalysis.Result throwResult) {
        Map<Stmt, List<ExceptionEntry>> catchers = getPotentialCatchers(ir);
        TypeManager typeManager = World.getTypeManager();
        Result result = new Result();
        ir.getStmts().forEach(stmt -> {
            Collection<ClassType> implicit = throwResult.mayThrowImplicitly(stmt);
            Collection<ClassType> explicit;
            if (stmt instanceof Throw) {
                explicit = throwResult.mayThrowExplicitly((Throw) stmt);
            } else if (stmt instanceof Invoke) {
                explicit = throwResult.mayThrowExplicitly((Invoke) stmt);
            } else {
                explicit = emptyList();
            }
            for (ExceptionEntry entry : catchers.getOrDefault(stmt, emptyList())) {
                Set<ClassType> uncaughtImplicit = newHybridSet();
                implicit.forEach(t -> {
                    if (typeManager.isSubtype(entry.getCatchType(), t)) {
                        result.addCaughtImplicit(stmt, entry.getHandler(), t);
                    } else {
                        uncaughtImplicit.add(t);
                    }
                });
                implicit = uncaughtImplicit;

                Set<ClassType> uncaughtExplicit = newHybridSet();
                explicit.forEach(t -> {
                    if (typeManager.isSubtype(entry.getCatchType(), t)) {
                        result.addCaughtExplicit(stmt, entry.getHandler(), t);
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
            for (int i = entry.getStart().getIndex(); i < entry.getEnd().getIndex(); ++i) {
                Stmt stmt = ir.getStmt(i);
                catchers.computeIfAbsent(stmt, s -> new ArrayList<>())
                        .add(entry);
            }
        });
        return catchers;
    }

    public static class Result {

        private final Map<Stmt, Map<Stmt, Set<ClassType>>> caughtImplicit = newHybridMap();

        private final Map<Stmt, Set<ClassType>> uncaughtImplicit = newHybridMap();

        private final Map<Stmt, Map<Stmt, Set<ClassType>>> caughtExplicit = newHybridMap();

        private final Map<Stmt, Set<ClassType>> uncaughtExplicit = newHybridMap();

        private void addCaughtImplicit(Stmt stmt, Catch catcher, ClassType exceptionType) {
            addToMapSet(caughtImplicit.computeIfAbsent(stmt, s -> newHybridMap()),
                    catcher, exceptionType);
        }

        private void addUncaughtImplicit(Stmt stmt, ClassType exceptionType) {
            addToMapSet(uncaughtImplicit, stmt, exceptionType);
        }

        /**
         * For given Stmt s, return a stream of (Stmt s', Set<ClassType> ts),
         * where the s' may catch the exceptions (of the types in ts)
         * thrown by s implicitly.
         */
        public Map<Stmt, Set<ClassType>> getCaughtImplicitOf(Stmt stmt) {
            return caughtImplicit.getOrDefault(stmt, Collections.emptyMap());
        }

        /**
         * @return the set of exception types that may be implicitly thrown
         * by given Stmt but not caught by its containing method.
         */
        public Set<ClassType> getUncaughtImplicitOf(Stmt stmt) {
            return uncaughtImplicit.getOrDefault(stmt, Collections.emptySet());
        }

        private void addCaughtExplicit(Stmt stmt, Catch catcher, ClassType exceptionType) {
            addToMapSet(caughtExplicit.computeIfAbsent(stmt, s -> newHybridMap()),
                    catcher, exceptionType);
        }

        private void addUncaughtExplicit(Stmt stmt, ClassType exceptionType) {
            addToMapSet(uncaughtExplicit, stmt, exceptionType);
        }

        /**
         * For given Stmt s, return a stream of (Stmt s', Set<ClassType> ts),
         * where the s' may catch the exceptions (of the types in ts)
         * thrown by s explicitly.
         */
        public Map<Stmt, Set<ClassType>> getCaughtExplicitOf(Stmt stmt) {
            return caughtExplicit.getOrDefault(stmt, Collections.emptyMap());
        }

        /**
         * @return the set of exception types that may be explicitly thrown
         * by given Stmt but not caught by its containing method.
         */
        public Set<ClassType> getUncaughtExplicitOf(Stmt stmt) {
            return uncaughtExplicit.getOrDefault(stmt, Collections.emptySet());
        }

        /**
         * @return all caught exceptions of given Stmt, including both
         * implicit and explicit exceptions.
         */
        public Map<Stmt, Set<ClassType>> getCaughtOf(Stmt stmt) {
            Map<Stmt, Set<ClassType>> caught = newMap();
            caught.putAll(getCaughtImplicitOf(stmt));
            caught.putAll(getCaughtExplicitOf(stmt));
            return caught;
        }

        /**
         * @return all uncaught exceptions of given Stmt, including both
         * implicit and explicit exceptions.
         */
        public Set<ClassType> getUncaughtOf(Stmt stmt) {
            Set<ClassType> uncaught = newSet();
            uncaught.addAll(getUncaughtImplicitOf(stmt));
            uncaught.addAll(getUncaughtExplicitOf(stmt));
            return uncaught;
        }
    }
}
