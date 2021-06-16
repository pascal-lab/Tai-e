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

import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.MockObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.AbstractModel;
import pascal.taie.analysis.pta.plugin.util.CSObjUtils;
import pascal.taie.analysis.pta.plugin.util.ReflectionUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.analysis.pta.pts.PointsToSetFactory;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.InvokeVirtual;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassMember;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeManager;
import pascal.taie.language.type.VoidType;

import java.util.List;
import java.util.Map;

import static pascal.taie.util.collection.MapUtils.newMap;

/**
 * Models APIs of java.lang.Class.
 */
class ClassModel extends AbstractModel {

    private final static String GET_CONSTRUCTOR = "getConstructor";

    private final static String GET_DECLARED_CONSTRUCTOR = "getDeclaredConstructor";

    private final static String GET_METHOD = "getMethod";

    private final static String GET_DECLARED_METHOD = "getDeclaredMethod";

    private final static String GET_PRIMITIVE_CLASS = "getPrimitiveClass";

    /**
     * Description for reflection meta objects.
     */
    private final static String META_DESC = "ReflectionMetaObj";

    private final JClass klass;

    private final ClassType constructor;

    private final ClassType method;

    private final ClassType field;

    private final Map<ClassMember, MockObj> refObjs = newMap();

    ClassModel(Solver solver) {
        super(solver);
        TypeManager typeManager = solver.getTypeManager();
        klass = hierarchy.getJREClass(StringReps.CLASS);
        constructor = typeManager.getClassType(StringReps.CONSTRUCTOR);
        method = typeManager.getClassType(StringReps.METHOD);
        field = typeManager.getClassType(StringReps.FIELD);
    }

    @Override
    public void handleNewInvoke(Invoke invoke) {
        MethodRef ref = invoke.getMethodRef();
        if (ref.getDeclaringClass().equals(klass)) {
            switch (ref.getName()) {
                case GET_CONSTRUCTOR:
                case GET_DECLARED_CONSTRUCTOR: {
                    addRelevantBase(invoke);
                    break;
                }
                case GET_METHOD:
                case GET_DECLARED_METHOD: {
                    addRelevantBase(invoke);
                    addRelevantArg(invoke, 0);
                    break;
                }
                case GET_PRIMITIVE_CLASS: {
                    addRelevantArg(invoke, 0);
                    break;
                }
            }
        }
    }

    @Override
    public void handleNewPointsToSet(CSVar csVar, PointsToSet pts) {
        relevantVars.get(csVar.getVar()).forEach(invoke -> {
            switch (invoke.getMethodRef().getName()) {
                case GET_CONSTRUCTOR: {
                    handleGetConstructor(csVar, pts, invoke);
                    break;
                }
                case GET_DECLARED_CONSTRUCTOR: {
                    handleGetDeclaredConstructor(csVar, pts, invoke);
                    break;
                }
                case GET_METHOD: {
                    handleGetMethod(csVar, pts, invoke);
                    break;
                }
                case GET_DECLARED_METHOD: {
                    handleGetDeclaredMethod(csVar, pts, invoke);
                    break;
                }
                case GET_PRIMITIVE_CLASS: {
                    handleGetPrimitiveClass(csVar, pts, invoke);
                    break;
                }
            }
        });
    }

    private void handleGetConstructor(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            PointsToSet ctorObjs = PointsToSetFactory.make();
            pts.forEach(obj -> {
                JClass jclass = CSObjUtils.toClass(obj);
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
                solver.addVarPointsTo(csVar.getContext(), result, ctorObjs);
            }
        }
    }

    private void handleGetDeclaredConstructor(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            PointsToSet ctorObjs = PointsToSetFactory.make();
            pts.forEach(obj -> {
                JClass jclass = CSObjUtils.toClass(obj);
                if (jclass != null) {
                    ReflectionUtils.getDeclaredConstructors(jclass)
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

    private void handleGetMethod(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            List<PointsToSet> args = getBaseArg0(csVar, pts,
                    (InvokeVirtual) invoke.getInvokeExp());
            PointsToSet clsObjs = args.get(0);
            PointsToSet nameObjs = args.get(1);
            PointsToSet mtdObjs = PointsToSetFactory.make();
            clsObjs.forEach(clsObj -> {
                JClass cls = CSObjUtils.toClass(clsObj);
                if (cls != null) {
                    nameObjs.forEach(nameObj -> {
                        String name = CSObjUtils.toString(nameObj);
                        if (name != null) {
                            ReflectionUtils.getMethods(cls, name)
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

    private void handleGetDeclaredMethod(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            List<PointsToSet> args = getBaseArg0(csVar, pts,
                    (InvokeVirtual) invoke.getInvokeExp());
            PointsToSet clsObjs = args.get(0);
            PointsToSet nameObjs = args.get(1);
            PointsToSet mtdObjs = PointsToSetFactory.make();
            clsObjs.forEach(clsObj -> {
                JClass cls = CSObjUtils.toClass(clsObj);
                if (cls != null) {
                    nameObjs.forEach(nameObj -> {
                        String name = CSObjUtils.toString(nameObj);
                        if (name != null) {
                            ReflectionUtils.getDeclaredMethods(cls, name)
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

    private void handleGetPrimitiveClass(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            pts.forEach(nameObj -> {
                String name = CSObjUtils.toString(nameObj);
                if (name != null) {
                    Type type = name.equals("void") ?
                            VoidType.VOID : PrimitiveType.get(name);
                    solver.addVarPointsTo(csVar.getContext(), result, defaultHctx,
                            heapModel.getConstantObj(ClassLiteral.get(type)));
                }
            });
        }
    }

    private MockObj getReflectionObj(ClassMember member) {
        return refObjs.computeIfAbsent(member, mbr -> {
            if (mbr instanceof JMethod) {
                if (((JMethod) mbr).isConstructor()) {
                    return new MockObj(META_DESC, mbr, constructor);
                } else {
                    return new MockObj(META_DESC, mbr, method);
                }
            } else {
                return new MockObj(META_DESC, mbr, field);
            }
        });
    }
}
