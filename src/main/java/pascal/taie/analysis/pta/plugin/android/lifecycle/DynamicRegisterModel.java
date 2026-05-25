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
import pascal.taie.analysis.pta.plugin.android.AndroidModelEdge;
import pascal.taie.analysis.pta.plugin.util.InvokeHandler;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.InvokeExp;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import static pascal.taie.analysis.pta.plugin.util.InvokeUtils.BASE;

/**
 * Models Android APIs that dynamically register lifecycle-related objects.
 *
 * <p>This model currently handles:
 *
 * <ul>
 *     <li>dynamic broadcast receiver registration via {@code registerReceiver}</li>
 *     <li>dynamic fragment registration via {@code FragmentTransaction}</li>
 *     <li>activity propagation to fragment {@code onAttach(Activity)}</li>
 * </ul>
 */
public class DynamicRegisterModel extends LifecycleHandler {

    /**
     * Records which activity variables produce a fragment manager object.
     *
     * <p>
     * FragmentManager object -> activity variables
     */
    private final MultiMap<CSObj, CSVar> activitiesByFragmentManager = Maps.newMultiMap();

    /**
     * Records which activity variables are associated with a fragment
     * transaction object.
     *
     * <p>
     * FragmentTransaction object -> activity variables
     */
    private final MultiMap<CSObj, CSVar> activitiesByFragmentTransaction = Maps.newMultiMap();

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

                handlerContext.intentFiltersByDynamicReceiver().put(receiverClass, intentFilter);
                handlerContext.lifecycleHelper()
                        .getLifeCycleMethods(receiverClass)
                        .forEach(receiverMethod -> addEntryPoint(receiverMethod, receiverObj.getObject()));
            }
        });

    }

    @InvokeHandler(signature = {
            "<android.support.v4.app.FragmentTransaction: android.support.v4.app.FragmentTransaction add(int,android.support.v4.app.Fragment)>",
            "<android.app.FragmentTransaction: android.app.FragmentTransaction add(int,android.app.Fragment)>",
            "<androidx.fragment.app.FragmentTransaction: androidx.fragment.app.FragmentTransaction add(int,androidx.fragment.app.Fragment)>",
            "<android.support.v4.app.FragmentTransaction: android.support.v4.app.FragmentTransaction replace(int,android.support.v4.app.Fragment)>",
            "<android.app.FragmentTransaction: android.app.FragmentTransaction replace(int,android.app.Fragment)>",
            "<androidx.fragment.app.FragmentTransaction: androidx.fragment.app.FragmentTransaction replace(int,androidx.fragment.app.Fragment)>"},
            argIndexes = {BASE, 1})
    public void dynamicRegisterFragment(Context context,
                                        Invoke invoke,
                                        PointsToSet fragmentTransactionObjs,
                                         PointsToSet fragmentObjs) {
        registerFragments(fragmentTransactionObjs, fragmentObjs);
    }

    @InvokeHandler(signature = {
            "<android.support.v4.app.FragmentTransaction: android.support.v4.app.FragmentTransaction add(android.support.v4.app.Fragment,java.lang.String)>",
            "<android.app.FragmentTransaction: android.app.FragmentTransaction add(android.app.Fragment,java.lang.String)>",
            "<androidx.fragment.app.FragmentTransaction: androidx.fragment.app.FragmentTransaction add(androidx.fragment.app.Fragment,java.lang.String)>"},
            argIndexes = {BASE, 0})
    public void dynamicRegisterFragmentByTag(Context context,
                                        Invoke invoke,
                                        PointsToSet fragmentTransactionObjs,
                                        PointsToSet fragmentObjs) {
        registerFragments(fragmentTransactionObjs, fragmentObjs);
    }

    @InvokeHandler(signature = {
            "<android.support.v4.app.DialogFragment: int show(android.support.v4.app.FragmentTransaction,java.lang.String)>"},
            argIndexes = {BASE, 0})
    public void showDialogFragment(Context context,
                                         Invoke invoke,
                                         PointsToSet fragmentObjs,
                                         PointsToSet fragmentTransactionObjs) {
        registerFragments(fragmentTransactionObjs, fragmentObjs);
    }

    @InvokeHandler(signature = {
            "<android.support.v4.app.FragmentActivity: android.support.v4.app.FragmentManager getSupportFragmentManager()>",
            "<android.app.Activity: android.app.FragmentManager getFragmentManager()>",
            "<androidx.fragment.app.FragmentActivity: androidx.fragment.app.FragmentManager getFragmentManager()>"},
            argIndexes = {BASE})
    public void getFragmentManager(Context context, Invoke invoke, PointsToSet pts) {
        CSVar csVar = csManager.getCSVar(context, InvokeUtils.getVar(invoke, BASE));
        CSObj result = addResultObjectForInvoke(context, invoke);
        if (result != null) {
            activitiesByFragmentManager.put(result, csVar);
        }
    }

    @InvokeHandler(signature = {
            "<android.support.v4.app.FragmentManager: android.support.v4.app.FragmentTransaction beginTransaction()>",
            "<android.app.FragmentManager: android.app.FragmentTransaction beginTransaction()>",
            "<androidx.fragment.app.FragmentManager: androidx.fragment.app.FragmentTransaction beginTransaction()>"},
            argIndexes = {BASE})
    public void beginTransaction(Context context, Invoke invoke, PointsToSet managerObjs) {
        managerObjs.forEach(fragmentManager -> {
            if (activitiesByFragmentManager.containsKey(fragmentManager)) {
                CSObj fragmentTransaction = addResultObjectForInvoke(context, invoke);
                if (fragmentTransaction != null) {
                    activitiesByFragmentTransaction.putAll(fragmentTransaction, activitiesByFragmentManager.get(fragmentManager));
                }
            }
        });
    }

    private void registerFragments(PointsToSet fragmentTransactionObjs, PointsToSet fragmentObjs) {
        fragmentTransactionObjs.forEach(fragmentTransaction -> {
            fragmentObjs.forEach(fragment -> {
                if (fragment.getObject().getType() instanceof ClassType classType) {
                    JClass fragmentClass = classType.getJClass();

                    handlerContext.lifecycleHelper()
                            .getLifeCycleMethods(fragmentClass)
                            .forEach(fragmentMethod -> {
                                addEntryPoint(fragmentMethod, fragment.getObject());
                                propagateActivityToOnAttach(
                                        fragmentTransaction,
                                        fragmentMethod
                                );
                            });
                }
            });
        });
    }

    /**
     * Propagates the activity associated with a FragmentTransaction to the
     * first parameter of {@code Fragment.onAttach(Activity)}.
     *
     * <p>This models the framework behavior that the hosting activity is
     * passed to fragments when they are attached.
     */
    private void propagateActivityToOnAttach(CSObj fragmentTransaction,
                                             JMethod fragmentMethod) {
        if (!fragmentMethod.getSubsignature().equals(ON_ATTACH)
        || !activitiesByFragmentTransaction.containsKey(fragmentTransaction)) {
            return;
        }

        CSVar activityParam = csManager.getCSVar(
                emptyContext,
                fragmentMethod.getIR().getParam(0)
        );

        activitiesByFragmentTransaction
                .get(fragmentTransaction)
                .forEach(activity ->
                        solver.addPFGEdge(
                                new AndroidModelEdge(activity, activityParam),
                                activityParam.getType()
                        ));
    }

}
