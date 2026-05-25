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

package pascal.taie.analysis.pta.plugin.android.icc;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.plugin.util.InvokeHandler;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;

import static pascal.taie.analysis.pta.plugin.util.InvokeUtils.BASE;

/**
 * Models ComponentName constructors by recording the class-name argument carried by each ComponentName object.
 */
public class ComponentNameInfoModel extends ICCHandler {

    public ComponentNameInfoModel(ICCContext context) {
        super(context);
    }

    @InvokeHandler(signature = {
            "<android.content.ComponentName: void <init>(java.lang.String,java.lang.String)>",
            "<android.content.ComponentName: void <init>(android.content.Context,java.lang.String)>",
            "<android.content.ComponentName: void <init>(android.content.Context,java.lang.Class)>"},
            argIndexes = {BASE})
    public void componentNameInit(Context context, Invoke invoke, PointsToSet componentNameObjs) {
        Var classNameArg = invoke.getInvokeExp().getArg(1);
        CSVar className = csManager.getCSVar(context, classNameArg);
        componentNameObjs.forEach(componentNameObj ->
                handlerContext.componentName2Info().put(componentNameObj, className));
    }

}
