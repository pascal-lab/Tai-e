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

import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.AbstractModel;
import pascal.taie.analysis.pta.plugin.util.CSObjs;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.ClassLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.PrimitiveType;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.VoidType;

/**
 * Models APIs of java.lang.Class.
 */
class ClassModel extends AbstractModel {

    ClassModel(Solver solver) {
        super(solver);
    }

    @Override
    protected void registerVarAndHandler() {
        JMethod getPrimitiveClass = hierarchy.getJREMethod("<java.lang.Class: java.lang.Class getPrimitiveClass(java.lang.String)>");
        registerRelevantVarIndexes(getPrimitiveClass, 0);
        registerAPIHandler(getPrimitiveClass, this::getPrimitiveClass);
    }

    private void getPrimitiveClass(CSVar csVar, PointsToSet pts, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            pts.forEach(nameObj -> {
                String name = CSObjs.toString(nameObj);
                if (name != null) {
                    Type type = name.equals("void") ?
                            VoidType.VOID : PrimitiveType.get(name);
                    solver.addVarPointsTo(csVar.getContext(), result,
                            heapModel.getConstantObj(ClassLiteral.get(type)));
                }
            });
        }
    }
}
