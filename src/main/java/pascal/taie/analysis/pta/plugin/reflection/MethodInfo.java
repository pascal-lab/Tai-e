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

package pascal.taie.analysis.pta.plugin.reflection;

import pascal.taie.ir.stmt.Invoke;
import pascal.taie.language.classes.JClass;

import javax.annotation.Nullable;
import java.util.Set;

record MethodInfo(Invoke invoke, @Nullable JClass clazz, @Nullable String name) {

    private static final Set<String> GET_METHOD =
            Set.of("getMethod", "getMethods");

    private static final Set<String> GET_DECLARED_METHOD =
            Set.of("getDeclaredMethod", "getDeclaredMethods");

    boolean isFromGetMethod() {
        return GET_METHOD.contains(invoke.getMethodRef().getName());
    }

    boolean isFromGetDeclaredMethod() {
        return GET_DECLARED_METHOD.contains(invoke.getMethodRef().getName());
    }

    boolean isClassUnknown() {
        return clazz == null;
    }

    boolean isNameUnknown() {
        return name == null;
    }
}
