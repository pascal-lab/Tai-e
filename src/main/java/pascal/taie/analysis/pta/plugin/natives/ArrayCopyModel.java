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

package pascal.taie.analysis.pta.plugin.natives;

import pascal.taie.analysis.pta.core.cs.element.CSManager;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.CSObjs;
import pascal.taie.analysis.pta.plugin.util.Model;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.ClassNames;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.Maps;

import java.util.Map;

class ArrayCopyModel implements Model {

    private final Solver solver;

    private final CSManager csManager;

    private final JMethod arraycopy;

    private final ClassType object;

    private final Map<Invoke, Var> tempVars = Maps.newMap();

    private final Map<Var, Var> srcVars = Maps.newMap();

    private final Map<Var, Var> destVars = Maps.newMap();

    ArrayCopyModel(Solver solver) {
        this.solver = solver;
        csManager = solver.getCSManager();
        ClassHierarchy hierarchy = solver.getHierarchy();
        arraycopy = hierarchy.getJREMethod("<java.lang.System: void arraycopy(java.lang.Object,int,java.lang.Object,int,int)>");
        //noinspection ConstantConditions
        object = hierarchy.getJREClass(ClassNames.OBJECT).getType();
    }

    JMethod getArraycopy() {
        return arraycopy;
    }

    @Override
    public void handleNewInvoke(Invoke invoke) {
        JMethod target = invoke.getMethodRef().resolveNullable();
        if (arraycopy.equals(target)) {
            Var src = invoke.getInvokeExp().getArg(0);
            Var dest = invoke.getInvokeExp().getArg(2);
            Var temp = getTempVar(invoke);
            srcVars.put(src, temp);
            destVars.put(dest, temp);
        }
    }

    private Var getTempVar(Invoke invoke) {
        String name = "%native-arraycopy-temp" + tempVars.size();
        return tempVars.computeIfAbsent(invoke,
            i -> new Var(i.getContainer(), name, object, -1));
    }

    @Override
    public boolean isRelevantVar(Var var) {
        return srcVars.containsKey(var)
            || destVars.containsKey(var);
    }

    @Override
    public void handleNewPointsToSet(CSVar csVar, PointsToSet pts) {
        Var temp;
        if ((temp = srcVars.get(csVar.getVar())) != null) {
            CSVar csTemp = csManager.getCSVar(csVar.getContext(), temp);
            pts.objects()
                .filter(CSObjs::isArray)
                .map(csManager::getArrayIndex)
                .forEach(srcIndex -> solver.addPFGEdge(srcIndex, csTemp,
                    PointerFlowEdge.Kind.ARRAY_LOAD));
        }
        if ((temp = destVars.get(csVar.getVar())) != null) {
            CSVar csTemp = csManager.getCSVar(csVar.getContext(), temp);
            pts.objects()
                .filter(CSObjs::isArray)
                .map(csManager::getArrayIndex)
                .forEach(destIndex -> solver.addPFGEdge(csTemp, destIndex,
                    PointerFlowEdge.Kind.ARRAY_STORE, destIndex.getType()));
        }
    }
}
