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
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.analysis.pta.plugin.android.AndroidModelEdge;
import pascal.taie.analysis.pta.plugin.util.InvokeHandler;
import pascal.taie.analysis.pta.pts.PointsToSet;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.ClassType;

import javax.annotation.Nullable;

import static pascal.taie.analysis.pta.plugin.util.InvokeUtils.BASE;

/**
 * Models Android {@code AsyncTask} execution.
 *
 * <p>For example, this model adds AsyncTask lifecycle methods as entry points
 * and propagates data from {@code execute(Object[])} to
 * {@code doInBackground(Object[])}, then from {@code doInBackground}'s return
 * values to {@code onPostExecute(Object)}.
 */
public class AsyncTaskModel extends AndroidMiscHandler {

    private static final Subsignature DO_IN_BACKGROUND_SUB_SIG =
            Subsignature.get("java.lang.Object doInBackground(java.lang.Object[])");

    private static final Subsignature ON_POST_EXECUTE_SUB_SIG =
            Subsignature.get("void onPostExecute(java.lang.Object)");

    public AsyncTaskModel(AndroidMiscContext specificContext) {
        super(specificContext);
    }

    @InvokeHandler(signature = {
            "<android.os.AsyncTask: android.os.AsyncTask execute(java.lang.Object[])>"},
            argIndexes = {BASE, 0})
    public void asyncTaskExecute(Context context, Invoke invoke,
                                 PointsToSet asyncTaskObjs,
                                 PointsToSet argObjs) {
        asyncTaskObjs.forEach(asyncTaskObj ->
                processAsyncTask(asyncTaskObj.getObject(), argObjs));
    }

    private void processAsyncTask(Obj asyncTaskObj, PointsToSet argObjs) {
        if (!(asyncTaskObj.getType() instanceof ClassType classType)) {
            return;
        }

        JMethod doInBackground = null;
        JMethod onPostExecute = null;

        for (JMethod asyncTaskMethod :
                handlerContext.lifecycleHelper().getLifeCycleMethods(classType.getJClass())) {
            addEntryPoint(asyncTaskMethod, asyncTaskObj);

            Subsignature subSig = asyncTaskMethod.getSubsignature();
            if (DO_IN_BACKGROUND_SUB_SIG.equals(subSig)) {
                doInBackground = asyncTaskMethod;
                propagateExecuteArgsToDoInBackground(doInBackground, argObjs);
            } else if (ON_POST_EXECUTE_SUB_SIG.equals(subSig)) {
                onPostExecute = asyncTaskMethod;
            }
        }

        processOnPostExecute(asyncTaskObj, doInBackground, onPostExecute);
    }


    /**
     * Propagates arguments passed to {@code execute(Object[])} to the first
     * parameter of {@code doInBackground(Object[])}.
     */
    private void propagateExecuteArgsToDoInBackground(JMethod doInBackground,
                                                      PointsToSet argObjs) {
        solver.addVarPointsTo(
                emptyContext,
                doInBackground.getIR().getParam(0),
                argObjs
        );
    }

    /**
     * Propagates return values of {@code doInBackground(Object[])} to the
     * parameter of {@code onPostExecute(Object)}.
     */
    private void processOnPostExecute(Obj asyncTaskObj,
                                      @Nullable JMethod doInBackground,
                                      @Nullable JMethod onPostExecute) {
        if (doInBackground == null || onPostExecute == null) {
            return;
        }

        CSVar postExecuteParamVar = csManager.getCSVar(
                emptyContext,
                onPostExecute.getIR().getParam(0)
        );

        doInBackground.getIR()
                .getReturnVars()
                .forEach(returnVar -> {
                    CSVar csReturnVar = csManager.getCSVar(
                            emptyContext,
                            returnVar
                    );

                    solver.addPFGEdge(
                            new AndroidModelEdge(csReturnVar, postExecuteParamVar)
                    );
                });
    }

}
