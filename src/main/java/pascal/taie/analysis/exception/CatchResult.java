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

import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.type.ClassType;

import java.util.Map;
import java.util.Set;

import static pascal.taie.util.collection.Maps.addToMapSet;
import static pascal.taie.util.collection.Maps.newHybridMap;
import static pascal.taie.util.collection.Maps.newMap;
import static pascal.taie.util.collection.Sets.newSet;

public class CatchResult {

    private final Map<Stmt, Map<Stmt, Set<ClassType>>> caughtImplicit = newHybridMap();

    private final Map<Stmt, Set<ClassType>> uncaughtImplicit = newHybridMap();

    private final Map<Stmt, Map<Stmt, Set<ClassType>>> caughtExplicit = newHybridMap();

    private final Map<Stmt, Set<ClassType>> uncaughtExplicit = newHybridMap();

    void addCaughtImplicit(Stmt stmt, Catch catcher, ClassType exceptionType) {
        addToMapSet(caughtImplicit.computeIfAbsent(stmt, s -> newHybridMap()),
                catcher, exceptionType);
    }

    void addUncaughtImplicit(Stmt stmt, ClassType exceptionType) {
        addToMapSet(uncaughtImplicit, stmt, exceptionType);
    }

    /**
     * @return all exception types that may be implicitly thrown by given Stmt
     * and caught by its containing method. The result of the call is a map
     * from Catch statements to set of exception types that are caught
     * by the Catches.
     */
    public Map<Stmt, Set<ClassType>> getCaughtImplicitOf(Stmt stmt) {
        return caughtImplicit.getOrDefault(stmt, Map.of());
    }

    /**
     * @return the set of exception types that may be implicitly thrown
     * by given Stmt but not caught by its containing method.
     */
    public Set<ClassType> getUncaughtImplicitOf(Stmt stmt) {
        return uncaughtImplicit.getOrDefault(stmt, Set.of());
    }

    void addCaughtExplicit(Stmt stmt, Catch catcher, ClassType exceptionType) {
        addToMapSet(caughtExplicit.computeIfAbsent(stmt, s -> newHybridMap()),
                catcher, exceptionType);
    }

    void addUncaughtExplicit(Stmt stmt, ClassType exceptionType) {
        addToMapSet(uncaughtExplicit, stmt, exceptionType);
    }

    /**
     * @return all exception types that may be explicitly thrown by given Stmt
     * and caught by its containing method. The result of the call is a map
     * from Catch statements to set of exception types that are caught
     * by the Catches.
     */
    public Map<Stmt, Set<ClassType>> getCaughtExplicitOf(Stmt stmt) {
        return caughtExplicit.getOrDefault(stmt, Map.of());
    }

    /**
     * @return the set of exception types that may be explicitly thrown
     * by given Stmt but not caught by its containing method.
     */
    public Set<ClassType> getUncaughtExplicitOf(Stmt stmt) {
        return uncaughtExplicit.getOrDefault(stmt, Set.of());
    }

    /**
     * @return all exception types that may be implicitly or explicitly
     * thrown by given Stmt and caught by its containing method.
     * The result of the call is a map from Catch statements to set of
     * exception types that are caught by the Catches.
     */
    public Map<Stmt, Set<ClassType>> getCaughtOf(Stmt stmt) {
        Map<Stmt, Set<ClassType>> caught = newMap();
        caught.putAll(getCaughtImplicitOf(stmt));
        caught.putAll(getCaughtExplicitOf(stmt));
        return caught;
    }

    /**
     * @return all exception types that may be implicitly or explicitly
     * thrown by given Stmt and cannot be caught by its containing method.
     */
    public Set<ClassType> getUncaughtOf(Stmt stmt) {
        Set<ClassType> uncaught = newSet();
        uncaught.addAll(getUncaughtImplicitOf(stmt));
        uncaught.addAll(getUncaughtExplicitOf(stmt));
        return uncaught;
    }
}
