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
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.plugin.android.AndroidModelEdge;
import pascal.taie.analysis.pta.plugin.android.lifecycle.LayoutResourceResolver.LayoutLookup;
import pascal.taie.analysis.pta.plugin.util.InvokeHandler;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.ClassType;

import java.util.Set;
import java.util.stream.Stream;

import static pascal.taie.analysis.pta.plugin.util.InvokeUtils.BASE;
import static pascal.taie.analysis.pta.plugin.util.InvokeUtils.RESULT;

/**
 * Models layout-related Android framework APIs.
 *
 * <p>For example, this model handles resource strings returned by
 * {@code Context.getString(int)}, callbacks declared in layout files,
 * and fragments/views introduced by layout inflation.
 */
public class LayoutModel extends LifecycleHandler {

    private final LayoutResourceResolver resourceResolver;

    public LayoutModel(LifecycleContext context) {
        super(context);
        this.resourceResolver = new LayoutResourceResolver(context.apkInfo(), hierarchy);
    }

    @InvokeHandler(signature = {
            "<android.app.FragmentManager: android.app.Fragment findFragmentById(int)>",
            "<android.app.Activity: android.view.View findViewById(int)>"
    }, argIndexes = {BASE})
    public void findById(Context context, Invoke invoke, PointsToSet baseObj) {
        Var result = InvokeUtils.getVar(invoke, RESULT);
        if (result == null) {
            return;
        }

        LayoutLookup lookup = resourceResolver.resolveComponentLookup(
                resourceResolver.getIntegerArgument(invoke, 0));
        if (lookup.requiresSyntheticResult()) {
            addResultObjectForInvoke(context, invoke);
        }
        if (lookup.componentClass() != null) {
            Obj componentObj = handlerContext.androidObjManager().getComponentObj(lookup.componentClass());
            solver.addVarPointsTo(context, result, componentObj);
        }
    }

    @InvokeHandler(signature = "<android.app.Activity: void setContentView(int)>", argIndexes = {BASE})
    public void setContentView(Context context, Invoke invoke, PointsToSet pts) {
        Var activity = InvokeUtils.getVar(invoke, BASE);
        if (activity == null || !(activity.getType() instanceof ClassType classType)) {
            return;
        }

        JClass activityClass = classType.getJClass();
        CSVar csActivity = csManager.getCSVar(context, activity);

        resolveLayoutFileNames(invoke).forEach(layoutFileName -> {
            addLayoutCallback(activityClass, pts, layoutFileName);
            addLayoutComponent(csActivity, layoutFileName);
        });
    }

    @InvokeHandler(signature = "<android.content.Context: java.lang.String getString(int)>", argIndexes = {BASE})
    public void getString(Context context, Invoke invoke, PointsToSet pts) {
        Var result = InvokeUtils.getVar(invoke, RESULT);
        if (result == null) {
            return;
        }
        String value = resourceResolver.getStringResource(
                resourceResolver.getIntegerArgument(invoke, 0));
        if (value != null) {
            Obj stringObj = handlerContext.androidObjManager()
                    .mockObjByString(StringLiteral.get(value), result);
            solver.addVarPointsTo(context, result, stringObj);
        }
    }

    private Set<String> resolveLayoutFileNames(Invoke invoke) {
        // If the concrete layout cannot be resolved, conservatively process
        // all known layout files.
        return resourceResolver.resolveLayoutFileNames(
                resourceResolver.getIntegerArgument(invoke, 0));
    }

    /**
     * Adds layout-declared callback methods, e.g., methods referenced by
     * {@code android:onClick}, as entry points of the hosting activity.
     */
    private void addLayoutCallback(JClass activityClass,
                                   PointsToSet activityObjs,
                                   String layoutFileName) {
        handlerContext.apkInfo()
                .layoutCallbacks()
                .get(layoutFileName)
                .forEach(callbackSubSig -> {
                    JMethod callbackMethod =
                            resolveCallbackMethod(activityClass, callbackSubSig);
                    if (callbackMethod != null) {
                        activityObjs.forEach(thisObj ->
                                addEntryPoint(callbackMethod, thisObj.getObject()));
                    }
                });
    }

    /**
     * Resolves a layout callback method from the activity class hierarchy.
     *
     * <p>Layout callbacks may be declared in a superclass of the concrete
     * activity class.
     */
    private JMethod resolveCallbackMethod(JClass activityClass, Subsignature callbackSubSig) {
        JClass cur = activityClass;

        while (cur != null) {
            JMethod method = cur.getDeclaredMethod(callbackSubSig);
            if (method != null) {
                return method;
            }

            cur = cur.getSuperClass();
        }

        return null;
    }

    /**
     * Adds lifecycle entry points for fragments/views declared in the given
     * layout file.
     */
    private void addLayoutComponent(CSVar activity, String layoutFileName) {
        Stream.concat(
                handlerContext.apkInfo().layoutFragments().entrySet().stream(),
                handlerContext.apkInfo().layoutViews().entrySet().stream())
                .filter(entry -> layoutFileName.equals(entry.getKey()))
                .forEach(map -> {
                    JClass componentClass = map.getValue();
                    Obj componentObj = handlerContext.androidObjManager().getComponentObj(componentClass);
                    handlerContext.lifecycleHelper()
                            .getLifeCycleMethods(componentClass)
                            .forEach(lifecycleMethod -> {
                                addEntryPoint(lifecycleMethod, componentObj);
                                if (lifecycleMethod.getSubsignature().equals(ON_ATTACH)) {
                                    CSVar paramVar = csManager.getCSVar(
                                            emptyContext,
                                            lifecycleMethod.getIR().getParam(0));
                                    solver.addPFGEdge(
                                            new AndroidModelEdge(activity, paramVar));
                                }
                            });
                });
    }

}
