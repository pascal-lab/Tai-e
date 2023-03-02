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

import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.AbstractModel;
import pascal.taie.language.classes.ClassMember;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.TypeSystem;

abstract class MetaObjModel extends AbstractModel {

    /**
     * Descriptor for reflection meta objects.
     */
    private final static Descriptor META_DESC = () -> "ReflectionMetaObj";

    private final ClassType constructor;

    private final ClassType method;

    private final ClassType field;

    protected final HeapModel heapModel;

    private JClass klass;

    MetaObjModel(Solver solver) {
        super(solver);
        TypeSystem typeSystem = solver.getTypeSystem();
        constructor = typeSystem.getClassType(ClassNames.CONSTRUCTOR);
        method = typeSystem.getClassType(ClassNames.METHOD);
        field = typeSystem.getClassType(ClassNames.FIELD);
        heapModel = solver.getHeapModel();
    }

    protected JMethod get(String methodName) {
        if (klass == null) {
            klass = hierarchy.getJREClass(ClassNames.CLASS);
        }
        assert klass != null;
        return klass.getDeclaredMethod(methodName);
    }

    protected Obj getReflectionObj(ClassMember member) {
        if (member instanceof JMethod mtd) {
            if (mtd.isConstructor()) {
                return heapModel.getMockObj(META_DESC, member, constructor);
            } else {
                return heapModel.getMockObj(META_DESC, member, method);
            }
        } else {
            return heapModel.getMockObj(META_DESC, member, field);
        }
    }

    abstract void handleNewCSMethod(CSMethod csMethod);
}
