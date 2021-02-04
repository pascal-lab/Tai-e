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

import pascal.taie.java.classes.JClass;
import pascal.taie.java.classes.JClassLoader;
import soot.Scene;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class SootClassLoader implements JClassLoader {

    private final Scene scene;

    private final Map<String, JClass> classes = new HashMap<>(1024);

    public SootClassLoader(Scene scene) {
        this.scene = scene;
    }

    @Override
    public JClass loadClass(String name) {
        return null;
    }

    @Override
    public Collection<JClass> getLoadedClasses() {
        return classes.values();
    }

    private void build(JClass jclass) {
        // modifiers
        // super class
        // super interfaces
        // fields
        // methods
    }
}
