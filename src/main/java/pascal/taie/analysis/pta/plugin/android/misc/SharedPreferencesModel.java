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
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.core.heap.ConstantObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.plugin.android.AndroidTransferEdge;
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

public class SharedPreferencesModel extends AndroidMiscHandler {

    public SharedPreferencesModel(AndroidMiscContext specificContext) {
        super(specificContext);
    }

    @InvokeHandler(signature = {
            "<android.content.Context: android.content.SharedPreferences getSharedPreferences(java.lang.String,int)>",
            "<android.content.ContextWrapper: android.content.SharedPreferences getSharedPreferences(java.lang.String,int)>"},
            argIndexes = {0})
    public void getSharedPreferences(Context context, Invoke invoke, PointsToSet stringObjs) {
        Var result = invoke.getResult();
        if (result == null) {
            return;
        }

        stringObjs.forEach(csObj -> {
            if (csObj.getObject() instanceof ConstantObj constantObj
                    && constantObj.getAllocation() instanceof StringLiteral stringLiteral) {
                Obj sharedPreferences = handlerContext.androidObjManager().getSharedPreferencesObj(stringLiteral.getString(), result);
                // sharedPreferences obj must be global share
                solver.addVarPointsTo(context, result, sharedPreferences);
            }
        });
    }

    @InvokeHandler(signature = "<android.content.SharedPreferences: android.content.SharedPreferences$Editor edit()>",
            argIndexes = {BASE})
    public void sharedPreferencesEdit(Context context, Invoke invoke, PointsToSet baseObjs) {
        Var result = invoke.getResult();
        if (result != null) {
            baseObjs.forEach(csObj -> solver.addVarPointsTo(context, result, csObj));
        }
    }

    @InvokeHandler(signature = "<android.content.SharedPreferences: void registerOnSharedPreferenceChangeListener(android.content.SharedPreferences$OnSharedPreferenceChangeListener)>",
            argIndexes = {BASE, 0})
    public void registerOnSharedPreferenceChangeListener(Context context,
                                                         Invoke invoke,
                                                         PointsToSet sharedPreferencesObjs,
                                                         PointsToSet listenerObjs) {
        CSVar csVar = csManager.getCSVar(context, InvokeUtils.getVar(invoke, BASE));
        sharedPreferencesObjs.forEach(sharedPreferencesObj ->
                listenerObjs.forEach(listenerObj -> {
                    if (listenerObj.getObject().getType() instanceof ClassType classType) {
                        JClass callback = classType.getJClass();
                        JMethod onSharedPreferenceChanged = callback.getDeclaredMethod("onSharedPreferenceChanged");
                        if (onSharedPreferenceChanged != null) {
                            Var param = onSharedPreferenceChanged.getIR().getParam(0);
                            handlerContext.sharedPreferences2Callback().put(sharedPreferencesObj, csManager.getCSMethod(context, onSharedPreferenceChanged));
                            solver.addPFGEdge(new AndroidTransferEdge(csVar, csManager.getCSVar(emptyContext, param)), param.getType());
                        }
                    }}
                )
        );
    }
}
