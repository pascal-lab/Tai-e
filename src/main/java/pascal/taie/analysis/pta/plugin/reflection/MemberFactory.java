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
import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.ConstantObj;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.PointerAnalysis;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.analysis.pta.pts.PointsToSetFactory;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.InvokeVirtual;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.ClassMember;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeManager;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static pascal.taie.util.collection.CollectionUtils.addToMapSet;
import static pascal.taie.util.collection.CollectionUtils.newHybridMap;
import static pascal.taie.util.collection.CollectionUtils.newMap;

/**
 * Model member-retrieving methods.
 */
class MemberFactory {

    private final PointerAnalysis pta;

    private final ClassHierarchy hierarchy;

    private final CSManager csManager;

    private final HeapModel heapModel;

    /**
     * Default heap context for MethodType objects.
     */
    private final Context defaultHctx;

    private final Type constructor;

    private final Type method;

    private final Type field;

    private final JMethod getConstructor;

    private final JMethod getDeclaredConstructor;

    private final JMethod getMethod;

    private final JMethod getDeclaredMethod;

    private final Map<Var, Set<Invoke>> relevantVars = newHybridMap();

    private final Map<ClassMember, ReflectionObj> refObjs = newMap();

    public MemberFactory(PointerAnalysis pta) {
        this.pta = pta;
        hierarchy = pta.getHierarchy();
        csManager = pta.getCSManager();
        heapModel = pta.getHeapModel();
        defaultHctx = pta.getContextSelector().getDefaultContext();
        TypeManager typeManager = pta.getTypeManager();
        constructor = typeManager.getClassType(StringReps.CONSTRUCTOR);
        method = typeManager.getClassType(StringReps.METHOD);
        field = typeManager.getClassType(StringReps.FIELD);
        JClass klass = hierarchy.getJREClass(StringReps.CLASS);
        getConstructor = klass.getDeclaredMethod("getConstructor");
        getDeclaredConstructor = klass.getDeclaredMethod("getDeclaredConstructor");
        getMethod = klass.getDeclaredMethod("getMethod");
        getDeclaredMethod = klass.getDeclaredMethod("getDeclaredMethod");
    }

    void handleNewInvoke(Invoke invoke) {
        if (invoke.getInvokeExp() instanceof InvokeVirtual) {
            JMethod target = invoke.getMethodRef().resolve();
            if (target.equals(getConstructor) ||
                    target.equals(getDeclaredConstructor)) {
                InvokeVirtual invokeExp = (InvokeVirtual) invoke.getInvokeExp();
                addToMapSet(relevantVars, invokeExp.getBase(), invoke);
            } else if (target.equals(getMethod) ||
                    target.equals(getDeclaredMethod)) {
                InvokeVirtual invokeExp = (InvokeVirtual) invoke.getInvokeExp();
                addToMapSet(relevantVars, invokeExp.getBase(), invoke);
                addToMapSet(relevantVars, invokeExp.getArg(0), invoke);
            }
        }
    }

    boolean isRelevantVar(Var var) {
        return relevantVars.containsKey(var);
    }

    void handleNewPointsToSet(CSVar csVar, PointsToSet pts) {
        relevantVars.get(csVar.getVar()).forEach(invoke -> {
            JMethod target = invoke.getMethodRef().resolve();
            if (target.equals(getConstructor)) {
                handleGetConstructor(csVar, pts, invoke);
            } else if (target.equals(getDeclaredConstructor)) {
                handleGetDeclaredConstructor(csVar, pts, invoke);
            } else if (target.equals(getMethod)) {
                handleGetMethod(csVar, pts, invoke);
            } else if (target.equals(getDeclaredMethod)) {
                handleGetDeclaredMethod(csVar, pts, invoke);
            }
        });
    }

    private void handleGetConstructor(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            PointsToSet ctorObjs = PointsToSetFactory.make();
            pts.forEach(obj -> {
                JClass jclass = toClass(obj);
                if (jclass != null) {
                    ReflectionUtils.getPublicConstructors(jclass)
                            .map(ctor -> {
                                Obj ctorObj = getReflectionObj(ctor);
                                return csManager.getCSObj(defaultHctx, ctorObj);
                            })
                            .forEach(ctorObjs::addObject);
                }
            });
            if (!ctorObjs.isEmpty()) {
                pta.addVarPointsTo(csVar.getContext(), result, ctorObjs);
            }
        }
    }

    private void handleGetDeclaredConstructor(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            PointsToSet ctorObjs = PointsToSetFactory.make();
            pts.forEach(obj -> {
                JClass jclass = toClass(obj);
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
                pta.addVarPointsTo(csVar.getContext(), result, ctorObjs);
            }
        }
    }

    private void handleGetMethod(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {

        }
    }

    private void handleGetDeclaredMethod(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {

        }
    }

    private ReflectionObj getReflectionObj(ClassMember member) {
        return refObjs.computeIfAbsent(member, m -> {
           if (m instanceof JMethod) {
               if (((JMethod) m).isConstructor()) {
                   return new ReflectionObj(constructor, m);
               } else {
                   return new ReflectionObj(method, m);
               }
           } else {
               return new ReflectionObj(field, m);
           }
        });
    }

    /**
     * For invocation m = c.getMethod(n, ...);
     * when points-to set of c or n changes,
     * this convenient method returns points-to sets of c and n.
     * For variable csVar.getVar(), this method returns pts,
     * otherwise, it just returns current points-to set of the variable.
     * @param csVar may be c or n.
     * @param pts changed part of csVar
     * @param invokeExp the call site which contain csVar
     */
    private List<PointsToSet> getArgs(CSVar csVar, PointsToSet pts,
                                      InvokeVirtual invokeExp) {
        PointsToSet basePts, arg0Pts;
        if (csVar.getVar().equals(invokeExp.getBase())) {
            basePts = pts;
            CSVar arg0 = csManager.getCSVar(csVar.getContext(),
                    invokeExp.getArg(0));
            arg0Pts = pta.getPointsToSetOf(arg0);
        } else {
            CSVar base = csManager.getCSVar(csVar.getContext(),
                    invokeExp.getBase());
            basePts = pta.getPointsToSetOf(base);
            arg0Pts = pts;
        }
        return List.of(basePts, arg0Pts);
    }

    /**
     * Convert a CSObj of class to corresponding JClass. If the object is
     * not a class constant, then return null.
     */
    private @Nullable JClass toClass(CSObj csObj) {
        Object alloc = csObj.getObject().getAllocation();
        if (alloc instanceof ClassLiteral) {
            ClassLiteral klass = (ClassLiteral) alloc;
            Type type = klass.getTypeValue();
            if (type instanceof ClassType) {
                return ((ClassType) type).getJClass();
            } else if (type instanceof ArrayType) {
                return hierarchy.getJREClass(StringReps.OBJECT);
            }
        }
        return null;
    }

    /**
     * Convert a CSObj of string constant to corresponding String.
     * If the object is not a string constant, then return null.
     */
    private static @Nullable String toString(CSObj csObj) {
        Obj obj = csObj.getObject();
        if (obj instanceof ConstantObj) {
            Object alloc = obj.getAllocation();
            if (alloc instanceof String) {
                return (String) alloc;
            }
        }
        return null;
    }
}
