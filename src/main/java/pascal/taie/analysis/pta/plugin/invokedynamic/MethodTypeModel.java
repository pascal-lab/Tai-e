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
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeManager;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Models invocations to MethodType.methodType(*);
 */
class MethodTypeModel extends AbstractModel {

    private final JMethod methodType0Arg;

    private final JMethod methodType1Arg;

    private final JMethod methodTypeMT;

    private final List<JMethod> methodTypeMethods;

    MethodTypeModel(Solver solver) {
        super(solver);
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

    @Override
    public void handleNewInvoke(Invoke invoke) {
        JMethod target = invoke.getMethodRef().resolve();
        if (methodTypeMethods.contains(target)) {
            // record MethodType-related variables
            for (int i = 0; i < invoke.getInvokeExp().getArgCount(); ++i) {
                addRelevantArg(invoke, i);
            }
        }
    }

    @Override
    public void handleNewPointsToSet(CSVar csVar, PointsToSet pts) {
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

    private void handleMethodType1Arg(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            List<PointsToSet> args = getArgs(csVar, pts, invoke);
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

    private void handleMethodTypeMT(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            List<PointsToSet> args = getArgs(csVar, pts, invoke);
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
                        return solver.getPointsToSetOf(csArg);
                    }
                })
                .collect(Collectors.toUnmodifiableList());
    }
}
