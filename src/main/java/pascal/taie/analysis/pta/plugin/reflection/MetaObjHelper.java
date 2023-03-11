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

import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.MockObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.language.classes.ClassMember;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.TypeSystem;

class MetaObjHelper {

    private final static Descriptor META_DESC = () -> "ReflectionMetaObj";

    private final static Descriptor LOG_META_DESC = () -> "ReflectionLogMetaObj";

    private final ClassType clazz;

    private final ClassType constructor;

    private final ClassType method;

    private final ClassType field;

    private final HeapModel heapModel;

    MetaObjHelper(Solver solver) {
        TypeSystem typeSystem = solver.getTypeSystem();
        clazz = typeSystem.getClassType(ClassNames.CLASS);
        constructor = typeSystem.getClassType(ClassNames.CONSTRUCTOR);
        method = typeSystem.getClassType(ClassNames.METHOD);
        field = typeSystem.getClassType(ClassNames.FIELD);
        heapModel = solver.getHeapModel();
    }

    /**
     * Given a JClass or ClassMember, return the corresponding meta object.
     * @throws IllegalArgumentException if type of {@code classOrMember}
     * is neither {@link JClass} nor {@link ClassMember}.
     */
    Obj getMetaObj(Object classOrMember) {
        return getMetaObj(classOrMember, META_DESC);
    }

    Obj getLogMetaObj(Object classOrMember) {
        return getMetaObj(classOrMember, LOG_META_DESC);
    }

    boolean isLogMetaObj(CSObj obj) {
        return obj.getObject() instanceof MockObj mockObj &&
                mockObj.getDescriptor().equals(LOG_META_DESC);
    }

    private Obj getMetaObj(Object classOrMember, Descriptor desc) {
        if (classOrMember instanceof JClass jclass) {
            ClassLiteral classLiteral = ClassLiteral.get(jclass.getType());
            return desc.equals(LOG_META_DESC)
                    ? heapModel.getMockObj(desc, classLiteral, clazz)
                    : heapModel.getConstantObj(classLiteral);
        } else if (classOrMember instanceof JMethod m) {
            if (m.isConstructor()) {
                return heapModel.getMockObj(desc, classOrMember, constructor);
            } else {
                return heapModel.getMockObj(desc, classOrMember, method);
            }
        } else if (classOrMember instanceof JField) {
            return heapModel.getMockObj(desc, classOrMember, field);
        } else {
            throw new IllegalArgumentException(
                    "Expected JClass or ClassMember," + " given " + classOrMember);
        }
    }
}
