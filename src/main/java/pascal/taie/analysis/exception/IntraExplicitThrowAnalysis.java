/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2022 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2022 Yue Li <yueli@nju.edu.cn>
 *
 * This file is part of Tai-e.
 *
 * Tai-e is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3
 * of the License, or (at your option) any later version.
 *
 * Tai-e is distributed in the hope that it will be useful,but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General
 * Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Tai-e. If not, see <https://www.gnu.org/licenses/>.
 */

package pascal.taie.analysis.exception;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.World;
import pascal.taie.ir.IR;
import pascal.taie.ir.exp.Exp;
import pascal.taie.ir.exp.NewInstance;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.MethodResolutionFailedException;
import pascal.taie.ir.stmt.DefinitionStmt;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Throw;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.type.ClassType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static pascal.taie.util.collection.Maps.newHybridMap;
import static pascal.taie.util.collection.Maps.newMap;

class IntraExplicitThrowAnalysis implements ExplicitThrowAnalysis {

    private static final Logger logger = LogManager.getLogger(IntraExplicitThrowAnalysis.class);

    @Override
    public void analyze(IR ir, ThrowResult result) {
        Map<Throw, ClassType> definiteThrows = findDefiniteThrows(ir);
        ir.forEach(stmt -> {
            if (stmt instanceof Throw throwStmt) {
                result.addExplicit(throwStmt,
                        mayThrowExplicitly(throwStmt, definiteThrows));
            } else if (stmt instanceof Invoke invoke) {
                result.addExplicit(invoke, mayThrowExplicitly(invoke));
            }
        });
    }

    /**
     * Performs a simple intra-procedural analysis to find out the
     * throw Stmts which only throws exception of definite type.
     */
    private static Map<Throw, ClassType> findDefiniteThrows(IR ir) {
        Map<Var, Throw> throwVars = newMap();
        Map<Exp, List<Exp>> assigns = newMap();
        ir.forEach(s -> {
            // collect all throw Stmts and corresponding thrown Vars
            if (s instanceof Throw throwStmt) {
                throwVars.put(throwStmt.getExceptionRef(), throwStmt);
            }
            // collect all definition stmts
            Exp lhs = null, rhs = null;
            if (s instanceof DefinitionStmt<?, ?> define) {
                lhs = define.getLValue();
                rhs = define.getRValue();
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
            return World.get()
                    .getClassHierarchy()
                    .getAllSubclassesOf(throwType.getJClass())
                    .stream()
                    .filter(Predicate.not(JClass::isAbstract))
                    .map(JClass::getType)
                    .toList();
        }
    }

    private static Collection<ClassType> mayThrowExplicitly(Invoke invoke) {
        try {
            return invoke.isDynamic() ?
                    List.of() : // InvokeDynamic.getMethodRef() is unavailable
                    invoke.getMethodRef().resolve().getExceptions();
        } catch (MethodResolutionFailedException e) {
            logger.warn(e.getMessage());
            return List.of();
        }
    }
}
