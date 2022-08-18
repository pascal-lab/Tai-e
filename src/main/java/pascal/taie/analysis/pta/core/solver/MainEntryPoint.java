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

package pascal.taie.analysis.pta.core.solver;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.ArrayIndex;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.TypeSystem;

import java.util.Set;

class MainEntryPoint extends EntryPoint {

    private final Solver solver;

    MainEntryPoint(JMethod method, Solver solver) {
        super(method);
        this.solver = solver;
    }

    @Override
    public Set<Obj> getThis() {
        return Set.of();
    }

    @Override
    public Set<Obj> getParam(int i) {
        assert i == 0; // main method has only one parameter
        return Set.of(getMainArgs());
    }

    private Obj getMainArgs() {
        HeapModel heapModel = solver.getHeapModel();
        TypeSystem typeSystem = solver.getTypeSystem();
        ClassType string = typeSystem.getClassType(ClassNames.STRING);
        ArrayType stringArray = typeSystem.getArrayType(string, 1);
        Obj args = heapModel.getMockObj(
                "<main-arguments>", this, stringArray, method);
        // set up element in main args
        Context ctx = solver.getContextSelector().getEmptyContext();
        CSObj csArgs = solver.getCSManager().getCSObj(ctx, args);
        ArrayIndex argsIndex = solver.getCSManager().getArrayIndex(csArgs);
        Obj argsElem = heapModel.getMockObj(
                "<main-arguments-element>", this, string, method);
        solver.addPointsTo(argsIndex, ctx, argsElem);
        return args;
    }
}
