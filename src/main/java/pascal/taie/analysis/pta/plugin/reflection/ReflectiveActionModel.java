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
import pascal.taie.analysis.pta.core.cs.element.InstanceField;
import pascal.taie.analysis.pta.core.cs.element.StaticField;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.heap.MockObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.AbstractModel;
import pascal.taie.analysis.pta.plugin.util.CSObjUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeManager;
import pascal.taie.util.collection.MapUtils;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

import static pascal.taie.util.collection.MapUtils.addToMapMap;
import static pascal.taie.util.collection.MapUtils.getMapMap;

/**
 * Models reflective-action methods, currently supports
 * - Class.forName(String)
 * - Class.forName(String,boolean,ClassLoader)
 * - Class.newInstance()
 * - Constructor.newInstance(Object[])
 * - Method.invoke(Object,Object[])
 * - Field.get(Object)
 * - Field.set(Object,Object)
 * - Array.newInstance(Class,int)
 * TODO: check accessibility
 */
class ReflectiveActionModel extends AbstractModel {

    /**
     * Description for objects created by reflective newInstance() calls.
     */
    private final static String REF_OBJ_DESC = "ReflectiveObj";

    private final Subsignature initNoArg;

    private final ContextSelector selector;

    private final TypeManager typeManager;

    /**
     * Map from Invoke (of Class/Constructor/Array.newInstance()) and type
     * to the reflectively-created objects.
     */
    private final Map<Invoke, Map<ReferenceType, MockObj>> newObjs = MapUtils.newMap();

    ReflectiveActionModel(Solver solver) {
        super(solver);
        initNoArg = Subsignature.get(StringReps.INIT_NO_ARG);
        selector = solver.getContextSelector();
        typeManager = solver.getTypeManager();
    }

    @Override
    protected void registerVarAndHandler() {
        JMethod classForName = hierarchy.getJREMethod("<java.lang.Class: java.lang.Class forName(java.lang.String)>");
        registerRelevantVarIndexes(classForName, 0);
        registerAPIHandler(classForName, this::classForName);

        JMethod classForName2 = hierarchy.getJREMethod("<java.lang.Class: java.lang.Class forName(java.lang.String,boolean,java.lang.ClassLoader)>");
        // TODO: take class loader into account
        registerRelevantVarIndexes(classForName2, 0);
        registerAPIHandler(classForName2, this::classForName);

        JMethod classNewInstance = hierarchy.getJREMethod("<java.lang.Class: java.lang.Object newInstance()>");
        registerRelevantVarIndexes(classNewInstance, BASE);
        registerAPIHandler(classNewInstance, this::classNewInstance);

        JMethod constructorNewInstance = hierarchy.getJREMethod("<java.lang.reflect.Constructor: java.lang.Object newInstance(java.lang.Object[])>");
        registerRelevantVarIndexes(constructorNewInstance, BASE);
        registerAPIHandler(constructorNewInstance, this::constructorNewInstance);

        JMethod methodInvoke = hierarchy.getJREMethod("<java.lang.reflect.Method: java.lang.Object invoke(java.lang.Object,java.lang.Object[])>");
        registerRelevantVarIndexes(methodInvoke, BASE, 0);
        registerAPIHandler(methodInvoke, this::methodInvoke);

        JMethod fieldGet = hierarchy.getJREMethod("<java.lang.reflect.Field: java.lang.Object get(java.lang.Object)>");
        registerRelevantVarIndexes(fieldGet, BASE, 0);
        registerAPIHandler(fieldGet, this::fieldGet);

        JMethod fieldSet = hierarchy.getJREMethod("<java.lang.reflect.Field: void set(java.lang.Object,java.lang.Object)>");
        registerRelevantVarIndexes(fieldSet, BASE, 0);
        registerAPIHandler(fieldSet, this::fieldSet);

        JMethod arrayNewInstance = hierarchy.getJREMethod("<java.lang.reflect.Array: java.lang.Object newInstance(java.lang.Class,int)>");
        registerRelevantVarIndexes(arrayNewInstance, 0);
        registerAPIHandler(arrayNewInstance, this::arrayNewInstance);
    }

