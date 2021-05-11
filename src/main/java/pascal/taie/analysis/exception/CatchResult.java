/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.analysis.exception;

import pascal.taie.ir.stmt.Catch;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.type.ClassType;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static pascal.taie.util.collection.CollectionUtils.addToMapSet;
import static pascal.taie.util.collection.CollectionUtils.newHybridMap;
import static pascal.taie.util.collection.CollectionUtils.newMap;
import static pascal.taie.util.collection.CollectionUtils.newSet;

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

    void addCaughtExplicit(Stmt stmt, Catch catcher, ClassType exceptionType) {
        addToMapSet(caughtExplicit.computeIfAbsent(stmt, s -> newHybridMap()),
                catcher, exceptionType);
    }

    void addUncaughtExplicit(Stmt stmt, ClassType exceptionType) {
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
