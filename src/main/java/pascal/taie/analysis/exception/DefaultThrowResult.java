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

import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.types.ClassType;

import java.util.Collection;
import java.util.Map;

import static java.util.Collections.emptySet;
import static pascal.taie.util.collection.CollectionUtils.newHybridMap;

public class DefaultThrowResult implements ThrowAnalysis.Result {

    private final Map<Stmt, Collection<ClassType>> throwMap = newHybridMap();

    void add(Stmt stmt, Collection<ClassType> exceptions) {
        throwMap.put(stmt, exceptions);
    }

    @Override
    public Collection<ClassType> mayThrow(Stmt stmt) {
        return throwMap.getOrDefault(stmt, emptySet());
    }
}
