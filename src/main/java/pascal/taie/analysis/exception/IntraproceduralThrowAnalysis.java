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
import pascal.taie.ir.exp.InvokeDynamic;
import pascal.taie.ir.exp.NewInstance;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.AssignStmt;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.types.ClassType;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static pascal.taie.util.collection.CollectionUtils.newHybridMap;
import static pascal.taie.util.collection.CollectionUtils.newMap;

/**
 * An intra-procedural throw analysis for computing the exceptions
 * that may be thrown by each Stmt.
 */
public class IntraproceduralThrowAnalysis implements ThrowAnalysis {

    /**
     * If this field is null, then this analysis ignores implicit exceptions.
     */
    @Nullable
    private final ImplicitThrowAnalysis implicitThrowAnalysis;

    /**
     * @param ignoreImplicit whether ignore implicit exceptions in the results.
     */
    public IntraproceduralThrowAnalysis(boolean ignoreImplicit) {
        implicitThrowAnalysis = ignoreImplicit ?
                null : ImplicitThrowAnalysis.get();
    }

    @Override
    public Result analyze(IR ir) {
        Map<Throw, ClassType> definiteThrows = findDefiniteThrows(ir);
        DefaultThrowAnalysisResult result = new DefaultThrowAnalysisResult(
                ir, implicitThrowAnalysis);
        ir.getStmts().forEach(stmt -> {
            if (stmt instanceof Throw) {
                Throw throwStmt = (Throw) stmt;
                result.addExplicit(throwStmt,
                        mayThrowExplicitly(throwStmt, definiteThrows));
            } else if (stmt instanceof Invoke) {
                Invoke invoke = (Invoke) stmt;
                result.addExplicit(invoke, mayThrowExplicitly(invoke));
            }
        });
        return result;
    }

    /**
     * Perform a simple intra-procedural analysis to find out the
     * throw Stmts which only throws exception of definite type.
     * TODO: use a systematic approach to compute definite type
     */
    private static Map<Throw, ClassType> findDefiniteThrows(IR ir) {
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
            if (rvalues != null && rvalues.size() == 1) {
                Exp rvalue = rvalues.get(0);
                if (rvalue instanceof NewInstance) {
                    definiteThrows.put(throwStmt, ((NewInstance) rvalue).getType());
                }
            }
        });
        return definiteThrows;
    }

    private static Collection<ClassType> mayThrowExplicitly(
            Throw throwStmt, Map<Throw, ClassType> definiteThrows) {
        ClassType throwType = definiteThrows.get(throwStmt);
        if (throwType != null) {
            return List.of(throwType);
        } else {
            // add all subtypes of the type of thrown variable
            throwType = (ClassType) throwStmt.getExceptionRef().getType();
            return World.getClassHierarchy()
                    .getAllSubclassesOf(throwType.getJClass(), true)
                    .stream()
                    .filter(Predicate.not(JClass::isAbstract))
                    .map(JClass::getType)
                    .collect(Collectors.toUnmodifiableList());
        }
    }

    private static Collection<ClassType> mayThrowExplicitly(Invoke invoke) {
        return invoke.getInvokeExp() instanceof InvokeDynamic ?
                Collections.emptyList() : // InvokeDynamic.getMethodRef() is unavailable
                invoke.getMethodRef().resolve().getExceptions();
    }
}
