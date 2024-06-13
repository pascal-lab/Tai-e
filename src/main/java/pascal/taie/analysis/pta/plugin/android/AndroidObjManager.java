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

package pascal.taie.analysis.pta.plugin.android;

import pascal.taie.analysis.pta.core.heap.Descriptor;
import pascal.taie.analysis.pta.core.heap.HeapModel;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.ir.exp.StringLiteral;
import pascal.taie.ir.exp.Var;
import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.Maps;

import java.util.Map;

public class AndroidObjManager {

    private static final Descriptor COMPONENT_DESC = () -> "ComponentObj";

    private static final Descriptor SHARED_PREFERENCES_DESC = () -> "SharedPreferencesObj";

    private static final Descriptor LAYOUT_NAME_DESC = () -> "LayoutNameObj";

    private static final Descriptor ANDROID_SPECIFIC_DESC = () -> "AndroidSpecificObj";

    private final HeapModel heapModel;

    private final Map<JClass, Obj> componentObj = Maps.newMap();

    private final Map<String, Obj> sharedPreferencesObj = Maps.newMap();

    private final Map<Var, Obj> androidSpecificObj = Maps.newMap();

    private final Map<StringLiteral, Obj> layoutStringObj = Maps.newMap();

    AndroidObjManager(HeapModel heapModel) {
        this.heapModel = heapModel;
    }

    private record LifecycleMethodParam(Object object, Type type, int index) {

        @Override
        public String toString() {
            return "LifecycleMethodParam{" +
                    "object=" + object +
                    ", type=" + type +
                    ", index=" + index +
                    '}';
        }
    }

    public Obj getComponentObj(JClass component) {
        return componentObj.computeIfAbsent(component,
                c -> heapModel.getMockObj(COMPONENT_DESC, c, c.getType())
        );
    }

    public Obj getSharedPreferencesObj(String fileName, Var result) {
        return sharedPreferencesObj.computeIfAbsent(fileName,
                f -> heapModel.getMockObj(
                        SHARED_PREFERENCES_DESC,
                        f,
                        result.getType(),
                        result.getMethod()
                )
        );
    }

    public Obj getAndroidStringObj(StringLiteral name, Var result) {
        return layoutStringObj.computeIfAbsent(name,
                f -> heapModel.getMockObj(
                        LAYOUT_NAME_DESC,
                        f,
                        result.getType()
                )
        );
    }

    public Obj getAndroidSpecificObj(Var var, Invoke invoke) {
        return androidSpecificObj.computeIfAbsent(var,
                v -> heapModel.getMockObj(
                        ANDROID_SPECIFIC_DESC,
                        invoke,
                        v.getType(),
                        invoke.getContainer()
                )
        );
    }

    /**
     * component lifecycle method parameter obj must be unique.
     */
    public Obj getLifecycleMethodParamObj(JClass component, Var param) {
        return getLifecycleMethodParamObj((Object) component, param);
    }

    public Obj getLifecycleMethodParamObj(JMethod lifecycleMethod, Var param) {
        return getLifecycleMethodParamObj((Object) lifecycleMethod, param);
    }

    public Obj getLifecycleMethodParamObj(Object object, Var param) {
        return androidSpecificObj.computeIfAbsent(param,
                v -> heapModel.getMockObj(
                        ANDROID_SPECIFIC_DESC,
                        new LifecycleMethodParam(object, param.getType(), param.getIndex()),
                        param.getType(),
                        param.getMethod()
                )
        );
    }

}
