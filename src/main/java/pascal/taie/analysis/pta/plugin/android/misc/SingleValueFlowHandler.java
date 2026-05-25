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
import pascal.taie.analysis.pta.core.cs.element.CSObj;
import pascal.taie.analysis.pta.core.cs.element.CSVar;
import pascal.taie.analysis.pta.plugin.android.AndroidModelEdge;
import pascal.taie.analysis.pta.plugin.util.InvokeHandler;
import pascal.taie.analysis.pta.plugin.util.InvokeUtils;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;

import static pascal.taie.analysis.pta.plugin.util.InvokeUtils.BASE;

/**
 * Models container-like APIs that conceptually store one logical value per
 * receiver object.
 *
 * <p>Examples include {@code TextView.setHint/getHint} and
 * {@code Intent.putExtras/getExtras}.
 */
public class SingleValueFlowHandler extends AndroidMiscHandler {

    /**
     * Values written to single-value container APIs, grouped by receiver
     * object.
     */
    private final MultiMap<CSObj, CSVar> writesByBase = Maps.newMultiMap();

    /**
     * Getter results that should receive values previously written to the same
     * receiver object.
     */
    private final MultiMap<CSObj, CSVar> pendingReadsByBase = Maps.newMultiMap();

    public SingleValueFlowHandler(AndroidMiscContext context) {
        super(context);
    }

    @Override
    public void onPhaseFinish() {
        resolvePendingReads();
    }

    @InvokeHandler(signature = {
            "<android.widget.TextView: void setHint(java.lang.CharSequence)>",
            "<android.content.Intent: android.content.Intent putExtras(android.os.Bundle)>"
    }, argIndexes = {BASE})
    public void writeSingleValue(Context context, Invoke invoke, PointsToSet baseObjs) {
        CSVar value = csManager.getCSVar(context, InvokeUtils.getVar(invoke, 0));
        baseObjs.forEach(baseObj -> writesByBase.put(baseObj, value));
    }

    @InvokeHandler(signature = {
            "<android.widget.TextView: java.lang.CharSequence getHint()>",
            "<android.content.Intent: android.os.Bundle getExtras()>"
    }, argIndexes = {BASE})
    public void readSingleValue(Context context, Invoke invoke, PointsToSet baseObjs) {
        Var result = invoke.getResult();
        if (result == null) {
            return;
        }
        CSVar csResult = csManager.getCSVar(context, result);
        baseObjs.forEach(baseObj -> pendingReadsByBase.put(baseObj, csResult));
    }

    private void resolvePendingReads() {
        pendingReadsByBase.forEach((baseObj, result) ->
                writesByBase.get(baseObj).forEach(value ->
                        solver.addPFGEdge(new AndroidModelEdge(value, result))));
    }
}
