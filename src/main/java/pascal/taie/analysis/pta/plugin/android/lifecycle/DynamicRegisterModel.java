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
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.plugin.android.AndroidTransferEdge;
import pascal.taie.analysis.pta.plugin.util.InvokeHandler;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import static pascal.taie.analysis.pta.plugin.util.InvokeUtils.BASE;

public class DynamicRegisterModel extends LifecycleHandler {

    private final MultiMap<CSObj, CSVar> fragmentManager2Activity = Maps.newMultiMap();

    private final MultiMap<CSObj, CSVar> fragmentTransaction2Activity = Maps.newMultiMap();

    public DynamicRegisterModel(LifecycleContext context) {
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

                super.handlerContext.dynamicReceiver2IntentFilter().put(receiverClass, intentFilter);
                super.handlerContext.lifecycleHelper()
                        .getLifeCycleMethods(receiverClass)
                        .forEach(receiverMethod -> addEntryPoint(receiverMethod, receiverObj.getObject()));
            }
        });

    }

    @InvokeHandler(signature = {
            "<androidx.fragment.app.FragmentTransaction: androidx.fragment.app.FragmentTransaction add(int,androidx.fragment.app.Fragment)>",
            "<android.app.FragmentTransaction: android.app.FragmentTransaction add(int,android.app.Fragment)>",
            "<androidx.fragment.app.FragmentTransaction: androidx.fragment.app.FragmentTransaction replace(int,androidx.fragment.app.Fragment)>",
            "<android.app.FragmentTransaction: android.app.FragmentTransaction replace(int,android.app.Fragment)>"},
            argIndexes = {BASE, 1})
    public void dynamicRegisterFragment(Context context,
                                        Invoke invoke,
                                        PointsToSet fragmentTransactionObjs,
                                         PointsToSet fragmentObjs) {
        fragmentTransactionObjs.forEach(fragmentTransactionObj -> {
            fragmentObjs.forEach(fragmentObj -> {
                if (fragmentObj.getObject().getType() instanceof ClassType classType) {
                    JClass fragmentClass = classType.getJClass();

                    super.handlerContext.lifecycleHelper()
                            .getLifeCycleMethods(fragmentClass)
                            .forEach(fragmentMethod -> {
                                addEntryPoint(fragmentMethod, fragmentObj.getObject());
                                if (fragmentMethod.getSubsignature().equals(ON_ATTACH) && fragmentTransaction2Activity.containsKey(fragmentTransactionObj)) {
                                    CSVar paramVar = csManager.getCSVar(emptyContext, fragmentMethod.getIR().getParam(0));
                                    fragmentTransaction2Activity
                                            .get(fragmentTransactionObj)
                                            .forEach(activity ->
                                                    solver.addPFGEdge(new AndroidTransferEdge(activity, paramVar), paramVar.getType())
                                            );
                                }
                            });
                }
            });
        });

    }

    @InvokeHandler(signature = {
            "<android.app.Activity: android.app.FragmentManager getFragmentManager()>",
            "<androidx.fragment.app.FragmentActivity: androidx.fragment.app.FragmentManager getFragmentManager()>"},
            argIndexes = {BASE})
    public void getFragmentManager(Context context, Invoke invoke, PointsToSet pts) {
        CSVar csVar = csManager.getCSVar(context, InvokeUtils.getVar(invoke, BASE));
        CSObj result = generateInvokeResultObj(context, invoke);
        if (result != null) {
            fragmentManager2Activity.put(result, csVar);
        }
    }

    @InvokeHandler(signature = {
            "<android.app.FragmentManager: android.app.FragmentTransaction beginTransaction()>",
            "<androidx.fragment.app.FragmentManager: androidx.fragment.app.FragmentTransaction beginTransaction()>"},
            argIndexes = {BASE})
    public void beginTransaction(Context context, Invoke invoke, PointsToSet pts) {
        pts.forEach(csObj -> {
            if (fragmentManager2Activity.containsKey(csObj)) {
                CSObj result = generateInvokeResultObj(context, invoke);
                if (result != null) {
                    fragmentTransaction2Activity.putAll(result, fragmentManager2Activity.get(csObj));
                }
            }
        });
    }

}
