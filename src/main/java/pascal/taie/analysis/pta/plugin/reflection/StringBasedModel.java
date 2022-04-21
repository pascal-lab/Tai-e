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

package pascal.taie.analysis.pta.plugin.reflection;

import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.CSObjs;
import pascal.taie.analysis.pta.plugin.util.Reflections;
import pascal.taie.analysis.pta.pts.PointsToSet;
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
            PointsToSet ctorObjs = solver.makePointsToSet();
            pts.forEach(obj -> {
                JClass jclass = CSObjs.toClass(obj);
                if (jclass != null) {
                    Reflections.getConstructors(jclass)
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
            PointsToSet ctorObjs = solver.makePointsToSet();
            pts.forEach(obj -> {
                JClass jclass = CSObjs.toClass(obj);
                if (jclass != null) {
                    Reflections.getDeclaredConstructors(jclass)
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
            PointsToSet mtdObjs = solver.makePointsToSet();
            clsObjs.forEach(clsObj -> {
                JClass cls = CSObjs.toClass(clsObj);
                if (cls != null) {
                    nameObjs.forEach(nameObj -> {
                        String name = CSObjs.toString(nameObj);
                        if (name != null) {
                            Reflections.getMethods(cls, name)
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
            PointsToSet mtdObjs = solver.makePointsToSet();
            clsObjs.forEach(clsObj -> {
                JClass cls = CSObjs.toClass(clsObj);
                if (cls != null) {
                    nameObjs.forEach(nameObj -> {
                        String name = CSObjs.toString(nameObj);
                        if (name != null) {
                            Reflections.getDeclaredMethods(cls, name)
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
