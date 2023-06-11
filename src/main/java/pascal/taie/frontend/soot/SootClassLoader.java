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

package pascal.taie.frontend.soot;

import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassLoader;
import pascal.taie.util.collection.Maps;
import soot.Scene;
import soot.SootClass;

import java.util.Collection;
import java.util.Map;

class SootClassLoader implements JClassLoader {

    private final transient Scene scene;

    private final ClassHierarchy hierarchy;

    private final boolean allowPhantom;

    private transient Converter converter;

    private final Map<String, JClass> classes = Maps.newMap(1024);

    SootClassLoader(Scene scene, ClassHierarchy hierarchy, boolean allowPhantom) {
        this.scene = scene;
        this.hierarchy = hierarchy;
        this.allowPhantom = allowPhantom;
    }

    @Override
    public JClass loadClass(String name) {
        JClass jclass = classes.get(name);
        if (jclass == null && scene != null) {
            SootClass sootClass = scene.getSootClassUnsafe(name, false);
            if (sootClass != null && (!sootClass.isPhantom() || allowPhantom)) {
                // TODO: handle phantom class more comprehensively
                jclass = new JClass(this, sootClass.getName(),
                        sootClass.moduleName);
                // New class must be put into classes map at first,
                // at build(jclass) may also trigger the loading of
                // the new created class. Not putting the class into classes
                // may cause infinite recursion.
                classes.put(name, jclass);
                new SootClassBuilder(converter, sootClass).build(jclass);
                hierarchy.addClass(jclass);
            }
        }
        // TODO: add warning for missing classes
        return jclass;
    }

    @Override
    public Collection<JClass> getLoadedClasses() {
        return classes.values();
    }

    void setConverter(Converter converter) {
        this.converter = converter;
    }
}
