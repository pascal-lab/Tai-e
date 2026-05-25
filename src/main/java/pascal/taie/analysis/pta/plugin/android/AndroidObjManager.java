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

/**
 * Manages Android-specific abstract heap objects used in PTA.
 *
 * <p>This manager creates and canonicalizes abstract objects that represent
 * Android framework/runtime entities which do not naturally correspond to
 * normal allocation sites in application code.
 */
public class AndroidObjManager {

    private static final Descriptor COMPONENT_DESC =
            () -> "ComponentObj";

    private static final Descriptor SHARED_PREFERENCES_DESC =
            () -> "SharedPreferencesObj";

    private static final Descriptor STRING_DESC =
            () -> "StringObjFromAndroid";

    private static final Descriptor ANDROID_SPECIFIC_DESC =
            () -> "AndroidSpecificObj";

    private final HeapModel heapModel;

    /**
     * Component objects are canonicalized by component class.
     *
     * <p>This allows different lifecycle callbacks of the same component to
     * share the same receiver object.
     */
    private final Map<JClass, Obj> componentObjs = Maps.newMap();

    /**
     * SharedPreferences objects are canonicalized by preference file name.
     */
    private final Map<String, Obj> sharedPreferencesObjs = Maps.newMap();

    /**
     * String resource objects are canonicalized by resolved string literal.
     *
     * <p>These objects model string values resolved from Android resources,
     * e.g., values in {@code strings.xml} returned by
     * {@code Context.getString(int)}.
     */
    private final Map<StringLiteral, Obj> stringObjs = Maps.newMap();

    /**
     * Synthetic framework objects keyed by the variable that receives them.
     *
     * <p>These objects model values produced by Android framework behavior,
     * such as lifecycle parameters and modeled invoke results.
     */
    private final Map<Var, Obj> androidSpecificObj = Maps.newMap();

    AndroidObjManager(HeapModel heapModel) {
        this.heapModel = heapModel;
    }

    /**
     * Identifier for lifecycle parameter abstractions.
     */
    private record LifecycleMethodParam(Object owner, Type type, int index) {

        @Override
        public String toString() {
            return "LifecycleMethodParam{" +
                    "owner=" + owner +
                    ", type=" + type +
                    ", index=" + index +
                    '}';
        }
    }

    public Obj getComponentObj(JClass component) {
        return componentObjs.computeIfAbsent(component,
                c -> heapModel.getMockObj(COMPONENT_DESC, c, c.getType())
        );
    }

    public Obj getSharedPreferencesObj(String fileName, Var result) {
        return sharedPreferencesObjs.computeIfAbsent(fileName,
                f -> heapModel.getMockObj(
                        SHARED_PREFERENCES_DESC,
                        f,
                        result.getType(),
                        result.getMethod()
                )
        );
    }

    /**
     * Returns an abstract object for a string value resolved from Android app.
     *
     * <p>String objects are canonicalized by their resolved string
     * literal value.
     */
    public Obj mockObjByString(StringLiteral name, Var result) {
        return stringObjs.computeIfAbsent(name,
                f -> heapModel.getMockObj(
                        STRING_DESC,
                        f,
                        result.getType()
                )
        );
    }

    public Obj mockAndroidSpecificObj(Var result, Invoke invoke) {
        return androidSpecificObj.computeIfAbsent(result,
                v -> heapModel.getMockObj(
                        ANDROID_SPECIFIC_DESC,
                        invoke,
                        v.getType(),
                        invoke.getContainer()
                )
        );
    }

    /**
     * Component lifecycle method parameter obj must be unique.
     */
    public Obj mockLifecycleMethodParamObj(JClass component, Var param) {
        return mockLifecycleMethodParamObj((Object) component, param);
    }

    public Obj mockLifecycleMethodParamObj(JMethod lifecycleMethod, Var param) {
        return mockLifecycleMethodParamObj((Object) lifecycleMethod, param);
    }


    private Obj mockLifecycleMethodParamObj(Object owner, Var param) {
        return androidSpecificObj.computeIfAbsent(param,
                p -> heapModel.getMockObj(
                        ANDROID_SPECIFIC_DESC,
                        new LifecycleMethodParam(
                                owner,
                                param.getType(),
                                param.getIndex()
                        ),
                        param.getType(),
                        param.getMethod()
                )
        );
    }

}
