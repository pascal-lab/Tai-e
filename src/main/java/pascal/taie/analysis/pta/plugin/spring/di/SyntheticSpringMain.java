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

package pascal.taie.analysis.pta.plugin.spring.di;

import pascal.taie.frontend.java.classes.DefaultClassLoader;
import pascal.taie.frontend.java.classes.PhantomClassBuilder;
import pascal.taie.frontend.java.classes.PhantomClassSource;
import pascal.taie.language.annotation.AnnotationHolder;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassLoader;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.Modifier;
import pascal.taie.language.classes.Subsignature;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.language.type.VoidType;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Provides a synthetic container method for Spring-managed invocations.
 */
final class SyntheticSpringMain {

    private static final String CLASS_NAME =
            "pascal.taie.analysis.pta.plugin.spring.SyntheticSpring";

    private static final Subsignature MAIN_SUBSIGNATURE =
            Subsignature.get("void main(java.lang.String[])");

    private SyntheticSpringMain() {
    }

    static JMethod getOrCreate(ClassHierarchy hierarchy, TypeSystem typeSystem) {
        return hierarchy.allClasses()
                .filter(jClass -> jClass.getName().equals(CLASS_NAME))
                .map(jClass -> jClass.getDeclaredMethod(MAIN_SUBSIGNATURE))
                .filter(method -> method != null)
                .findFirst()
                .orElseGet(() -> create(hierarchy, typeSystem));
    }

    private static JMethod create(ClassHierarchy hierarchy,
                                  TypeSystem typeSystem) {
        PhantomClassSource source = new PhantomClassSource(CLASS_NAME, true);
        JClassLoader loader = new DefaultClassLoader(List.of(source));
        JClass syntheticClass = loader.loadClass(CLASS_NAME);
        JMethod main = new JMethod(
                syntheticClass,
                "main",
                EnumSet.of(Modifier.PUBLIC, Modifier.STATIC, Modifier.SYNTHETIC),
                List.of(typeSystem.getArrayType(typeSystem.stringType(), 1)),
                VoidType.VOID,
                List.of(),
                null,
                AnnotationHolder.emptyHolder(),
                List.of(AnnotationHolder.emptyHolder()),
                List.of("args"),
                null);
        syntheticClass.build(new PhantomClassBuilder(typeSystem, source) {

            @Override
            public Set<Modifier> getModifiers() {
                return EnumSet.of(Modifier.PUBLIC, Modifier.SYNTHETIC);
            }

            @Override
            public ClassType getClassType() {
                return typeSystem.getClassType(loader, CLASS_NAME);
            }

            @Override
            public Collection<JMethod> getDeclaredMethods() {
                return List.of(main);
            }
        });
        hierarchy.addClass(syntheticClass);
        return main;
    }
}
