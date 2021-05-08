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
import pascal.taie.analysis.pta.core.solver.PointerAnalysis;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;

public class ReflectionPlugin implements Plugin {

    private MemberFactory memberFactory;

    @Override
    public void setPointerAnalysis(PointerAnalysis pta) {
        memberFactory = new MemberFactory(pta);
    }

    @Override
    public void handleNewMethod(JMethod method) {
        method.getIR().getStmts().forEach(stmt -> {
            if (stmt instanceof Invoke) {
                Invoke invoke = (Invoke) stmt;
                memberFactory.handleNewInvoke(invoke);
            }
        });
    }

    @Override
    public void handleNewPointsToSet(CSVar csVar, PointsToSet pts) {
        if (memberFactory.isRelevantVar(csVar.getVar())) {
            memberFactory.handleNewPointsToSet(csVar, pts);
        }
    }
}
