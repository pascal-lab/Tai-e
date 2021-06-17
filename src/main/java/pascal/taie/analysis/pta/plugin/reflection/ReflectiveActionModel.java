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

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.heap.MockObj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.AbstractModel;
import pascal.taie.analysis.pta.plugin.util.CSObjUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.MapUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static pascal.taie.util.collection.MapUtils.addToMapMap;
import static pascal.taie.util.collection.MapUtils.getMapMap;

/**
 * Models reflective-action methods, currently supports
 * - Class.newInstance()
 * - Constructor.newInstance(Object[])
 * - Method.invoke(Object,Object[])
 * TODO:
 *  - pass reflective arguments and return values
 *  - trigger class initializer
 *  - check accessibility
 */
class ReflectiveActionModel extends AbstractModel {

    /**
     * Description for objects created by reflective newInstance() calls.
     */
    private final static String REF_OBJ_DESC = "ReflectiveObj";

    private final Subsignature initNoArg;

    private final ContextSelector selector;

    /**
     * Map from Invoke (of newInstance()) and type to reflectively-created objects.
     */
    private final Map<Invoke, Map<ClassType, MockObj>> newObjs = MapUtils.newMap();

    ReflectiveActionModel(Solver solver) {
        super(solver);
        initNoArg = Subsignature.get(StringReps.INIT_NO_ARG);
        selector = solver.getContextSelector();
    }

    @Override
    protected void registerVarAndHandler() {
        JMethod classNewInstance = hierarchy.getJREMethod("<java.lang.Class: java.lang.Object newInstance()>");
        registerRelevantVarIndexes(classNewInstance, BASE);
        registerAPIHandler(classNewInstance, this::classNewInstance);

        JMethod constructorNewInstance = hierarchy.getJREMethod("<java.lang.reflect.Constructor: java.lang.Object newInstance(java.lang.Object[])>");
        registerRelevantVarIndexes(constructorNewInstance, BASE);
        registerAPIHandler(constructorNewInstance, this::constructorNewInstance);

        JMethod methodInvoke = hierarchy.getJREMethod("<java.lang.reflect.Method: java.lang.Object invoke(java.lang.Object,java.lang.Object[])>");
        registerRelevantVarIndexes(methodInvoke, BASE, 0);
        registerAPIHandler(methodInvoke, this::methodInvoke);
    }

    private void classNewInstance(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Context context = csVar.getContext();
        pts.forEach(obj -> {
            JClass klass = CSObjUtils.toClass(obj);
            if (klass == null) {
                return;
            }
            JMethod init = klass.getDeclaredMethod(initNoArg);
            if (init == null) {
                return;
            }
            ClassType type = klass.getType();
            CSObj csNewObj = newReflectiveObj(context, invoke, type);
            addReflectiveCallEdge(context, invoke, csNewObj, init, null);
        });
    }

    private void constructorNewInstance(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Context context = csVar.getContext();
        pts.forEach(obj -> {
            JMethod constructor = CSObjUtils.toConstructor(obj);
            if (constructor == null) {
                return;
            }
            ClassType type = constructor.getDeclaringClass().getType();
            CSObj csNewObj = newReflectiveObj(context, invoke, type);
            addReflectiveCallEdge(context, invoke, csNewObj,
                    constructor, invoke.getInvokeExp().getArg(0));
        });
    }

    private void methodInvoke(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Context context = csVar.getContext();
        List<PointsToSet> args = getArgs(csVar, pts, invoke, BASE, 0);
        PointsToSet mtdObjs = args.get(0);
        PointsToSet recvObjs = args.get(1);
        Var argsVar = invoke.getInvokeExp().getArg(1);
        mtdObjs.forEach(mtdObj -> {
            JMethod method = CSObjUtils.toMethod(mtdObj);
            if (method == null) {
                return;
            }
            if (method.isStatic()) {
                addReflectiveCallEdge(context, invoke, null, method, argsVar);
            } else {
                recvObjs.forEach(recvObj ->
                        addReflectiveCallEdge(context, invoke, recvObj, method, argsVar)
                );
            }
        });
    }

    private CSObj newReflectiveObj(Context context, Invoke invoke, ClassType type) {
        MockObj newObj = getMapMap(newObjs, invoke, type);
        if (newObj == null) {
            newObj = new MockObj(REF_OBJ_DESC, invoke, type,
                    invoke.getContainer());
            // TODO: process newObj by heapModel?
            addToMapMap(newObjs, invoke, type, newObj);
        }
        CSObj csNewObj = csManager.getCSObj(context, newObj);
        Var result = invoke.getResult();
        if (result != null) {
            solver.addVarPointsTo(context, result, csNewObj);
        }
        return csNewObj;
    }

    private void addReflectiveCallEdge(
            Context callerCtx, Invoke callSite,
            @Nullable CSObj recvObj, JMethod callee, Var args) {
        if (!callee.isConstructor() && !callee.isStatic()) {
            // dispatch for instance method (except constructor)
            assert recvObj != null : "recvObj is required for instance method";
            callee = hierarchy.dispatch(recvObj.getObject().getType(),
                    callee.getRef());
            if (callee == null) {
                return;
            }
        }
        CSCallSite csCallSite = csManager.getCSCallSite(callerCtx, callSite);
        Context calleeCtx;
        if (callee.isStatic()) {
            calleeCtx = selector.selectContext(csCallSite, callee);
        } else {
            calleeCtx = selector.selectContext(csCallSite, recvObj, callee);
            // pass receiver object to 'this' variable of callee
            solver.addVarPointsTo(calleeCtx, callee.getIR().getThis(), recvObj);
        }
        ReflectiveCallEdge callEdge = new ReflectiveCallEdge(csCallSite,
                csManager.getCSMethod(calleeCtx, callee), args);
        solver.addCallEdge(callEdge);
    }
}
