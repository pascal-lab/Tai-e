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

package pascal.taie.analysis.pta.plugin.android.lifecycle;

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.plugin.util.InvokeHandler;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.type.ClassType;

/**
 * Models Android APIs that dynamically register broadcast receivers.
 *
 * <p>This model records dynamically-added intent filters and installs the
 * receiver lifecycle entry points.
 */
public class DynamicReceiverModel extends LifecycleHandler {

    public DynamicReceiverModel(LifecycleContext context) {
        super(context);
    }

    @InvokeHandler(signature = {
            "<android.content.Context: android.content.Intent registerReceiver(android.content.BroadcastReceiver,android.content.IntentFilter)>",
            "<android.content.ContextWrapper: android.content.Intent registerReceiver(android.content.BroadcastReceiver,android.content.IntentFilter)>"},
            argIndexes = {0})
    public void dynamicRegisterReceiver(Context context, Invoke invoke, PointsToSet receiverObjs) {
        InvokeExp invokeExp = invoke.getInvokeExp();
        CSVar intentFilter = csManager.getCSVar(context, invokeExp.getArg(1));
        receiverObjs.forEach(receiverObj -> {
            if (receiverObj.getObject().getType() instanceof ClassType classType) {
                JClass receiverClass = classType.getJClass();

                handlerContext.intentFiltersByDynamicReceiver().put(receiverClass, intentFilter);
                handlerContext.lifecycleHelper()
                        .getLifeCycleMethods(receiverClass)
                        .forEach(receiverMethod -> addEntryPoint(receiverMethod, receiverObj.getObject()));
            }
        });
    }
}
