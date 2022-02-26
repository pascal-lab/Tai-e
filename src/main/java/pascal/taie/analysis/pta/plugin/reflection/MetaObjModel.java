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

import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.heap.MockObj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.AbstractModel;
import pascal.taie.language.classes.ClassMember;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.TypeSystem;

import java.util.Map;
import java.util.Set;

import static pascal.taie.util.collection.Maps.newMap;

abstract class MetaObjModel extends AbstractModel {

    /**
     * Description for reflection meta objects.
     */
    private final static String META_DESC = "ReflectionMetaObj";

    private final ClassType constructor;

    private final ClassType method;

    private final ClassType field;

    private final Map<ClassMember, MockObj> refObjs = newMap();

    private JClass klass;

    private final Set<String> methods = Set.of(
            "getConstructor", "getDeclaredConstructor",
            "getConstructors", "getDeclaredConstructors",
            "getMethod", "getDeclaredMethod",
            "getMethods", "getDeclaredMethods",
            "getField", "getDeclaredField",
            "getFields", "getDeclaredFields"
    );

    MetaObjModel(Solver solver) {
        super(solver);
        TypeSystem typeSystem = solver.getTypeSystem();
        constructor = typeSystem.getClassType(ClassNames.CONSTRUCTOR);
        method = typeSystem.getClassType(ClassNames.METHOD);
        field = typeSystem.getClassType(ClassNames.FIELD);
    }

    protected JMethod get(String methodName) {
        if (klass == null) {
            klass = hierarchy.getJREClass(ClassNames.CLASS);
        }
        assert klass != null;
        return klass.getDeclaredMethod(methodName);
    }

    protected MockObj getReflectionObj(ClassMember member) {
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

    abstract void handleNewCSMethod(CSMethod csMethod);
}
