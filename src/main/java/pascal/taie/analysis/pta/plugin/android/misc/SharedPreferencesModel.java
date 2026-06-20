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

import pascal.taie.analysis.pta.core.cs.context.Context;
import pascal.taie.analysis.pta.core.cs.element.CSMethod;
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.ConstantObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.plugin.android.AndroidModelEdge;
import pascal.taie.analysis.pta.plugin.util.InvokeHandler;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.ClassType;

import static pascal.taie.analysis.pta.plugin.util.InvokeUtils.BASE;

/**
 * Models SharedPreferences objects, editors, and change listeners.
 */
public class SharedPreferencesModel extends AndroidMiscHandler {

    private static final String ON_SHARED_PREFERENCE_CHANGED =
            "onSharedPreferenceChanged";

    public SharedPreferencesModel(AndroidMiscContext context) {
        super(context);
    }

    @InvokeHandler(signature = {
            "<android.content.Context: android.content.SharedPreferences getSharedPreferences(java.lang.String,int)>",
            "<android.content.ContextWrapper: android.content.SharedPreferences getSharedPreferences(java.lang.String,int)>"},
            argIndexes = {0})
    public void getSharedPreferences(Context context, Invoke invoke, PointsToSet fileNameObjs) {
        Var result = invoke.getResult();
        if (result == null) {
            return;
        }

        fileNameObjs.forEach(fileName -> {
            if (fileName.getObject() instanceof ConstantObj constantObj
                    && constantObj.getAllocation() instanceof StringLiteral stringLiteral) {
                Obj sharedPreferences = handlerContext.androidObjManager()
                        .getSharedPreferencesObj(stringLiteral.getString(), result);
                // Canonicalize SharedPreferences objects by file name so later
                // reads, writes, and listener callbacks meet on the same object.
                solver.addVarPointsTo(context, result, sharedPreferences);
            }
        });
    }

    /**
     * Models SharedPreferences.edit() by reusing the same abstract object for the editor.
     *
     * <p>This keeps editor writes and SharedPreferences reads/callbacks connected in
     * {@link MapLikeHandler}, which stores key-value writes by receiver object.
     */
    @InvokeHandler(signature = "<android.content.SharedPreferences: android.content.SharedPreferences$Editor edit()>",
            argIndexes = {BASE})
    public void sharedPreferencesEdit(Context context, Invoke invoke, PointsToSet sharedPreferencesObjs) {
        Var result = invoke.getResult();
        if (result != null) {
            sharedPreferencesObjs.forEach(sharedPreferencesObj ->
                    solver.addVarPointsTo(context, result, sharedPreferencesObj));
        }
    }

    @InvokeHandler(signature = "<android.content.SharedPreferences: void registerOnSharedPreferenceChangeListener(android.content.SharedPreferences$OnSharedPreferenceChangeListener)>",
            argIndexes = {BASE, 0})
    public void registerOnSharedPreferenceChangeListener(Context context,
                                                         Invoke invoke,
                                                         PointsToSet sharedPreferencesObjs,
                                                         PointsToSet listenerObjs) {
        CSVar sharedPreferencesVar = csManager.getCSVar(context, InvokeUtils.getVar(invoke, BASE));
        listenerObjs.forEach(listenerObj ->
                registerListenerObject(context, sharedPreferencesVar, sharedPreferencesObjs, listenerObj));
    }

    private void registerListenerObject(Context registrationContext,
                                        CSVar sharedPreferencesVar,
                                        PointsToSet sharedPreferencesObjs,
                                        CSObj listenerObj) {
        JMethod callbackMethod = getOnSharedPreferenceChanged(listenerObj);
        if (callbackMethod == null) {
            return;
        }

        CSMethod csCallback = csManager.getCSMethod(registrationContext, callbackMethod);
        sharedPreferencesObjs.forEach(sharedPreferencesObj ->
                recordCallback(sharedPreferencesObj, csCallback));
        connectSharedPreferencesArg(sharedPreferencesVar, csCallback);
    }

    private JMethod getOnSharedPreferenceChanged(CSObj listenerObj) {
        if (listenerObj.getObject().getType() instanceof ClassType classType) {
            JClass listenerClass = classType.getJClass();
            return listenerClass.getDeclaredMethod(ON_SHARED_PREFERENCE_CHANGED);
        }
        return null;
    }

    /**
     * Records which callback method should be notified when a preference object is written.
     *
     * <p>{@link MapLikeHandler} later uses this mapping to propagate the changed key
     * from editor {@code put*()} calls to callback parameter 1.
     */
    private void recordCallback(CSObj sharedPreferencesObj, CSMethod csCallback) {
        handlerContext.sharedPreferences2Callback().put(sharedPreferencesObj, csCallback);
    }

    /**
     * Propagates the registered SharedPreferences receiver to callback parameter 0.
     */
    private void connectSharedPreferencesArg(CSVar sharedPreferencesVar, CSMethod csCallback) {
        Var callbackParam = csCallback.getMethod().getIR().getParam(0);
        solver.addPFGEdge(
                new AndroidModelEdge(
                        sharedPreferencesVar,
                        csManager.getCSVar(csCallback.getContext(), callbackParam)));
    }
}
