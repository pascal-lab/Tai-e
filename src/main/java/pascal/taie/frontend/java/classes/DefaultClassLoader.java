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

import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassLoader;
import pascal.taie.util.collection.Maps;

import java.util.Collection;
import java.util.Map;

/**
 * A basic implementation of {@link JClassLoader} that loads and manages
 * {@link JClass} objects from a given collection of {@link ClassSource}.
 * <p>
 * All classes that can be loaded by this loader originate from the
 * {@link ClassSource} collection provided at construction time;
 * no additional classes outside this collection are supported.
 */
public class DefaultClassLoader implements JClassLoader {

    private final Map<String, JClass> classes;

    /**
     * Initializes the {@link JClass} objects from provided {@link ClassSource}s.
     */
    public DefaultClassLoader(Collection<ClassSource> classSources) {
        this.classes = Maps.newMap();
        classSources.forEach(source -> {
            String name = source.getClassName();
            classes.put(name, new JClass(this, name));
        });
    }

    @Override
    public JClass loadClass(String name) {
        return loadClass(name, true);
    }

    @Override
    public JClass loadClass(String name, boolean allowPhantom) {
        // TODO: add warning for missing classes
        return classes.get(name);
    }

    @Override
    public Collection<JClass> getLoadedClasses() {
        return classes.values();
    }
}
