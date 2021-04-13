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
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.NewInstance;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignStmt;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.types.ClassType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static pascal.taie.util.collection.CollectionUtils.newHybridMap;
import static pascal.taie.util.collection.CollectionUtils.newHybridSet;
import static pascal.taie.util.collection.CollectionUtils.newMap;

/**
 * An intra-procedural throw analysis for computing the exceptions
 * that may be thrown by each Stmt.
 */
public class DefaultThrowAnalysis extends AbstractThrowAnalysis {
    
    public DefaultThrowAnalysis(boolean includeImplicit) {
        super(includeImplicit);
    }

    /**
     * Perform a simple intra-procedural analysis to find out the
     * throw Stmts which only throws exception of definite type.
     * TODO: use a systematic approach to compute definite type
     */
    @Override
    protected Map<Throw, ClassType> preAnalysis(IR ir) {
        Map<Var, Throw> throwVars = newMap();
        Map<Exp, List<Exp>> assigns = newMap();
        ir.getStmts().forEach(s -> {
            // collect all throw Stmts and corresponding thrown Vars
            if (s instanceof Throw) {
                Throw throwStmt = (Throw) s;
                throwVars.put(throwStmt.getExceptionRef(), throwStmt);
            }
            // collect all assignments
            Exp lhs = null, rhs = null;
            if (s instanceof AssignStmt) {
                AssignStmt<?, ?> assign = (AssignStmt<?, ?>) s;
                lhs = assign.getLValue();
                rhs = assign.getRValue();
            } else if (s instanceof Invoke) {
                Invoke invoke = (Invoke) s;
                lhs = invoke.getResult();
                rhs = invoke.getInvokeExp();
            }
            if (lhs != null && rhs != null) {
                assigns.computeIfAbsent(lhs, e -> new ArrayList<>()).add(rhs);
            }
        });
        // For throw v, if v is assigned only once and is assigned by
        // a new expression, then the type of thrown exception is definite.
        Map<Throw, ClassType> definiteThrows = newHybridMap();
        throwVars.values().forEach(throwStmt -> {
            List<Exp> rvalues = assigns.get(throwStmt.getExceptionRef());
            if (rvalues.size() == 1) {
                Exp rvalue = rvalues.get(0);
                if (rvalue instanceof NewInstance) {
                    definiteThrows.put(throwStmt, ((NewInstance) rvalue).getType());
                }
            }
        });
        return definiteThrows;
    }

    @Override
    protected Collection<ClassType> mayThrow(Stmt stmt, Object info) {
        if (stmt instanceof Throw) {
            @SuppressWarnings("unchecked")
            Map<Stmt, ClassType> definiteThrows = (Map<Stmt, ClassType>) info;
            return mayThrow((Throw) stmt, definiteThrows);
        } else if (stmt instanceof Invoke) {
            return mayThrow((Invoke) stmt);
        } else if (includeImplicit) {
            return getImplicitExceptions(stmt);
        } else {
            return Collections.emptyList();
        }
    }

    private Collection<ClassType> mayThrow(
            Throw throwStmt, Map<Stmt, ClassType> definiteThrows) {
        Set<ClassType> result = newHybridSet();
        if (includeImplicit) {
            result.addAll(NULL_POINTER_EXCEPTION);
        }
        ClassType throwType = definiteThrows.get(throwStmt);
        if (throwType != null) {
            result.add(throwType);
        } else {
            // add all subtypes of the type of thrown variable
            throwType = (ClassType) throwStmt.getExceptionRef().getType();
            World.getClassHierarchy()
                    .getAllSubclassesOf(throwType.getJClass(), true)
                    .stream()
                    .map(JClass::getType)
                    .forEach(result::add);
        }
        return Collections.unmodifiableSet(result);
    }

    private Collection<ClassType> mayThrow(Invoke invoke) {
        Set<ClassType> result = newHybridSet();
        if (includeImplicit) {
            if (invoke.isStatic()) {
                result.addAll(INITIALIZER_ERROR);
            } else {
                result.addAll(NULL_POINTER_EXCEPTION);
            }
        }
        result.addAll(invoke.getMethodRef().resolve().getExceptions());
        return Collections.unmodifiableSet(result);
    }
}
