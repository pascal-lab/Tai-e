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

import pascal.taie.analysis.pta.core.cs.element.ArrayIndex;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.solver.PointerFlowEdge;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.AbstractModel;
import pascal.taie.analysis.pta.plugin.util.CSObjs;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Sets;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

class NativeModel extends AbstractModel {

    private Set<JMethod> nativeMethods;

    NativeModel(Solver solver) {
        super(solver);
    }

    Collection<JMethod> getNativeMethods() {
        return Collections.unmodifiableSet(nativeMethods);
    }

    @Override
    protected void registerVarAndHandler() {
        nativeMethods = Sets.newSet();

        JMethod arraycopy = hierarchy.getJREMethod("<java.lang.System: void arraycopy(java.lang.Object,int,java.lang.Object,int,int)>");
        registerRelevantVarIndexes(arraycopy, 0, 2);
        registerAPIHandler(arraycopy, this::systemArrayCopy);
        nativeMethods.add(arraycopy);
    }

    private void systemArrayCopy(CSVar csVar, PointsToSet pts, Invoke invoke) {
        List<PointsToSet> args = getArgs(csVar, pts, invoke, 0, 2);
        PointsToSet srcObjs = args.get(0);
        PointsToSet destObjs = args.get(1);
        srcObjs.objects()
            .filter(CSObjs::isArray)
            .forEach(srcArray -> {
                ArrayIndex src = csManager.getArrayIndex(srcArray);
                destObjs.objects()
                    .filter(CSObjs::isArray)
                    .forEach(destArray -> {
                        ArrayIndex dest = csManager.getArrayIndex(destArray);
                        solver.addPFGEdge(src, dest, dest.getType(),
                            PointerFlowEdge.Kind.ARRAY_STORE);
                    });
            });
    }
}
