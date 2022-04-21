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

package pascal.taie.analysis.pta.plugin.util;

import pascal.taie.language.classes.ClassMember;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Static utility methods for reflection analysis.
 */
public final class Reflections {

    private Reflections() {
    }

    public static Stream<JMethod> getDeclaredConstructors(JClass jclass) {
        return jclass.getDeclaredMethods()
                .stream()
                .filter(JMethod::isConstructor);
    }

    public static Stream<JMethod> getConstructors(JClass jclass) {
        return getDeclaredConstructors(jclass).filter(ClassMember::isPublic);
    }

    public static Stream<JMethod> getDeclaredMethods(JClass jclass, String methodName) {
        return jclass.getDeclaredMethods()
                .stream()
                .filter(m -> m.getName().equals(methodName) &&
                        !m.isConstructor());
    }

    public static Stream<JMethod> getMethods(JClass jclass, String methodName) {
        List<JMethod> methods = new ArrayList<>();
        while (jclass != null) {
            jclass.getDeclaredMethods()
                    .stream()
                    .filter(m -> m.getName().equals(methodName) &&
                            m.isPublic() && !m.isConstructor())
                    .forEach(m -> {
                        if (methods.stream().noneMatch(mtd ->
                                mtd.getSubsignature()
                                        .equals(m.getSubsignature()))) {
                            methods.add(m);
                        }
                    });
            jclass = jclass.getSuperClass();
        }
        return methods.stream();
    }
}
