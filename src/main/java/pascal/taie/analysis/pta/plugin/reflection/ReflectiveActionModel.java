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
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.cs.element.InstanceField;
import pascal.taie.analysis.pta.core.cs.element.StaticField;
import pascal.taie.analysis.pta.core.cs.selector.ContextSelector;
import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.AbstractModel;
import pascal.taie.analysis.pta.plugin.util.CSObjs;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.ReferenceType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.language.type.VoidType;

import javax.annotation.Nullable;
import java.util.List;

import static pascal.taie.analysis.pta.core.solver.PointerFlowEdge.Kind.INSTANCE_STORE;
import static pascal.taie.analysis.pta.core.solver.PointerFlowEdge.Kind.STATIC_STORE;

/**
 * Models reflective-action methods, currently supports
 * <ul>
 *     <li>Class.forName(String)
 *     <li>Class.forName(String,boolean,ClassLoader)
 *     <li>Class.newInstance()
 *     <li>Constructor.newInstance(Object[])
 *     <li>Method.invoke(Object,Object[])
 *     <li>Field.get(Object)
 *     <li>Field.set(Object,Object)
 *     <li>Array.newInstance(Class,int)
 * </ul>
 * TODO: check accessibility
 */
class ReflectiveActionModel extends AbstractModel {

    /**
     * Descriptor for objects created by reflective newInstance() calls.
     */
    private final static Descriptor REF_OBJ_DESC = () -> "ReflectiveObj";

    private final Subsignature initNoArg;

    private final ContextSelector selector;

    private final TypeSystem typeSystem;

    ReflectiveActionModel(Solver solver) {
        super(solver);
        initNoArg = Subsignature.getNoArgInit();
        selector = solver.getContextSelector();
        typeSystem = solver.getTypeSystem();
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
            String className = CSObjs.toString(obj);
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
            JClass klass = CSObjs.toClass(obj);
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
            JMethod constructor = CSObjs.toConstructor(obj);
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
        Obj newObj = heapModel.getMockObj(REF_OBJ_DESC,
                invoke, type, invoke.getContainer());
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
            JMethod target = CSObjs.toMethod(mtdObj);
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
            JField field = CSObjs.toField(fldObj);
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
                    if (typeSystem.isSubtype(declType, objType)) {
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
            JField field = CSObjs.toField(fldObj);
            if (field == null) {
                return;
            }
            if (field.isStatic()) {
                StaticField sfield = csManager.getStaticField(field);
                solver.addPFGEdge(from, sfield, STATIC_STORE, sfield.getType());
            } else {
                Type declType = field.getDeclaringClass().getType();
                baseObjs.forEach(baseObj -> {
                    Type objType = baseObj.getObject().getType();
                    if (typeSystem.isSubtype(declType, objType)) {
                        InstanceField ifield = csManager.getInstanceField(baseObj, field);
                        solver.addPFGEdge(from, ifield, INSTANCE_STORE, ifield.getType());
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
            Type baseType = CSObjs.toType(obj);
            if (baseType == null || baseType instanceof VoidType) {
                return;
            }
            ArrayType arrayType = typeSystem.getArrayType(baseType, 1);
            CSObj csNewArray = newReflectiveObj(context, invoke, arrayType);
            solver.addVarPointsTo(context, result, csNewArray);
        });
    }
}
