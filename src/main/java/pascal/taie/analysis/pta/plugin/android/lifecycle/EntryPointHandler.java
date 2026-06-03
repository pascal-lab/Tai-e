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

import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.exp.Var;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.util.collection.Maps;

import java.util.Map;

import static pascal.taie.android.AndroidClassNames.BUNDLE;

/**
 * Initializes Android lifecycle entry points for pointer analysis.
 *
 * <p>This handler seeds lifecycle methods of manifest-declared entry-point
 * classes, e.g., enabled activities, services, broadcast receivers, content
 * providers, and the application class.
 */
public class EntryPointHandler extends LifecycleHandler {

    public EntryPointHandler(LifecycleContext context) {
        super(context);
    }

    @Override
    public void onStart() {
        handlerContext.apkInfo()
                .getEntrypointClasses()
                .forEach(this::addComponentLifecycleEntryPoints);
    }

    /**
     * Adds lifecycle methods of the given Android component as PTA entry points.
     *
     * <p>The receiver object of these lifecycle methods is the canonical
     * component object managed by {@link pascal.taie.analysis.pta.plugin.android.AndroidObjManager}.
     */
    private void addComponentLifecycleEntryPoints(JClass componentClass) {
        Obj componentObj = handlerContext.androidObjManager()
                .getComponentObj(componentClass);

        handlerContext.lifecycleHelper()
                .getLifeCycleMethods(componentClass)
                .forEach(lifecycleMethod ->
                        addLifecycleEntryPoint(
                                componentClass,
                                componentObj,
                                lifecycleMethod
                        ));
    }

    private void addLifecycleEntryPoint(JClass componentClass,
                                        Obj componentObj,
                                        JMethod lifecycleMethod) {
        addEntryPoint(
                lifecycleMethod,
                componentObj,
                getLifecycleParamObjs(componentClass, lifecycleMethod)
        );
    }

    /**
     * Creates parameter objects for lifecycle method parameters that should be
     * explicitly modeled.
     *
     * <p>Currently, {@code android.os.Bundle} parameters are modeled because
     * they may carry component state supplied by the Android framework.
     */
    private Map<Integer, Obj> getLifecycleParamObjs(JClass componentClass,
                                                    JMethod lifecycleMethod) {
        Map<Integer, Obj> paramObjsByIndex = Maps.newMap();

        for (int i = 0; i < lifecycleMethod.getParamCount(); ++i) {
            if (isBundleParam(lifecycleMethod, i)) {
                Var param = lifecycleMethod.getIR().getParam(i);
                Obj paramObj = handlerContext.androidObjManager()
                        .mockLifecycleMethodParamObj(componentClass, param);
                paramObjsByIndex.put(i, paramObj);
            }
        }

        return paramObjsByIndex;
    }

    private static boolean isBundleParam(JMethod method, int index) {
        return BUNDLE.equals(method.getParamType(index).getName());
    }
}
