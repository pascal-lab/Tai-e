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
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.AbstractModel;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.StringReps;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.MapUtils;

import java.util.Map;

/**
 * Models side-effects reflective APIs, currently supports
 * - Class.newInstance()
 * - Constructor.newInstance()
 * - Method.invoke()
 */
class SideEffectModel extends AbstractModel {

    private final static String NEW_INSTANCE = "newInstance";

    private final static String INVOKE = "invoke";

    /**
     * Description for objects created by reflective newInstance() calls.
     */
    private final static String REF_OBJ_DESC = "ReflectiveObj";

    private final JClass klass;

    private final JClass constructor;

    private final JClass method;

    /**
     * Map from Invoke (of newInstance()) and type to reflectively-created objects.
     */
    private final Map<Invoke, Map<ClassType, MockObj>> newObjs = MapUtils.newMap();

    SideEffectModel(Solver solver) {
        super(solver);
        klass = hierarchy.getJREClass(StringReps.CLASS);
        constructor = hierarchy.getJREClass(StringReps.CONSTRUCTOR);
        method = hierarchy.getJREClass(StringReps.METHOD);
    }

    @Override
    public void handleNewInvoke(Invoke invoke) {
        MethodRef ref = invoke.getMethodRef();
        String methodName = ref.getName();
        if (ref.getDeclaringClass().equals(klass) &&
                methodName.equals(NEW_INSTANCE)) {
            // klass.newInstance()
            addRelevantBase(invoke);
        }
        if (ref.getDeclaringClass().equals(constructor) &&
                methodName.equals(NEW_INSTANCE)) {
            // constructor.newInstance(args)
            addRelevantBase(invoke);
        }
        if (ref.getDeclaringClass().equals(method) &&
                methodName.equals(INVOKE)) {
            // m.invoke(o, args)
            addRelevantBase(invoke);
            addRelevantArg(invoke, 0);
        }
    }

    @Override
    public void handleNewPointsToSet(CSVar csVar, PointsToSet pts) {
        relevantVars.get(csVar.getVar()).forEach(invoke -> {
        });
    }
}
