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

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.Pointer;
import pascal.taie.analysis.pta.core.heap.AbstractHeapModel;
import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.MockObj;
import pascal.taie.analysis.pta.core.solver.Solver;
import pascal.taie.analysis.pta.plugin.util.AnalysisModelPlugin;
import pascal.taie.analysis.pta.plugin.util.InvokeHandler;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.util.collection.Sets;

import java.util.Set;

public class ArrayCopyOfModel extends AnalysisModelPlugin {

    private static final Descriptor COPYOF_ARRAY_DESC = () -> "ArrayGeneratedByCopyOfModel";

    ArrayCopyOfModel(Solver solver) {
        super(solver);
    }

    @InvokeHandler(signature = "<java.util.Arrays: java.lang.Object[] copyOf(java.lang.Object[],int)>", argIndexes = {0})
    public void arraysCopyOf(Context context, Invoke invoke, PointsToSet from) {
        Var result = invoke.getResult();
        if(result != null){
            Pointer to = solver.getCSManager().getCSVar(context, result);
            Set<CSObj> toObjs = Sets.newHybridSet();
            from.getObjects().forEach(csObj -> {
                // handle the argument0's obj is zero-length-array obj
                if(csObj.getObject() instanceof MockObj mockObj
                        && mockObj.getDescriptor().equals(AbstractHeapModel.ZERO_LENGTH_ARRAY_DESC)){
                    toObjs.add(solver.getCSManager().getCSObj(context,
                            heapModel.getMockObj(COPYOF_ARRAY_DESC, invoke, mockObj.getType())));
                }
                else {
                    toObjs.add(csObj);
                }
            });
            toObjs.forEach(csObj -> solver.addPointsTo(to, csObj));
        }
    }

}
