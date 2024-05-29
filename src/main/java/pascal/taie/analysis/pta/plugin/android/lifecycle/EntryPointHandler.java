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
import pascal.taie.language.classes.JClass;
import pascal.taie.util.collection.Maps;

import java.util.Map;
import java.util.Set;

import static pascal.taie.android.AndroidClassNames.BUNDLE;

/**
 * Initializes android entry points for pointer analysis.
 */
public class EntryPointHandler extends LifecycleHandler {

    public EntryPointHandler(LifecycleContext context) {
        super(context);
    }

    @Override
    public void onStart() {
        Set<JClass> entryClasses = handlerContext.apkInfo().getEntrypointClasses();
        // process android lifecycle method
        for (JClass ec : entryClasses) {
            Obj thisObj = handlerContext.androidObjManager().getComponentObj(ec);
            handlerContext.lifecycleHelper().getLifeCycleMethods(ec).forEach(em -> {
                Map<Integer, Obj> paramIndex = Maps.newMap();
                for (int i = 0; i < em.getParamCount(); i++) {
                    // android.os.Bundle stores component context information
                    if (em.getParamType(i).getName().equals(BUNDLE)) {
                        paramIndex.put(i, handlerContext.androidObjManager().getLifecycleMethodParamObj(ec, em.getIR().getParam(i)));
                    }
                }
                addEntryPoint(em, thisObj, paramIndex);
            });
        }
    }

}
