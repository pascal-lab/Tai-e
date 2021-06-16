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

package pascal.taie.analysis.pta.plugin.reflection;

import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.plugin.util.Model;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;

import java.util.function.Predicate;

public class ReflectionAnalysis implements Plugin {

    private Model classModel;

    private Model sideEffectModel;

    @Override
    public void setSolver(Solver solver) {
        classModel = new ClassModel(solver);
        sideEffectModel = new SideEffectModel(solver);
    }

    @Override
    public void onNewMethod(JMethod method) {
        method.getIR().getStmts()
                .stream()
                .filter(s -> s instanceof Invoke)
                .map(s -> (Invoke) s)
                .filter(Predicate.not(Invoke::isDynamic))
                .forEach(invoke -> {
                    classModel.handleNewInvoke(invoke);
                    sideEffectModel.handleNewInvoke(invoke);
                });
    }

    @Override
    public void onNewPointsToSet(CSVar csVar, PointsToSet pts) {
        if (classModel.isRelevantVar(csVar.getVar())) {
            classModel.handleNewPointsToSet(csVar, pts);
        }
    }
}
