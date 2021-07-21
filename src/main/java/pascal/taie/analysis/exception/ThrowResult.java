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

import pascal.taie.ir.IR;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.language.type.ClassType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static pascal.taie.util.collection.MapUtils.newHybridMap;

public class ThrowResult {

    private final IR ir;

    /**
     * If this field is null, then this result returns empty collection
     * for implicit exceptions.
     */
    private final ImplicitThrowAnalysis implicit;

    private final Map<Stmt, Collection<ClassType>> explicitExceptions = newHybridMap();

    ThrowResult(IR ir, ImplicitThrowAnalysis implicitThrowAnalysis) {
        this.ir = ir;
        this.implicit = implicitThrowAnalysis;
    }

    void addExplicit(Throw throwStmt, Collection<ClassType> exceptions) {
        explicitExceptions.put(throwStmt, exceptions);
    }

    void addExplicit(Invoke invoke, Collection<ClassType> exceptions) {
        explicitExceptions.put(invoke, exceptions);
    }

    public IR getIR() {
        return ir;
    }

    public Collection<ClassType> mayThrowImplicitly(Stmt stmt) {
        return implicit == null ? List.of() :
                implicit.mayThrowImplicitly(stmt);
    }

    public Collection<ClassType> mayThrowExplicitly(Throw throwStmt) {
        return explicitExceptions.getOrDefault(throwStmt, Set.of());
    }

    public Collection<ClassType> mayThrowExplicitly(Invoke invoke) {
        return explicitExceptions.getOrDefault(invoke, Set.of());
    }
}
