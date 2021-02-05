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

import pascal.taie.java.types.ClassType;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JClass {

    private final JClassLoader loader;

    private final String name;

    private final String moduleName;

    private ClassType type;

    private Set<Modifier> modifiers;

    private JClass superClass;

    private Collection<JClass> implementedInterfaces = Collections.emptySet();

    private JClass outerClass;

    private Collection<JClass> innerClasses;

    private Map<String, JField> declaredFields;

    private Map<Subsignature, JMethod> declaredMethods;

    // TODO: annotations

    public JClass(JClassLoader loader, String name) {
        this(loader, name, null);
    }

    public JClass(JClassLoader loader, String name, String moduleName) {
        this.loader = loader;
        this.name = name;
        this.moduleName = moduleName;
    }

    public void init(JClassBuilder builder) {
        type = builder.getClassType();
        modifiers = builder.getModifiers();
        superClass = builder.getSuperClass();
        implementedInterfaces = builder.getInterfaces();
        declaredFields = builder.getDeclaredFields().stream()
                .collect(Collectors.toMap(JField::getName,
                        Function.identity()));
        declaredMethods = builder.getDeclaredMethods().stream()
                .collect(Collectors.toMap(JMethod::getSubsignature,
                        Function.identity()));
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

    public JClass getSuperClass() {
        return superClass;
    }

    public Collection<JClass> getImplementedInterfaces() {
        return implementedInterfaces;
    }

    public Collection<JField> getDeclaredFields() {
        return declaredFields.values();
    }

    public Collection<JMethod> getDeclaredMethods() {
        return declaredMethods.values();
    }
}
