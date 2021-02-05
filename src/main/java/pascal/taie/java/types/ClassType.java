/*
 * Tai-e: A Program Analysis Framework for Java
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

package pascal.taie.java.types;

import pascal.taie.java.classes.JClass;
import pascal.taie.java.classes.JClassLoader;
import pascal.taie.util.HashUtils;

public class ClassType implements ReferenceType {

    private final JClassLoader loader;

    private final String name;

    private JClass jclass;

    public ClassType(JClassLoader loader, String name) {
        this.loader = loader;
        this.name = name;
    }

    @Override
    public String getName() {
        return name;
    }

    public JClass getJClass() {
        if (jclass == null) {
            jclass = loader.loadClass(name);
        }
        return jclass;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        ClassType classType = (ClassType) o;
        return loader.equals(classType.loader)
                && name.equals(classType.name);
    }

    @Override
    public int hashCode() {
        return HashUtils.hash(loader, name);
    }

    @Override
    public String toString() {
        return name;
    }
}
