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

import pascal.taie.frontend.java.FrontendContext;
import pascal.taie.frontend.java.main.NewFrontendComponent;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassLoader;

import java.util.Collection;
import java.util.Map;

public class DefaultClassLoader extends NewFrontendComponent
        implements JClassLoader {

    private final ClassHierarchy hierarchy;

    private final boolean allowPhantom;

    Map<String, JClass> mapping;

    private final Object phantomLock = new Object();

    DefaultClassLoader(FrontendContext context, ClassHierarchy hierarchy, boolean allowPhantom) {
        super(context);
        this.hierarchy = hierarchy;
        this.allowPhantom = allowPhantom;
    }

    @Override
    public JClass loadClass(String name) {
        return loadClass(name, allowPhantom);
    }

    @Override
    public JClass loadClass(String name, boolean allowPhantom) {
        JClass jclass = mapping.get(name);
        // Disable phantom class creating with this function
        if (jclass == null && this.allowPhantom && allowPhantom) {
            return loadPhantomClass(name);
        }

        // TODO: add warning for missing classes
        return jclass;
    }

    public JClass loadPhantomClass(String name) {
        synchronized (phantomLock) {
            JClass jclass = mapping.get(name);
            if (jclass == null) {
                // phantom class
                // what should a moduleName for a phantom class be?
                jclass = new JClass(this, name, null);
                mapping.put(name, jclass); // mapping itself is a concurrent map
                new PhantomClassBuilder(ctx(), name).build(jclass);
                // Here is the only point where hierarchy could be concurrently added
                // if there is no mutex.
                hierarchy.addClass(jclass);
            }
            return jclass;
        }
    }

    public void setMapping(Map<String, JClass> mapping) {
        this.mapping = mapping;
    }

    @Override
    public Collection<JClass> getLoadedClasses() {
        return null;
    }
}