    private void classForName(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Context context = csVar.getContext();
        pts.forEach(obj -> {
            String className = CSObjUtils.toString(obj);
            if (className == null) {
                return;
            }
            JClass klass = hierarchy.getClass(className);
            if (klass == null) {
                return;
            }
            solver.initializeClass(klass);
            Var result = invoke.getResult();
            if (result != null) {
                Obj clsObj = heapModel.getConstantObj(
                        ClassLiteral.get(klass.getType()));
                CSObj csObj = csManager.getCSObj(defaultHctx, clsObj);
                solver.addVarPointsTo(context, result, csObj);
            }
        });
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

    private CSObj newReflectiveObj(Context context, Invoke invoke, ReferenceType type) {
        MockObj newObj = getMapMap(newObjs, invoke, type);
        if (newObj == null) {
            newObj = new MockObj(REF_OBJ_DESC, invoke, type,
                    invoke.getContainer());
            // TODO: process newObj by heapModel?
            addToMapMap(newObjs, invoke, type, newObj);
        }
        // TODO: double-check if the heap context is proper
        CSObj csNewObj = csManager.getCSObj(context, newObj);
        Var result = invoke.getResult();
        if (result != null) {
            solver.addVarPointsTo(context, result, csNewObj);
        }
        return csNewObj;
    }

    private void methodInvoke(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Context context = csVar.getContext();
        List<PointsToSet> args = getArgs(csVar, pts, invoke, BASE, 0);
        PointsToSet mtdObjs = args.get(0);
        PointsToSet recvObjs = args.get(1);
        Var argsVar = invoke.getInvokeExp().getArg(1);
        mtdObjs.forEach(mtdObj -> {
            JMethod target = CSObjUtils.toMethod(mtdObj);
            if (target == null) {
                return;
            }
            if (target.isStatic()) {
                addReflectiveCallEdge(context, invoke, null, target, argsVar);
            } else {
                recvObjs.forEach(recvObj ->
                        addReflectiveCallEdge(context, invoke, recvObj, target, argsVar)
                );
            }
        });
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

    private void fieldGet(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result == null) {
            return;
        }
        Context context = csVar.getContext();
        CSVar to = csManager.getCSVar(context, result);
        List<PointsToSet> args = getArgs(csVar, pts, invoke, BASE, 0);
        PointsToSet fldObjs = args.get(0);
        PointsToSet baseObjs = args.get(1);
        fldObjs.forEach(fldObj -> {
            JField field = CSObjUtils.toField(fldObj);
            if (field == null) {
                return;
            }
            if (field.isStatic()) {
                StaticField sfield = csManager.getStaticField(field);
                solver.addPFGEdge(sfield, to, PointerFlowEdge.Kind.STATIC_LOAD);
            } else {
                Type declType = field.getDeclaringClass().getType();
                baseObjs.forEach(baseObj -> {
                    Type objType = baseObj.getObject().getType();
                    if (typeManager.isSubtype(declType, objType)) {
                        InstanceField ifield = csManager.getInstanceField(baseObj, field);
                        solver.addPFGEdge(ifield, to, PointerFlowEdge.Kind.INSTANCE_LOAD);
                    }
                });
            }
        });
    }

    private void fieldSet(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Context context = csVar.getContext();
        CSVar from = csManager.getCSVar(context, invoke.getInvokeExp().getArg(1));
        List<PointsToSet> args = getArgs(csVar, pts, invoke, BASE, 0);
        PointsToSet fldObjs = args.get(0);
        PointsToSet baseObjs = args.get(1);
        fldObjs.forEach(fldObj -> {
            JField field = CSObjUtils.toField(fldObj);
            if (field == null) {
                return;
            }
            if (field.isStatic()) {
                StaticField sfield = csManager.getStaticField(field);
                solver.addPFGEdge(from, sfield, PointerFlowEdge.Kind.STATIC_STORE);
            } else {
                Type declType = field.getDeclaringClass().getType();
                baseObjs.forEach(baseObj -> {
                    Type objType = baseObj.getObject().getType();
                    if (typeManager.isSubtype(declType, objType)) {
                        InstanceField ifield = csManager.getInstanceField(baseObj, field);
                        solver.addPFGEdge(from, ifield, ifield.getType(),
                                PointerFlowEdge.Kind.INSTANCE_STORE);
                    }
                });
            }
        });
    }

    private void arrayNewInstance(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result == null) {
            return;
        }
        Context context = csVar.getContext();
        pts.forEach(obj -> {
            Type baseType = CSObjUtils.toType(obj);
            if (baseType == null) {
                return;
            }
            ArrayType arrayType = typeManager.getArrayType(baseType, 1);
            CSObj csNewArray = newReflectiveObj(context, invoke, arrayType);
            solver.addVarPointsTo(context, result, csNewArray);
        });
    }
}
