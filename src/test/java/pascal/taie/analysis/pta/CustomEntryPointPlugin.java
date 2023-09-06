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

package pascal.taie.analysis.pta;

import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.DeclaredParamProvider;
import pascal.taie.analysis.pta.core.solver.EmptyParamProvider;
import pascal.taie.analysis.pta.core.solver.EntryPoint;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.core.solver.SpecifiedParamProvider;
import pascal.taie.analysis.pta.plugin.Plugin;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JField;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.TypeSystem;

public class CustomEntryPointPlugin implements Plugin {

    private Solver solver;

    private ClassHierarchy hierarchy;

    private TypeSystem typeSystem;

    private HeapModel heapModel;

    @Override
    public void setSolver(Solver solver) {
        this.solver = solver;
        this.hierarchy = solver.getHierarchy();
        this.typeSystem = solver.getTypeSystem();
        this.heapModel = solver.getHeapModel();
    }

    @Override
    public void onStart() {
        JClass clz = hierarchy.getClass("CustomEntryPoints");
        assert clz != null;

        JMethod emptyParam = clz.getDeclaredMethod("entryWithEmptyParam");
        assert emptyParam != null;
        solver.addEntryPoint(new EntryPoint(emptyParam, EmptyParamProvider.get()));

        JMethod declaredParam1 = clz.getDeclaredMethod("entryWithDeclaredParam1");
        assert declaredParam1 != null;
        solver.addEntryPoint(new EntryPoint(
                declaredParam1, new DeclaredParamProvider(declaredParam1, heapModel, 1)));

        JMethod declaredParam2 = clz.getDeclaredMethod("entryWithDeclaredParam2");
        assert declaredParam2 != null;
        solver.addEntryPoint(new EntryPoint(
                declaredParam2, new DeclaredParamProvider(declaredParam2, heapModel, 2)));

        JMethod specifiedParam = clz.getDeclaredMethod("entryWithSpecifiedParam");
        assert specifiedParam != null;
        SpecifiedParamProvider.Builder paramProviderBuilder =
                new SpecifiedParamProvider.Builder(specifiedParam);
        Obj thisObj = heapModel.getMockObj(Descriptor.ENTRY_DESC, "MethodParam{this}",
                clz.getType(), specifiedParam);
        Obj p0 = heapModel.getMockObj(Descriptor.ENTRY_DESC, "MethodParam{0}",
                specifiedParam.getParamType(0), specifiedParam);
        Obj p1 = heapModel.getMockObj(Descriptor.ENTRY_DESC, "MethodParam{1}",
                specifiedParam.getParamType(1), specifiedParam);
        Obj stringObj = heapModel.getMockObj(Descriptor.ENTRY_DESC, "MethodParam{0}.s1",
                typeSystem.getType(ClassNames.STRING), specifiedParam);
        Obj param1Obj = heapModel.getMockObj(Descriptor.ENTRY_DESC, "MethodParam{1}[*]",
                typeSystem.getType("Param1"), specifiedParam);
        JField s1Field = hierarchy.getField("<Param1: java.lang.String s1>");
        paramProviderBuilder.addThisObj(thisObj)
                .addParamObj(0, p0)
                .addFieldObj(p0, s1Field, stringObj)
                .addParamObj(1, p1)
                .addArrayObj(p1, param1Obj)
                .setDelegate(new DeclaredParamProvider(specifiedParam, heapModel));
        solver.addEntryPoint(new EntryPoint(specifiedParam, paramProviderBuilder.build()));
    }
}
