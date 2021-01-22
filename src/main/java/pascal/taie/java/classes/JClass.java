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

package pascal.taie.java.classes;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class JClass {

    private final JClassLoader loader;
    private final String name;
    private final Set<Modifier> modifiers;
    private final String moduleName;
    private JClass superClass;
    private Collection<JClass> superInterfaces;
    private JClass outerClass;
    private Collection<JClass> innerClasses;
    private List<JMethod> declaredMethods;
    private List<JField> declaredFields;

    public JClass(JClassLoader loader, String name,
                  Set<Modifier> modifiers, String moduleName) {
        this.loader = loader;
        this.name = name;
        this.modifiers = modifiers;
        this.moduleName = moduleName;
    }

    public String getName() {
        return name;
    }

    public boolean isPublic() {
        return Modifier.hasPublic(modifiers);
    }

    public boolean isProtected() {
        return Modifier.hasProtected(modifiers);
    }

    public boolean isPrivate() {
        return Modifier.hasPrivate(modifiers);
    }

    public boolean isInterface() {
        return Modifier.hasInterface(modifiers);
    }

    public boolean isAbstract() {
        return Modifier.hasAbstract(modifiers);
    }

    public boolean isStatic() {
        return Modifier.hasStatic(modifiers);
    }

    public boolean isFinal() {
        return Modifier.hasFinal(modifiers);
    }

    public boolean isStrictFP() {
        return Modifier.hasStrictFP(modifiers);
    }

    public boolean isSynthetic() {
        return Modifier.hasSynthetic(modifiers);
    }
}
