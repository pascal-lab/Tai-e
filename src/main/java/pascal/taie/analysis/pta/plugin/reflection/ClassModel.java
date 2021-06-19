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
import pascal.taie.analysis.pta.plugin.util.AbstractModel;
import pascal.taie.analysis.pta.plugin.util.CSObjUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.VoidType;

/**
 * Models APIs of java.lang.Class.
 */
class ClassModel extends AbstractModel {

    ClassModel(Solver solver) {
        super(solver);
    }

    @Override
    protected void registerVarAndHandler() {
        JMethod getPrimitiveClass = hierarchy.getJREMethod("<java.lang.Class: java.lang.Class getPrimitiveClass(java.lang.String)>");
        registerRelevantVarIndexes(getPrimitiveClass, 0);
        registerAPIHandler(getPrimitiveClass, this::getPrimitiveClass);
    }

    private void getPrimitiveClass(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            pts.forEach(nameObj -> {
                String name = CSObjUtils.toString(nameObj);
                if (name != null) {
                    Type type = name.equals("void") ?
                            VoidType.VOID : PrimitiveType.get(name);
                    solver.addVarPointsTo(csVar.getContext(), result, defaultHctx,
                            heapModel.getConstantObj(ClassLiteral.get(type)));
                }
            });
        }
    }
}
