/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.frontend.soot;

import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JClassLoader;
import soot.Scene;
import soot.SootClass;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class SootClassLoader implements JClassLoader {

    private final Scene scene;

    private final ClassHierarchy hierarchy;

    private Converter converter;

    private final Map<String, JClass> classes = new HashMap<>(1024);

    SootClassLoader(Scene scene, ClassHierarchy hierarchy) {
        this.scene = scene;
        this.hierarchy = hierarchy;
    }

    @Override
    public JClass loadClass(String name) {
        JClass jclass = classes.get(name);
        if (jclass == null) {
            // TODO: confirm if this API is suitable
            SootClass sootClass = scene.getSootClassUnsafe(name, false);
            if (sootClass != null) {
                jclass = new JClass(this, sootClass.getName(),
                        sootClass.moduleName);
                // New class must be put into classes map at first,
                // at build(jclass) may also trigger the loading of
                // the new created class. Not putting the class into classes
                // may cause infinite recursion.
                classes.put(name, jclass);
                new SootClassBuilder(converter, sootClass)
                        .build(jclass);
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
