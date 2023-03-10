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
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.AbstractModel;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

abstract class MetaObjModel extends AbstractModel {

    protected final MetaObjHelper helper;

    private JClass klass;

    MetaObjModel(Solver solver) {
        super(solver);
        helper = new MetaObjHelper(solver);
    }

    protected JMethod get(String methodName) {
        if (klass == null) {
            klass = hierarchy.getJREClass(ClassNames.CLASS);
        }
        assert klass != null;
        return klass.getDeclaredMethod(methodName);
    }

    protected Obj getMetaObj(Object classOrMember) {
        return helper.getMetaObj(classOrMember);
    }

    void handleNewCSMethod(CSMethod csMethod) {
    }
}
