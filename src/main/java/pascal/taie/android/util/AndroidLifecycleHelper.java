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

package pascal.taie.android.util;

import pascal.taie.android.config.AndroidLifecycleConfig;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Subsignature;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Resolves Android lifecycle/callback methods according to the Android
 * lifecycle configuration.
 *
 * <p>For example, given an Activity subclass, this helper finds lifecycle
 * methods such as {@code onCreate}, {@code onStart}, and {@code onResume}
 * that are actually implemented by the analyzed application class hierarchy.
 */
public class AndroidLifecycleHelper {

    /**
     * Configured lifecycle/callback methods grouped by Android component base
     * class.
     *
     * <p>
     * component base class -> configured lifecycle/callback methods
     */
    private final Map<JClass, List<JMethod>> configuredCallbacks;

    private final ClassHierarchy hierarchy;

    public AndroidLifecycleHelper(ClassHierarchy hierarchy) {
        this.configuredCallbacks = loadConfiguredCallbacks(hierarchy);
        this.hierarchy = hierarchy;
    }

    private static Map<JClass, List<JMethod>> loadConfiguredCallbacks(
            ClassHierarchy hierarchy) {
        return AndroidLifecycleConfig.loadAndroidLifecycleConfig()
                .stream()
                .collect(Collectors.toMap(
                        config -> hierarchy.getClass(config.className()),
                        config -> config.callbackMethodSubSigs()
                                .stream()
                                .map(subSig -> hierarchy.getMethod(
                                        "<" + config.className() + ": " + subSig + ">"))
                                .filter(Objects::nonNull)
                                .toList()
                ));
    }

    public boolean isLifeCycleMethod(JMethod method,
                                     String component,
                                     Subsignature methodSubSig) {
        return isComponent(component, method.getDeclaringClass())
                && method.getSubsignature().equals(methodSubSig);
    }

    public boolean isComponent(String component, JClass current) {
        return isComponent(hierarchy.getClass(component), current);
    }

    public boolean isComponent(JClass component, JClass current) {
        return hierarchy.isSubclass(component, current);
    }

    public List<JMethod> getLifeCycleMethods(JClass entryClass) {
        List<JMethod> lifecycleMethods = new ArrayList<>();
        if (!entryClass.isApplication()) {
            return lifecycleMethods;
        }

        addDefaultConstructor(entryClass, lifecycleMethods);
        addConfiguredCallbacks(entryClass, lifecycleMethods);

        return lifecycleMethods;
    }

    /**
     * Adds the no-argument constructor as a lifecycle entry if it exists.
     */
    private static void addDefaultConstructor(JClass entryClass,
                                              List<JMethod> lifecycleMethods) {
        JMethod init = entryClass.getDeclaredMethod(
                Subsignature.get(Subsignature.NO_ARG_INIT)
        );
        if (init != null) {
            lifecycleMethods.add(init);
        }
    }

    /**
     * Dispatches configured lifecycle/callback methods to {@code entryClass}
     * and keeps only application methods.
     */
    private void addConfiguredCallbacks(JClass entryClass,
                                        List<JMethod> lifecycleMethods) {
        getConfiguredCallbacksOf(entryClass).forEach(callback -> {
            JMethod dispatchedMethod =
                    hierarchy.dispatch(entryClass, callback.getRef());
            if (dispatchedMethod != null && dispatchedMethod.isApplication()) {
                lifecycleMethods.add(dispatchedMethod);
            }
        });
    }

    private Set<JMethod> getConfiguredCallbacksOf(JClass entryClass) {
        return configuredCallbacks.keySet().stream()
                .filter(component -> isComponent(component, entryClass))
                .flatMap(component -> configuredCallbacks.get(component).stream())
                .collect(Collectors.toSet());
    }

}
