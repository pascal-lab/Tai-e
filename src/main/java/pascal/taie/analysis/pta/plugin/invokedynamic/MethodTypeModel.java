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

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.analysis.pta.pts.PointsToSetFactory;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.MethodType;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeManager;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static pascal.taie.util.collection.MapUtils.addToMapSet;
import static pascal.taie.util.collection.MapUtils.newHybridMap;

/**
 * Models invocations to MethodType.methodType(*);
 */
class MethodTypeModel {

    private final Solver pta;

    private final CSManager csManager;

    private final HeapModel heapModel;

    /**
     * Default heap context for MethodType objects.
     */
    private final Context defaultHctx;

    private final JMethod methodType0Arg;

    private final JMethod methodType1Arg;

    private final JMethod methodTypeMT;

    private final List<JMethod> methodTypeMethods;

    private final Map<Var, Set<Invoke>> relevantVars = newHybridMap();

    MethodTypeModel(Solver solver) {
        this.pta = solver;
        csManager = solver.getCSManager();
        heapModel = solver.getHeapModel();
        defaultHctx = solver.getContextSelector().getDefaultContext();
        ClassHierarchy hierarchy = solver.getHierarchy();
        TypeManager typeManager = solver.getTypeManager();
        JClass methodType = hierarchy.getJREClass(StringReps.METHOD_TYPE);
        Type mt = methodType.getType();
        Type klass = typeManager.getClassType(StringReps.CLASS);
        methodType0Arg = methodType.getDeclaredMethod(
                Subsignature.get("methodType", List.of(klass), mt));
        methodType1Arg = methodType.getDeclaredMethod(
                Subsignature.get("methodType", List.of(klass, klass), mt));
        methodTypeMT = methodType.getDeclaredMethod(
                Subsignature.get("methodType", List.of(klass, mt), mt));
        methodTypeMethods = List.of(methodType0Arg, methodType1Arg, methodTypeMT);
    }

    void handleNewInvoke(Invoke invoke) {
        if (invoke.isStatic()) {
            JMethod target = invoke.getMethodRef().resolve();
            if (methodTypeMethods.contains(target)) {
                // record MethodType-related variables
                invoke.getInvokeExp().getArgs().forEach(arg ->
                        addToMapSet(relevantVars, arg, invoke));
            }
        }
    }

    boolean isRelevantVar(Var var) {
        return relevantVars.containsKey(var);
    }

    void handleNewPointsToSet(CSVar csVar, PointsToSet pts) {
        relevantVars.get(csVar.getVar()).forEach(invoke -> {
            JMethod target = invoke.getMethodRef().resolve();
            if (target.equals(methodType0Arg)) {
                handleMethodType0Arg(csVar, pts, invoke);
            } else if (target.equals(methodType1Arg)) {
                handleMethodType1Arg(csVar, pts, invoke);
            } else if (target.equals(methodTypeMT)) {
                handleMethodTypeMT(csVar, pts, invoke);
            }
        });
    }

    private void handleMethodType0Arg(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            PointsToSet mtObjs = PointsToSetFactory.make();
            pts.forEach(obj -> {
                Type retType = toType(obj);
                if (retType != null) {
                    MethodType mt = MethodType.get(List.of(), retType);
                    Obj mtObj = heapModel.getConstantObj(mt);
                    mtObjs.addObject(csManager.getCSObj(defaultHctx, mtObj));
                }
            });
            if (!mtObjs.isEmpty()) {
                pta.addVarPointsTo(csVar.getContext(), result, mtObjs);
            }
        }
    }

    private void handleMethodType1Arg(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            List<PointsToSet> args = getArgs(csVar, pts, invoke);
            PointsToSet retObjs = args.get(0);
            PointsToSet paramObjs = args.get(1);
            PointsToSet mtObjs = PointsToSetFactory.make();
            retObjs.forEach(retObj -> {
                Type retType = toType(retObj);
                if (retType != null) {
                    paramObjs.forEach(paramObj -> {
                        Type paramType = toType(paramObj);
                        if (paramType != null) {
                            MethodType mt = MethodType.get(List.of(paramType), retType);
                            Obj mtObj = heapModel.getConstantObj(mt);
                            mtObjs.addObject(csManager.getCSObj(defaultHctx, mtObj));
                        }
                    });
                }
            });
            if (!mtObjs.isEmpty()) {
                pta.addVarPointsTo(csVar.getContext(), result, mtObjs);
            }
        }
    }

    private void handleMethodTypeMT(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            List<PointsToSet> args = getArgs(csVar, pts, invoke);
            PointsToSet retObjs = args.get(0);
            PointsToSet mtObjs = args.get(1);
            PointsToSet resultMTObjs = PointsToSetFactory.make();
            retObjs.forEach(retObj -> {
                Type retType = toType(retObj);
                if (retType != null) {
                    mtObjs.forEach(mtObj -> {
                        MethodType mt = toMethodType(mtObj);
                        if (mt != null) {
                            MethodType resultMT = MethodType.get(mt.getParamTypes(), retType);
                            Obj resultMTObj = heapModel.getConstantObj(resultMT);
                            resultMTObjs.addObject(csManager.getCSObj(defaultHctx, resultMTObj));
                        }
                    });
                }
            });
            if (!resultMTObjs.isEmpty()) {
                pta.addVarPointsTo(csVar.getContext(), result, resultMTObjs);
            }
        }
    }

    /**
     * For invocation MethodType.methodType(a0, a1, ...);
     * when points-to set of a0, a1 or an changes,
     * this convenient method returns points-to sets of a0, a1, ...
     * For case ai == csVar.getVar(), this method returns pts,
     * otherwise, it just returns current points-to set of ai.
     * @param csVar may be any of ai.
     * @param pts changed part of csVar
     * @param invoke the call site which contain csVar
     */
    private List<PointsToSet> getArgs(CSVar csVar, PointsToSet pts, Invoke invoke) {
        return invoke.getInvokeExp().getArgs()
                .stream()
                .map(arg -> {
                    if (arg.equals(csVar.getVar())) {
                        return pts;
                    } else {
                        CSVar csArg = csManager.getCSVar(csVar.getContext(), arg);
                        return pta.getPointsToSetOf(csArg);
                    }
                })
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Converts a CSObj of class to corresponding type. If the object is
     * not a class constant, then return null.
     */
    private static @Nullable Type toType(CSObj csObj) {
        Object alloc = csObj.getObject().getAllocation();
        return alloc instanceof ClassLiteral ?
                ((ClassLiteral) alloc).getTypeValue() : null;
    }

    /**
     * Converts a CSObj of MethodType to corresponding MethodType.
     * If the object is not a MethodType, then return null.
     */
    private static @Nullable MethodType toMethodType(CSObj csObj) {
        Object alloc = csObj.getObject().getAllocation();
        return alloc instanceof MethodType ? (MethodType) alloc : null;
    }
}
