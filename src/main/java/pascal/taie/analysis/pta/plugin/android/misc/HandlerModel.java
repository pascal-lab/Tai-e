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
import pascal.taie.analysis.pta.plugin.util.InvokeHandler;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;

public class HandlerModel extends AndroidMiscHandler {

    private final JMethod runnableRun =
            hierarchy.getJREMethod("<java.lang.Runnable: void run()>");

    public HandlerModel(AndroidMiscContext context) {
        super(context);
    }

    @InvokeHandler(signature = {
            "<android.os.Handler: boolean postDelayed(java.lang.Runnable,long)>",
            "<android.os.Handler: boolean post(java.lang.Runnable)>"
    }, argIndexes = {0})
    public void handlerPostDelayed(Context context, Invoke invoke, PointsToSet runnableObjs) {
        runnableObjs.forEach(csObj -> {
            JMethod dispatch = hierarchy.dispatch(csObj.getObject().getType(), runnableRun.getRef());
            if (dispatch != null) {
                addEntryPoint(dispatch, csObj.getObject());
            }
        });
    }

}
