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
import pascal.taie.analysis.pta.plugin.util.ReflectionUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.analysis.pta.pts.PointsToSetFactory;
import pascal.taie.ir.exp.MethodHandle;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.StringReps;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Models java.lang.invoke.MethodHandles.Lookup.find*(...).
 * For details, please refer to
 * https://docs.oracle.com/javase/7/docs/api/java/lang/invoke/MethodHandles.Lookup.html
 * TODO: take Lookup.lookupClass's visibility into account
 * TODO: take MethodType into account
 */
class LookupModel extends AbstractModel {

    private static final String FIND_CONSTRUCTOR = "findConstructor";

    private static final String FIND_VIRTUAL = "findVirtual";

    private static final String FIND_STATIC = "findStatic";

    private final JClass lookup;

    LookupModel(Solver solver) {
        super(solver);
        lookup = hierarchy.getJREClass(StringReps.LOOKUP);
    }

    @Override
    public void handleNewInvoke(Invoke invoke) {
        MethodRef ref = invoke.getMethodRef();
        if (ref.getDeclaringClass().equals(lookup)) {
            switch (ref.getName()) {
                case FIND_CONSTRUCTOR: {
                    addRelevantArg(invoke, 0);
                    break;
                }
                case FIND_VIRTUAL:
                case FIND_STATIC: {
                    addRelevantArg(invoke, 0);
                    addRelevantArg(invoke, 1);
                    break;
                }
            }
        }
    }

    @Override
    public void handleNewPointsToSet(CSVar csVar, PointsToSet pts) {
        relevantVars.get(csVar.getVar()).forEach(invoke -> {
            switch (invoke.getMethodRef().getName()) {
                case FIND_CONSTRUCTOR: {
                    handleFindConstructor(csVar, pts, invoke);
                    break;
                }
                case FIND_VIRTUAL: {
                    handleFindVirtual(csVar, pts, invoke);
                    break;
                }
                case FIND_STATIC: {
                    handleFindStatic(csVar, pts, invoke);
                    break;
                }
            }
        });
    }

    private void handleFindConstructor(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            PointsToSet mhObjs = PointsToSetFactory.make();
            pts.forEach(clsObj -> {
                JClass cls = CSObjUtils.toClass(clsObj);
                if (cls != null) {
                    ReflectionUtils.getDeclaredConstructors(cls)
                            .map(ctor -> {
                                MethodHandle mh = MethodHandle.get(
                                        MethodHandle.Kind.REF_newInvokeSpecial,
                                        ctor.getRef());
                                Obj mhObj = heapModel.getConstantObj(mh);
                                return csManager.getCSObj(defaultHctx, mhObj);
                            })
                            .forEach(mhObjs::addObject);
                }
            });
            if (!mhObjs.isEmpty()) {
                solver.addVarPointsTo(csVar.getContext(), result, mhObjs);
            }
        }
    }

    private void handleFindVirtual(CSVar csVar, PointsToSet pts, Invoke invoke) {
        // TODO: find private methods in (direct/indirect) super class.
        handleFindMethod(csVar, pts, invoke, (cls, name) ->
                ReflectionUtils.getDeclaredMethods(cls, name)
                        .filter(Predicate.not(JMethod::isStatic)),
                MethodHandle.Kind.REF_invokeVirtual);
    }

    private void handleFindStatic(CSVar csVar, PointsToSet pts, Invoke invoke) {
        // TODO: find static methods in (direct/indirect) super class.
        handleFindMethod(csVar, pts, invoke, (cls, name) ->
                ReflectionUtils.getDeclaredMethods(cls, name)
                        .filter(JMethod::isStatic),
                MethodHandle.Kind.REF_invokeStatic);
    }

    private void handleFindMethod(
            CSVar csVar, PointsToSet pts, Invoke invoke,
            BiFunction<JClass, String, Stream<JMethod>> getter,
            MethodHandle.Kind kind) {
        Var result = invoke.getResult();
        if (result != null) {
            List<PointsToSet> args = getArgs(csVar, pts, invoke, 0, 1);
            PointsToSet clsObjs = args.get(0);
            PointsToSet nameObjs = args.get(1);
            PointsToSet mhObjs = PointsToSetFactory.make();
            clsObjs.forEach(clsObj -> {
                JClass cls = CSObjUtils.toClass(clsObj);
                if (cls != null) {
                    nameObjs.forEach(nameObj -> {
                        String name = CSObjUtils.toString(nameObj);
                        if (name != null) {
                            getter.apply(cls, name)
                                    .map(mtd -> {
                                        MethodHandle mh = MethodHandle.get(
                                                kind, mtd.getRef());
                                        Obj mhObj = heapModel.getConstantObj(mh);
                                        return csManager.getCSObj(defaultHctx, mhObj);
                                    })
                                    .forEach(mhObjs::addObject);
                        }
                    });
                }
            });
            if (!mhObjs.isEmpty()) {
                solver.addVarPointsTo(csVar.getContext(), result, mhObjs);
            }
        }
    }
}
