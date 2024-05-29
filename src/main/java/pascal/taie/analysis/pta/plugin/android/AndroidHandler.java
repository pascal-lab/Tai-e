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

package pascal.taie.analysis.pta.plugin.android;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.core.solver.DeclaredParamProvider;
import pascal.taie.analysis.pta.core.solver.EntryPoint;
import pascal.taie.analysis.pta.core.solver.SpecifiedParamProvider;
import pascal.taie.analysis.pta.plugin.util.AnalysisModelPlugin;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;

import java.util.Map;

/**
 * Abstract class for android handlers.
 */
public abstract class AndroidHandler extends AnalysisModelPlugin {

    protected final AndroidContext handlerContext;

    protected AndroidHandler(AndroidContext context) {
        super(context.solver());
        this.handlerContext = context;
    }

    protected void addEntryPoint(JMethod entry, Obj thisObj) {
        addEntryPoint(entry, thisObj, null);
    }

    protected void addEntryPoint(JMethod entry, Obj thisObj, Map<Integer, Obj> paramIndex) {
        SpecifiedParamProvider.Builder builder =
                new SpecifiedParamProvider.Builder(entry);
        builder.addThisObj(thisObj)
                .setDelegate(new DeclaredParamProvider(entry, solver.getHeapModel()));
        if (paramIndex != null) {
            paramIndex.forEach(builder::addParamObj);
        }
        solver.addEntryPoint(new EntryPoint(entry, builder.build()));
    }

    protected CSObj generateInvokeResultObj(Context context, Invoke invoke) {
        Var result = invoke.getResult();
        if (result != null) {
            CSObj resultCSObj = csManager.getCSObj(context, handlerContext.androidObjManager()
                    .getAndroidSpecificObj(result, invoke));
            solver.addVarPointsTo(context, result, resultCSObj);
            return resultCSObj;
        }
        return null;
    }

}
