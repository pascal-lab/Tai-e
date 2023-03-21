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

package pascal.taie.analysis.pta.plugin.invokedynamic;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.AbstractModel;
import pascal.taie.analysis.pta.plugin.util.CSObjs;
import pascal.taie.analysis.pta.plugin.util.InvokeHandler;
import pascal.taie.analysis.pta.plugin.util.Reflections;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.MethodHandle;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Models java.lang.invoke.MethodHandles.Lookup.find*(...).
 * For details, please refer to
 * <a href="https://docs.oracle.com/javase/7/docs/api/java/lang/invoke/MethodHandles.Lookup.html">MethodHandles.Lookup.html</a>
 * TODO: take Lookup.lookupClass's visibility into account
 * TODO: take MethodType into account
 */
public class LookupModel extends AbstractModel {

    LookupModel(Solver solver) {
        super(solver);
    }

    @InvokeHandler(signature = "<java.lang.invoke.MethodHandles$Lookup: java.lang.invoke.MethodHandle findConstructor(java.lang.Class,java.lang.invoke.MethodType)>", argIndexes = {0})
    public void findConstructor(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            Context context = csVar.getContext();
            pts.forEach(clsObj -> {
                JClass cls = CSObjs.toClass(clsObj);
                if (cls != null) {
                    Reflections.getDeclaredConstructors(cls)
                            .map(ctor -> MethodHandle.get(
                                    MethodHandle.Kind.REF_newInvokeSpecial,
                                    ctor.getRef()))
                            .map(heapModel::getConstantObj)
                            .forEach(mhObj -> solver.addVarPointsTo(context, result, mhObj));
                }
            });
        }
    }

    @InvokeHandler(signature = "<java.lang.invoke.MethodHandles$Lookup: java.lang.invoke.MethodHandle findVirtual(java.lang.Class,java.lang.String,java.lang.invoke.MethodType)>", argIndexes = {0, 1})
    public void findVirtual(CSVar csVar, PointsToSet pts, Invoke invoke) {
        // TODO: find private methods in (direct/indirect) super class.
        findMethod(csVar, pts, invoke, (cls, name) ->
                        Reflections.getDeclaredMethods(cls, name)
                                .filter(Predicate.not(JMethod::isStatic)),
                MethodHandle.Kind.REF_invokeVirtual);
    }

    @InvokeHandler(signature = "<java.lang.invoke.MethodHandles$Lookup: java.lang.invoke.MethodHandle findStatic(java.lang.Class,java.lang.String,java.lang.invoke.MethodType)>", argIndexes = {0, 1})
    public void findStatic(CSVar csVar, PointsToSet pts, Invoke invoke) {
        // TODO: find static methods in (direct/indirect) super class.
        findMethod(csVar, pts, invoke, (cls, name) ->
                        Reflections.getDeclaredMethods(cls, name)
                                .filter(JMethod::isStatic),
                MethodHandle.Kind.REF_invokeStatic);
    }

    private void findMethod(
            CSVar csVar, PointsToSet pts, Invoke invoke,
            BiFunction<JClass, String, Stream<JMethod>> getter,
            MethodHandle.Kind kind) {
        Var result = invoke.getResult();
        if (result != null) {
            List<PointsToSet> args = getArgs(csVar, pts, invoke, 0, 1);
            PointsToSet clsObjs = args.get(0);
            PointsToSet nameObjs = args.get(1);
            Context context = csVar.getContext();
            clsObjs.forEach(clsObj -> {
                JClass cls = CSObjs.toClass(clsObj);
                if (cls != null) {
                    nameObjs.forEach(nameObj -> {
                        String name = CSObjs.toString(nameObj);
                        if (name != null) {
                            getter.apply(cls, name)
                                    .map(mtd -> MethodHandle.get(kind, mtd.getRef()))
                                    .map(heapModel::getConstantObj)
                                    .forEach(mhObj ->
                                            solver.addVarPointsTo(context, result, mhObj));
                        }
                    });
                }
            });
        }
    }
}
