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

package pascal.taie.frontend.newfrontend.hierarchy;

import pascal.taie.frontend.newfrontend.main.NewFrontendComponent;
import pascal.taie.frontend.newfrontend.source.AsmSource;
import pascal.taie.frontend.newfrontend.FrontendContext;
import pascal.taie.frontend.newfrontend.source.ClassSource;
import pascal.taie.frontend.newfrontend.source.JavaSource;
import pascal.taie.frontend.newfrontend.java.JavaClassBuilder;
import pascal.taie.World;
import pascal.taie.frontend.newfrontend.source.PhantomClassSource;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.ClassHierarchyImpl;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassBuilder;
import pascal.taie.util.collection.Maps;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Stream;

public class DefaultCHBuilder extends NewFrontendComponent
        implements ClassHierarchyBuilder {

    public DefaultCHBuilder(FrontendContext context) {
        super(context);
    }

    @Override
    public ClassHierarchy build(Collection<ClassSource> sources) {
        ClassHierarchyImpl ch = new ClassHierarchyImpl();
        DefaultClassLoader dcl = new DefaultClassLoader(
                ctx(), ch, World.get().getOptions().isAllowPhantom());
        Map<String, JClass> m = Maps.newMap();
        dcl.setMapping(m);
        sources.forEach(i -> {
            String name = i.getClassName();
            m.put(name, new JClass(dcl, name));
        });

        ch.setDefaultClassLoader(dcl);
        ch.setBootstrapClassLoader(dcl);
        ctx().initClassloaderAndTypeSystem(dcl);

        Stream<ClassSource> classToBuild = sources.parallelStream();
        classToBuild.forEach(i -> {
            JClass klass = m.getOrDefault(i.getClassName(), null);
            if (klass == null) {
                throw new IllegalStateException();
            }
            JClassBuilder asb = getClassBuilder(i, klass);
            asb.build(klass);
            if (i instanceof AsmSource as) {
                ctx().noticeClassSource(klass, as);
            }
        });

        for (var i : m.values()) {
            if (i.getIndex() == -1) {
                ch.addClass(i);
            }
        }
        ctx().initHierarchy(ch);
        return ch;
    }

    private JClassBuilder getClassBuilder(
            ClassSource source, JClass jClass) {
        if (source instanceof AsmSource i) {
            return new BytecodeClassBuilder(ctx(), i, jClass);
        } else if (source instanceof JavaSource j) {
            return new JavaClassBuilder(j, jClass);
        } else if (source instanceof PhantomClassSource p) {
            return new PhantomClassBuilder(ctx(), p.getClassName());
        } else {
            throw new UnsupportedOperationException();
        }
    }
}
