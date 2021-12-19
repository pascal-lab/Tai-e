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
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;

import java.util.Map;
import java.util.Set;

public class CatchResult {

    private final Map<Stmt, MultiMap<Stmt, ClassType>> caughtImplicit = Maps.newHybridMap();

    private final MultiMap<Stmt, ClassType> uncaughtImplicit
            = Maps.newMultiMap(Maps.newHybridMap());

    private final Map<Stmt, MultiMap<Stmt, ClassType>> caughtExplicit = Maps.newHybridMap();

    private final MultiMap<Stmt, ClassType> uncaughtExplicit
            = Maps.newMultiMap(Maps.newHybridMap());

    void addCaughtImplicit(Stmt stmt, Catch catcher, ClassType exceptionType) {
        caughtImplicit.computeIfAbsent(stmt, s -> Maps.newMultiMap(Maps.newHybridMap()))
                .put(catcher, exceptionType);
    }

    void addUncaughtImplicit(Stmt stmt, ClassType exceptionType) {
        uncaughtImplicit.put(stmt, exceptionType);
    }

    /**
     * @return all exception types that may be implicitly thrown by given Stmt
     * and caught by its containing method. The result of the call is a map
     * from Catch statements to set of exception types that are caught
     * by the Catches.
     */
    public MultiMap<Stmt, ClassType> getCaughtImplicitOf(Stmt stmt) {
        return caughtImplicit.getOrDefault(stmt, Maps.newMultiMap(Map.of()));
    }

    /**
     * @return the set of exception types that may be implicitly thrown
     * by given Stmt but not caught by its containing method.
     */
    public Set<ClassType> getUncaughtImplicitOf(Stmt stmt) {
        return uncaughtImplicit.get(stmt);
    }

    void addCaughtExplicit(Stmt stmt, Catch catcher, ClassType exceptionType) {
        caughtExplicit.computeIfAbsent(stmt, s -> Maps.newMultiMap())
                .put(catcher, exceptionType);
    }

    void addUncaughtExplicit(Stmt stmt, ClassType exceptionType) {
        uncaughtExplicit.put(stmt, exceptionType);
    }

    /**
     * @return all exception types that may be explicitly thrown by given Stmt
     * and caught by its containing method. The result of the call is a map
     * from Catch statements to set of exception types that are caught
     * by the Catches.
     */
    public MultiMap<Stmt, ClassType> getCaughtExplicitOf(Stmt stmt) {
        return caughtExplicit.getOrDefault(stmt, Maps.newMultiMap());
    }

    /**
     * @return the set of exception types that may be explicitly thrown
     * by given Stmt but not caught by its containing method.
     */
    public Set<ClassType> getUncaughtExplicitOf(Stmt stmt) {
        return uncaughtExplicit.get(stmt);
    }

    /**
     * @return all exception types that may be implicitly or explicitly
     * thrown by given Stmt and caught by its containing method.
     * The result of the call is a map from Catch statements to set of
     * exception types that are caught by the Catches.
     */
    public MultiMap<Stmt, ClassType> getCaughtOf(Stmt stmt) {
        MultiMap<Stmt, ClassType> caught = Maps.newMultiMap();
        caught.putAll(getCaughtImplicitOf(stmt));
        caught.putAll(getCaughtExplicitOf(stmt));
        return caught;
    }

    /**
     * @return all exception types that may be implicitly or explicitly
     * thrown by given Stmt and cannot be caught by its containing method.
     */
    public Set<ClassType> getUncaughtOf(Stmt stmt) {
        Set<ClassType> uncaught = Sets.newSet();
        uncaught.addAll(getUncaughtImplicitOf(stmt));
        uncaught.addAll(getUncaughtExplicitOf(stmt));
        return uncaught;
    }
}
