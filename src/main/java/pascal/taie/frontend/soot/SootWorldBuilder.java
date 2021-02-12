/*
 * Tai-e - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.frontend.soot;

import pascal.taie.java.ClassHierarchy;
import pascal.taie.java.World;
import pascal.taie.java.WorldBuilder;
import pascal.taie.java.classes.ClassHierarchyImpl;
import pascal.taie.java.types.TypeManagerImpl;
import soot.Scene;

public class SootWorldBuilder implements WorldBuilder {

    @Override
    public World build() {
        World world = new World();
        ClassHierarchy hierarchy = new ClassHierarchyImpl();
        Scene scene = Scene.v();
        SootClassLoader loader = new SootClassLoader(scene, hierarchy);
        hierarchy.setDefaultClassLoader(loader);
        hierarchy.setBootstrapClassLoader(loader);
        world.setClassHierarchy(hierarchy);
        world.setTypeManager(new TypeManagerImpl(hierarchy));
        World.set(world);
        buildClasses(hierarchy, scene);
        return world;
    }

    private void buildClasses(ClassHierarchy hierarchy, Scene scene) {
        scene.getClasses().forEach(c ->
                hierarchy.getDefaultClassLoader().loadClass(c.getName()));
    }
}
