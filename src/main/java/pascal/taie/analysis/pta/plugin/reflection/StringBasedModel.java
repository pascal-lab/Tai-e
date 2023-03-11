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

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.CSObjs;
import pascal.taie.analysis.pta.plugin.util.Reflections;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.ir.stmt.Stmt;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static pascal.taie.analysis.pta.plugin.util.InvokeUtils.BASE;

class StringBasedModel extends InferenceModel {

    StringBasedModel(Solver solver, MetaObjHelper helper, Set<Invoke> invokesWithLog) {
        super(solver, helper, invokesWithLog);
    }

    @Override
    protected void registerVarAndHandler() {
        JClass clazz = Objects.requireNonNull(hierarchy.getJREClass(ClassNames.CLASS));

        JMethod classForName = clazz.getDeclaredMethod(
                Subsignature.get("java.lang.Class forName(java.lang.String)"));
        registerRelevantVarIndexes(classForName, 0);
        registerAPIHandler(classForName, this::classForName);

        JMethod classForName2 = clazz.getDeclaredMethod(
                Subsignature.get("java.lang.Class forName(java.lang.String,boolean,java.lang.ClassLoader)"));
        // TODO: take class loader into account
        registerRelevantVarIndexes(classForName2, 0);
        registerAPIHandler(classForName2, this::classForName);

        JMethod getConstructor = clazz.getDeclaredMethod("getConstructor");
        registerRelevantVarIndexes(getConstructor, BASE);
        registerAPIHandler(getConstructor, this::getConstructor);

        JMethod getDeclaredConstructor = clazz.getDeclaredMethod("getDeclaredConstructor");
        registerRelevantVarIndexes(getDeclaredConstructor, BASE);
        registerAPIHandler(getDeclaredConstructor, this::getDeclaredConstructor);

        JMethod getMethod = clazz.getDeclaredMethod("getMethod");
        registerRelevantVarIndexes(getMethod, BASE, 0);
        registerAPIHandler(getMethod, this::getMethod);

        JMethod getDeclaredMethod = clazz.getDeclaredMethod("getDeclaredMethod");
        registerRelevantVarIndexes(getDeclaredMethod, BASE, 0);
        registerAPIHandler(getDeclaredMethod, this::getDeclaredMethod);
    }

    @Override
    protected void handleNewNonInvokeStmt(Stmt stmt) {
        // nothing to do
    }

    @Override
    protected void classForName(CSVar csVar, PointsToSet pts, Invoke invoke) {
        if (invokesWithLog.contains(invoke)) {
            return;
        }
        Context context = csVar.getContext();
        pts.forEach(obj -> {
            String className = CSObjs.toString(obj);
            if (className != null) {
                JClass clazz = hierarchy.getClass(className);
                if (clazz != null) {
                    solver.initializeClass(clazz);
                    Var result = invoke.getResult();
                    if (result != null) {
                        Obj clsObj = helper.getMetaObj(clazz);
                        solver.addVarPointsTo(context, result, clsObj);
                    }
                }
            }
        });
    }

    @Override
    protected void getConstructor(CSVar csVar, PointsToSet pts, Invoke invoke) {
        if (invokesWithLog.contains(invoke)) {
            return;
        }
        Var result = invoke.getResult();
        if (result != null) {
            Context context = csVar.getContext();
            pts.forEach(obj -> {
                JClass jclass = CSObjs.toClass(obj);
                if (jclass != null) {
                    Reflections.getConstructors(jclass)
                            .map(helper::getMetaObj)
                            .forEach(ctorObj ->
                                    solver.addVarPointsTo(context, result, ctorObj));
                }
            });
        }
    }

    @Override
    protected void getDeclaredConstructor(CSVar csVar, PointsToSet pts, Invoke invoke) {
        if (invokesWithLog.contains(invoke)) {
            return;
        }
        Var result = invoke.getResult();
        if (result != null) {
            Context context = csVar.getContext();
            pts.forEach(obj -> {
                JClass jclass = CSObjs.toClass(obj);
                if (jclass != null) {
                    Reflections.getDeclaredConstructors(jclass)
                            .map(helper::getMetaObj)
                            .forEach(ctorObj ->
                                    solver.addVarPointsTo(context, result, ctorObj));
                }
            });
        }
    }

    @Override
    protected void getMethod(CSVar csVar, PointsToSet pts, Invoke invoke) {
        if (invokesWithLog.contains(invoke)) {
            return;
        }
        Var result = invoke.getResult();
        if (result != null) {
            List<PointsToSet> args = getArgs(csVar, pts, invoke, BASE, 0);
            PointsToSet clsObjs = args.get(0);
            PointsToSet nameObjs = args.get(1);
            Context context = csVar.getContext();
            clsObjs.forEach(clsObj -> {
                JClass cls = CSObjs.toClass(clsObj);
                if (cls != null) {
                    nameObjs.forEach(nameObj -> {
                        String name = CSObjs.toString(nameObj);
                        if (name != null) {
                            Reflections.getMethods(cls, name)
                                    .map(helper::getMetaObj)
                                    .forEach(mtdObj ->
                                            solver.addVarPointsTo(context, result, mtdObj));
                        }
                    });
                }
            });
        }
    }

    @Override
    protected void getDeclaredMethod(CSVar csVar, PointsToSet pts, Invoke invoke) {
        if (invokesWithLog.contains(invoke)) {
            return;
        }
        Var result = invoke.getResult();
        if (result != null) {
            List<PointsToSet> args = getArgs(csVar, pts, invoke, BASE, 0);
            PointsToSet clsObjs = args.get(0);
            PointsToSet nameObjs = args.get(1);
            Context context = csVar.getContext();
            clsObjs.forEach(clsObj -> {
                JClass cls = CSObjs.toClass(clsObj);
                if (cls != null) {
                    nameObjs.forEach(nameObj -> {
                        String name = CSObjs.toString(nameObj);
                        if (name != null) {
                            Reflections.getDeclaredMethods(cls, name)
                                    .map(helper::getMetaObj)
                                    .forEach(mtdObj ->
                                            solver.addVarPointsTo(context, result, mtdObj));
                        }
                    });
                }
            });
        }
    }
}
