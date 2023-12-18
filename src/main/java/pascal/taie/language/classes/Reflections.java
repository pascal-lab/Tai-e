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

package pascal.taie.language.classes;

import pascal.taie.util.collection.Sets;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Static utility methods for modeling the behaviors of reflection APIs.
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
                .filter(m -> m.getName().equals(methodName));
    }

    public static Stream<JMethod> getDeclaredMethods(JClass jclass) {
        return jclass.getDeclaredMethods()
                .stream()
                .filter(m -> !m.isConstructor() && !m.isStaticInitializer());
    }

    public static Stream<JMethod> getMethods(JClass jclass, String methodName) {
        List<JMethod> methods = new ArrayList<>();
        Set<Subsignature> subSignatures = Sets.newHybridSet();
        while (jclass != null) {
            jclass.getDeclaredMethods()
                    .stream()
                    .filter(m -> m.isPublic() && m.getName().equals(methodName))
                    .filter(m -> !subSignatures.contains(m.getSubsignature()))
                    .forEach(m -> {
                        methods.add(m);
                        subSignatures.add(m.getSubsignature());
                    });
            jclass = jclass.getSuperClass();
        }
        return methods.stream();
    }

    public static Stream<JMethod> getMethods(JClass jclass) {
        List<JMethod> methods = new ArrayList<>();
        Set<Subsignature> subSignatures = Sets.newHybridSet();
        while (jclass != null) {
            jclass.getDeclaredMethods()
                    .stream()
                    .filter(JMethod::isPublic)
                    .filter(m -> !m.isConstructor() && !m.isStaticInitializer())
                    .filter(m -> !subSignatures.contains(m.getSubsignature()))
                    .forEach(m -> {
                        methods.add(m);
                        subSignatures.add(m.getSubsignature());
                    });
            jclass = jclass.getSuperClass();
        }
        return methods.stream();
    }
}
