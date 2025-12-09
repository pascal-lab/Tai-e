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

package pascal.taie.frontend.java.classes;

import pascal.taie.World;
import pascal.taie.frontend.java.FrontendContext;
import pascal.taie.frontend.java.main.NewFrontendComponent;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.ClassHierarchyImpl;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassBuilder;
import pascal.taie.util.collection.Maps;

import java.util.Collection;
import java.util.Map;

public class ClassHierarchyBuilder extends NewFrontendComponent {

    public ClassHierarchyBuilder(FrontendContext context) {
        super(context);
    }

    public ClassHierarchy build(Collection<ClassSource> sources) {
        ClassHierarchyImpl hierarchy = new ClassHierarchyImpl();
        DefaultClassLoader loader = new DefaultClassLoader(
                ctx(), hierarchy, World.get().getOptions().isAllowPhantom());
        Map<String, JClass> classes = Maps.newMap();
        loader.setClasses(classes);
        sources.forEach(i -> {
            String name = i.getClassName();
            classes.put(name, new JClass(loader, name));
        });

        hierarchy.setDefaultClassLoader(loader);
        hierarchy.setBootstrapClassLoader(loader);
        ctx().initClassloaderAndTypeSystem(loader);

        sources.parallelStream().forEach(source -> {
            JClass jclass = classes.getOrDefault(source.getClassName(), null);
            if (jclass == null) {
                throw new IllegalStateException();
            }
            JClassBuilder asb = getClassBuilder(source, jclass);
            asb.build(jclass);
            if (source instanceof AsmSource asmSource) {
                ctx().noticeClassSource(jclass, asmSource);
            }
        });

        for (JClass jclass : classes.values()) {
            if (jclass.getIndex() == -1) {
                hierarchy.addClass(jclass);
            }
        }
        return hierarchy;
    }

    private JClassBuilder getClassBuilder(
            ClassSource source, JClass jClass) {
        if (source instanceof AsmSource i) {
            return new BytecodeClassBuilder(ctx(), i, jClass);
        } else if (source instanceof PhantomClassSource p) {
            return new PhantomClassBuilder(ctx(), p.getClassName());
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
