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

package pascal.taie.analysis.pta.plugin;

import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.ir.stmt.AssignLiteral;
import pascal.taie.ir.stmt.FieldStmt;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;

/**
 * Triggers the analysis of class initializers.
 * Well, the description of "when initialization occurs" of JLS (11 Ed., 12.4.1)
 * and JVM Spec. (11 Ed., 5.5) looks not very consistent.
 * TODO: handles class initialization triggered by MethodHandle,
 *  and superinterfaces (that declare default methods).
 */
public class ClassInitializer implements Plugin {

    private Solver solver;

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
    }

    @Override
    public void onNewMethod(JMethod method) {
        if (method.isStatic() || method.isConstructor()) {
            solver.initializeClass(method.getDeclaringClass());
        }
        method.getIR().forEach(s -> {
            if (s instanceof AssignLiteral) {
                Type type = ((AssignLiteral) s).getRValue().getType();
                if (type instanceof ClassType) {
                    solver.initializeClass(((ClassType) type).getJClass());
                }
            } else if (s instanceof FieldStmt<?, ?> fieldStmt) {
                if (fieldStmt.isStatic()) {
                    JField field = fieldStmt.getFieldRef().resolve();
                    solver.initializeClass(field.getDeclaringClass());
                }
            }
        });
    }
}
