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

import pascal.taie.language.type.ClassType;
import pascal.taie.util.AbstractResultHolder;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents classes in the program. Each instance contains various
 * information of a class, including class name, modifiers, declared
 * methods and fields, etc.
 */
public class JClass extends AbstractResultHolder {

    private final JClassLoader loader;

    private final String name;

    private final String moduleName;

    private String simpleName;

    private ClassType type;

    private Set<Modifier> modifiers;

    private JClass superClass;

    private Collection<JClass> interfaces;

    private JClass outerClass;

    private Collection<JClass> innerClasses;

    private Map<String, JField> declaredFields;

    private Map<Subsignature, JMethod> declaredMethods;

    // TODO: annotations

    /**
     * If this class is application class.
     */
    private boolean isApplication;

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
        simpleName = builder.getSimpleName();
        type = builder.getClassType();
        modifiers = builder.getModifiers();
        superClass = builder.getSuperClass();
        interfaces = builder.getInterfaces();
        declaredFields = Collections.unmodifiableMap(
                builder.getDeclaredFields()
                        .stream()
                        .collect(Collectors.toMap(JField::getName, f -> f,
                                (oldV, newV) -> oldV, LinkedHashMap::new))
        );
        declaredMethods = Collections.unmodifiableMap(
                builder.getDeclaredMethods()
                        .stream()
                        .collect(Collectors.toMap(JMethod::getSubsignature, m -> m,
                                (oldV, newV) -> oldV, LinkedHashMap::new))
        );
        isApplication = builder.isApplication();
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

    public String getSimpleName() {
        return simpleName;
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

    public @Nullable
    JClass getSuperClass() {
        return superClass;
    }

    public Collection<JClass> getInterfaces() {
        return interfaces;
    }

    public Collection<JField> getDeclaredFields() {
        return declaredFields.values();
    }

    public @Nullable
    JField getDeclaredField(String fieldName) {
        return declaredFields.get(fieldName);
    }

    public Collection<JMethod> getDeclaredMethods() {
        return declaredMethods.values();
    }

    /**
     * Attempts to retrieve the method with the given name.
     *
     * @throws AmbiguousMethodException if this class has multiple methods
     *                                  with the given name.
     */
    public @Nullable
    JMethod getDeclaredMethod(String methodName) {
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

    /**
     * Attempts to retrieve the method with the given subsignature.
     * If the class has declared a method that has the same subsignature
     * as the given one, then returns the method; otherwise, returns null.
     */
    public @Nullable
    JMethod getDeclaredMethod(Subsignature subSignature) {
        return declaredMethods.get(subSignature);
    }

    public @Nullable
    JMethod getClinit() {
        return getDeclaredMethod(Subsignature.getClinit());
    }

    public boolean isApplication() {
        return isApplication;
    }

    @Override
    public String toString() {
        return getName();
    }
}
