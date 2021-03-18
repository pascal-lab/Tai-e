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

package pascal.taie.language.classes;

import pascal.taie.language.types.ClassType;

import javax.annotation.Nullable;
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

    private Collection<JClass> interfaces = Collections.emptySet();

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

    /**
     * This method should be called after creating this instance.
     */
    public void build(JClassBuilder builder) {
        type = builder.getClassType();
        modifiers = builder.getModifiers();
        superClass = builder.getSuperClass();
        interfaces = builder.getInterfaces();
        declaredFields = builder.getDeclaredFields().stream()
                .collect(Collectors.toMap(JField::getName,
                        Function.identity()));
        declaredMethods = builder.getDeclaredMethods().stream()
                .collect(Collectors.toMap(JMethod::getSubsignature,
                        Function.identity()));
    }

    public JClassLoader getClassLoader() {
        return loader;
    }

    public String getName() {
        return name;
    }

    public String getModuleName() {
        return moduleName;
    }

    public ClassType getType() {
        return type;
    }

    public Set<Modifier> getModifiers() {
        return Collections.unmodifiableSet(modifiers);
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

    public @Nullable JClass getSuperClass() {
        return superClass;
    }

    public Collection<JClass> getInterfaces() {
        return interfaces;
    }

    public Collection<JField> getDeclaredFields() {
        return declaredFields.values();
    }

    public @Nullable JField getDeclaredField(String fieldName) {
        return declaredFields.get(fieldName);
    }

    public Collection<JMethod> getDeclaredMethods() {
        return declaredMethods.values();
    }

    /**
     * Attempts to retrieve the method with the given name.
     * @throws AmbiguousMethodException if this class has multiple methods
     *  with the given name.
     */
    public @Nullable JMethod getDeclaredMethod(String methodName) {
        JMethod result = null;
        for (JMethod method : declaredMethods.values()) {
            if (method.getName().equals(methodName)) {
                if (result == null) {
                    result = method;
                } else {
                    throw new AmbiguousMethodException(name, methodName);
                }
            }
        }
        return result;
    }

    public @Nullable JMethod getDeclaredMethod(Subsignature subSignature) {
        return declaredMethods.get(subSignature);
    }

    public @Nullable JMethod getClinit() {
        return getDeclaredMethod(Subsignature.get(StringReps.CLINIT));
    }

    @Override
    public String toString() {
        return getName();
    }
}
