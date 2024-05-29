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

package pascal.taie.analysis.pta.plugin.android.misc;

import pascal.taie.analysis.graph.flowgraph.FlowKind;
import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.ArrayIndex;
import pascal.taie.analysis.pta.core.cs.element.CSCallSite;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.ConstantObj;
import pascal.taie.analysis.pta.plugin.util.InvokeHandler;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.IntLiteral;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.util.collection.Sets;

import java.util.Set;

import static pascal.taie.analysis.pta.plugin.util.InvokeUtils.BASE;

public class JavaMiscModel extends AndroidMiscHandler {

    private final Set<CSCallSite> subStringInvokes = Sets.newSet();

    public JavaMiscModel(AndroidMiscContext specificContext) {
        super(specificContext);
    }

    @InvokeHandler(signature = "<java.lang.Class: java.lang.String getName()>", argIndexes = {BASE})
    public void classGetName(Context context, Invoke invoke, PointsToSet classes) {
        Var result = invoke.getResult();
        if (result != null) {
            classes.forEach(csObj -> solver.addVarPointsTo(context, result, csObj));
        }
    }

    @InvokeHandler(signature = "<java.lang.String: void <init>(java.lang.String)>", argIndexes = {0})
    public void initStringWithString(Context context, Invoke invoke, PointsToSet stringObjs) {
        CSVar csVar = csManager.getCSVar(context, InvokeUtils.getVar(invoke, BASE));
        stringObjs.forEach(csObj -> solver.addPointsTo(csVar, csObj));
    }

    @InvokeHandler(signature = "<java.lang.String: char[] toCharArray()>", argIndexes = {BASE})
    public void toCharArray(Context context, Invoke invoke, PointsToSet pts) {
        CSVar csVar = csManager.getCSVar(context, InvokeUtils.getVar(invoke, BASE));
        CSObj array = generateInvokeResultObj(context, invoke);
        if (array != null) {
            ArrayIndex arrayIndex = csManager.getArrayIndex(array);
            solver.addPFGEdge(csVar, arrayIndex, FlowKind.ARRAY_STORE);
        }
    }

    @InvokeHandler(signature = "<java.lang.String: java.lang.String substring(int)>", argIndexes = {BASE})
    public void subString(Context context, Invoke invoke, PointsToSet baseObjs) {
        Var result = invoke.getResult();
        Var index = invoke.getInvokeExp().getArg(0);
        if (result == null || subStringInvokes.contains(csManager.getCSCallSite(context, invoke))) {
            return;
        }

        baseObjs.forEach(baseObj -> {
            if (baseObj.getObject() instanceof ConstantObj constantObj && constantObj.getAllocation() instanceof StringLiteral stringLiteral && index.isConst() && index.getConstValue() instanceof IntLiteral intLiteral) {
                subStringInvokes.add(csManager.getCSCallSite(context, invoke));
                try {
                    StringLiteral subString = StringLiteral.get(stringLiteral.getString().substring(intLiteral.getValue()));
                    solver.addVarPointsTo(context, result, context, solver.getHeapModel().getConstantObj(subString));
                } catch (Exception ignored) {
                }
            }
        });
    }

}
