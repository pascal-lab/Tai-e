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

import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.CSObjUtils;
import pascal.taie.analysis.pta.plugin.util.ReflectionUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.analysis.pta.pts.PointsToSetFactory;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JClass;

import java.util.List;

class StringBasedModel extends MetaObjModel {

    StringBasedModel(Solver solver) {
        super(solver);
    }

    @Override
    protected void registerVarAndHandler() {
        registerRelevantVarIndexes(get("getConstructor"), BASE);
        registerAPIHandler(get("getConstructor"), this::getConstructor);

        registerRelevantVarIndexes(get("getDeclaredConstructor"), BASE);
        registerAPIHandler(get("getDeclaredConstructor"), this::getDeclaredConstructor);

        registerRelevantVarIndexes(get("getMethod"), BASE, 0);
        registerAPIHandler(get("getMethod"), this::getMethod);

        registerRelevantVarIndexes(get("getDeclaredMethod"), BASE, 0);
        registerAPIHandler(get("getDeclaredMethod"), this::getDeclaredMethod);
    }

    private void getConstructor(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            PointsToSet ctorObjs = PointsToSetFactory.make();
            pts.forEach(obj -> {
                JClass jclass = CSObjUtils.toClass(obj);
                if (jclass != null) {
                    ReflectionUtils.getConstructors(jclass)
                            .map(ctor -> {
                                Obj ctorObj = getReflectionObj(ctor);
                                return csManager.getCSObj(defaultHctx, ctorObj);
                            })
                            .forEach(ctorObjs::addObject);
                }
            });
            if (!ctorObjs.isEmpty()) {
                solver.addVarPointsTo(csVar.getContext(), result, ctorObjs);
            }
        }
    }

    private void getDeclaredConstructor(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            PointsToSet ctorObjs = PointsToSetFactory.make();
            pts.forEach(obj -> {
                JClass jclass = CSObjUtils.toClass(obj);
                if (jclass != null) {
                    ReflectionUtils.getDeclaredConstructors(jclass)
                            .map(ctor -> {
                                Obj ctorObj = getReflectionObj(ctor);
                                return csManager.getCSObj(defaultHctx, ctorObj);
                            })
                            .forEach(ctorObjs::addObject);
                }
            });
            if (!ctorObjs.isEmpty()) {
                solver.addVarPointsTo(csVar.getContext(), result, ctorObjs);
            }
        }
    }

    private void getMethod(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            List<PointsToSet> args = getArgs(csVar, pts, invoke, BASE, 0);
            PointsToSet clsObjs = args.get(0);
            PointsToSet nameObjs = args.get(1);
            PointsToSet mtdObjs = PointsToSetFactory.make();
            clsObjs.forEach(clsObj -> {
                JClass cls = CSObjUtils.toClass(clsObj);
                if (cls != null) {
                    nameObjs.forEach(nameObj -> {
                        String name = CSObjUtils.toString(nameObj);
                        if (name != null) {
                            ReflectionUtils.getMethods(cls, name)
                                    .map(mtd -> {
                                        Obj mtdObj = getReflectionObj(mtd);
                                        return csManager.getCSObj(defaultHctx, mtdObj);
                                    })
                                    .forEach(mtdObjs::addObject);
                        }
                    });
                }
            });
            if (!mtdObjs.isEmpty()) {
                solver.addVarPointsTo(csVar.getContext(), result, mtdObjs);
            }
        }
    }

    private void getDeclaredMethod(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            List<PointsToSet> args = getArgs(csVar, pts, invoke, BASE, 0);
            PointsToSet clsObjs = args.get(0);
            PointsToSet nameObjs = args.get(1);
            PointsToSet mtdObjs = PointsToSetFactory.make();
            clsObjs.forEach(clsObj -> {
                JClass cls = CSObjUtils.toClass(clsObj);
                if (cls != null) {
                    nameObjs.forEach(nameObj -> {
                        String name = CSObjUtils.toString(nameObj);
                        if (name != null) {
                            ReflectionUtils.getDeclaredMethods(cls, name)
                                    .map(mtd -> {
                                        Obj mtdObj = getReflectionObj(mtd);
                                        return csManager.getCSObj(defaultHctx, mtdObj);
                                    })
                                    .forEach(mtdObjs::addObject);
                        }
                    });
                }
            });
            if (!mtdObjs.isEmpty()) {
                solver.addVarPointsTo(csVar.getContext(), result, mtdObjs);
            }
        }
    }

    @Override
    void handleNewCSMethod(CSMethod csMethod) {
    }
}
