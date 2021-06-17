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

package pascal.taie.analysis.pta.plugin.invokedynamic;

import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.AbstractModel;
import pascal.taie.analysis.pta.plugin.util.CSObjUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.analysis.pta.pts.PointsToSetFactory;
import pascal.taie.ir.exp.MethodType;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;

import java.util.List;

/**
 * Models invocations to MethodType.methodType(*);
 */
class MethodTypeModel extends AbstractModel {

    MethodTypeModel(Solver solver) {
        super(solver);
    }

    @Override
    protected void registerVarAndHandler() {
        JMethod mt1Class = hierarchy.getJREMethod("<java.lang.invoke.MethodType: java.lang.invoke.MethodType methodType(java.lang.Class)>");
        registerRelevantVarIndexes(mt1Class, 0);
        registerAPIHandler(mt1Class, this::handle1Class);

        JMethod mt2Classes = hierarchy.getJREMethod("<java.lang.invoke.MethodType: java.lang.invoke.MethodType methodType(java.lang.Class,java.lang.Class)>");
        registerRelevantVarIndexes(mt2Classes, 0, 1);
        registerAPIHandler(mt2Classes, this::handle2Classes);

        JMethod mtClassMt = hierarchy.getJREMethod("<java.lang.invoke.MethodType: java.lang.invoke.MethodType methodType(java.lang.Class,java.lang.invoke.MethodType)>");
        registerRelevantVarIndexes(mtClassMt, 0, 1);
        registerAPIHandler(mtClassMt, this::handleClassMt);
    }

    /**
     * Handles MethodType.methodType(java.lang.Class)
     */
    private void handle1Class(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            PointsToSet mtObjs = PointsToSetFactory.make();
            pts.forEach(obj -> {
                Type retType = CSObjUtils.toType(obj);
                if (retType != null) {
                    MethodType mt = MethodType.get(List.of(), retType);
                    Obj mtObj = heapModel.getConstantObj(mt);
                    mtObjs.addObject(csManager.getCSObj(defaultHctx, mtObj));
                }
            });
            if (!mtObjs.isEmpty()) {
                solver.addVarPointsTo(csVar.getContext(), result, mtObjs);
            }
        }
    }

    /**
     * Handles MethodType.methodType(java.lang.Class,java.lang.Class)
     */
    private void handle2Classes(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            List<PointsToSet> args = getArgs(csVar, pts, invoke, 0, 1);
            PointsToSet retObjs = args.get(0);
            PointsToSet paramObjs = args.get(1);
            PointsToSet mtObjs = PointsToSetFactory.make();
            retObjs.forEach(retObj -> {
                Type retType = CSObjUtils.toType(retObj);
                if (retType != null) {
                    paramObjs.forEach(paramObj -> {
                        Type paramType = CSObjUtils.toType(paramObj);
                        if (paramType != null) {
                            MethodType mt = MethodType.get(List.of(paramType), retType);
                            Obj mtObj = heapModel.getConstantObj(mt);
                            mtObjs.addObject(csManager.getCSObj(defaultHctx, mtObj));
                        }
                    });
                }
            });
            if (!mtObjs.isEmpty()) {
                solver.addVarPointsTo(csVar.getContext(), result, mtObjs);
            }
        }
    }

    /**
     * Handles MethodType.methodType(java.lang.Class,java.lang.invoke.MethodType)
     */
    private void handleClassMt(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            List<PointsToSet> args = getArgs(csVar, pts, invoke, 0, 1);
            PointsToSet retObjs = args.get(0);
            PointsToSet mtObjs = args.get(1);
            PointsToSet resultMTObjs = PointsToSetFactory.make();
            retObjs.forEach(retObj -> {
                Type retType = CSObjUtils.toType(retObj);
                if (retType != null) {
                    mtObjs.forEach(mtObj -> {
                        MethodType mt = CSObjUtils.toMethodType(mtObj);
                        if (mt != null) {
                            MethodType resultMT = MethodType.get(mt.getParamTypes(), retType);
                            Obj resultMTObj = heapModel.getConstantObj(resultMT);
                            resultMTObjs.addObject(csManager.getCSObj(defaultHctx, resultMTObj));
                        }
                    });
                }
            });
            if (!resultMTObjs.isEmpty()) {
                solver.addVarPointsTo(csVar.getContext(), result, resultMTObjs);
            }
        }
    }
}
