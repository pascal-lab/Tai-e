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

package pascal.taie.frontend.java;

import pascal.taie.World;
import pascal.taie.frontend.java.classes.AsmSource;
import pascal.taie.frontend.java.classes.BytecodeClassBuilder;
import pascal.taie.frontend.java.classes.ClassSource;
import pascal.taie.frontend.java.classes.DefaultClassLoader;
import pascal.taie.frontend.java.classes.PhantomClassBuilder;
import pascal.taie.frontend.java.classes.PhantomClassSource;
import pascal.taie.frontend.java.type.FrontendTypeSystem;
import pascal.taie.ir.IRBuilder;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.ClassHierarchyImpl;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassBuilder;
import pascal.taie.language.classes.JClassLoader;
import pascal.taie.language.type.TypeSystem;
import pascal.taie.util.collection.Maps;

import java.util.Collection;
import java.util.Map;

class FrontendBuilder {

    private final ClassHierarchy hierarchy;

    private final FrontendTypeSystem typeSystem;

    private final IRBuilder irBuilder;

    FrontendBuilder(Collection<ClassSource> classSources) {
        // create key components
        ClassHierarchyImpl hierarchy = new ClassHierarchyImpl();

        DefaultClassLoader loader = new DefaultClassLoader(hierarchy,
                World.get().getOptions().isAllowPhantom());

        hierarchy.setDefaultClassLoader(loader);
        hierarchy.setBootstrapClassLoader(loader);

        FrontendTypeSystem typeSystem = new FrontendTypeSystem(loader);
        loader.setTypeSystem(typeSystem);

        DefaultIRBuilder irBuilder = new DefaultIRBuilder(typeSystem);

        // set fields
        this.hierarchy = hierarchy;
        this.typeSystem = typeSystem;
        this.irBuilder = irBuilder;

        // build classes
        Map<String, JClass> classes = Maps.newMap();
        classSources.forEach(source -> {
            String name = source.getClassName();
            classes.put(name, new JClass(loader, name));
        });
        loader.setClasses(classes);
        classSources.parallelStream().forEach(source -> {
            JClass jclass = classes.getOrDefault(source.getClassName(), null);
            if (jclass == null) {
                throw new IllegalStateException();
            }
            getClassBuilder(loader, source, jclass).build(jclass);
            if (source instanceof AsmSource asmSource) {
                irBuilder.putClassSource(jclass, asmSource);
            }
        });
        for (JClass jclass : classes.values()) {
            if (jclass.getIndex() == -1) {
                hierarchy.addClass(jclass);
            }
        }
    }

    private JClassBuilder getClassBuilder(
            JClassLoader loader, ClassSource source, JClass jClass) {
        if (source instanceof AsmSource asmSource) {
            return new BytecodeClassBuilder(typeSystem, loader, asmSource, jClass);
        } else if (source instanceof PhantomClassSource pSource) {
            return new PhantomClassBuilder(typeSystem, pSource.getClassName());
        } else {
            throw new UnsupportedOperationException();
        }
    }

    ClassHierarchy getHierarchy() {
        return hierarchy;
    }

    TypeSystem getTypeSystem() {
        return typeSystem;
    }

    IRBuilder getIRBuilder() {
        return irBuilder;
    }
}
