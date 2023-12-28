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

package pascal.taie.language.classes;

import pascal.taie.World;
import pascal.taie.language.annotation.Annotated;
import pascal.taie.language.annotation.Annotation;
import pascal.taie.language.annotation.AnnotationHolder;
import pascal.taie.language.generics.ClassGSignature;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.AbstractResultHolder;
import pascal.taie.util.Experimental;
import pascal.taie.util.Indexable;
import pascal.taie.util.collection.CollectionUtils;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.MultiMapCollector;
import pascal.taie.util.collection.Sets;

import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Represents classes in the program. Each instance contains various
 * information of a class, including class name, modifiers, declared
 * methods and fields, etc.
 */
public class JClass extends AbstractResultHolder
        implements Annotated, Indexable, Serializable {

    private final JClassLoader loader;

    private final String name;

    private final String moduleName;

    private String simpleName;

    private ClassType type;

    @Nullable
    @Experimental
    private ClassGSignature gSignature;

    private Set<Modifier> modifiers;

    private JClass superClass;

    private Collection<JClass> interfaces;

    private JClass outerClass;

    private MultiMap<String, JField> declaredFields;

    private Map<Subsignature, JMethod> declaredMethods;

    private AnnotationHolder annotationHolder;

    private boolean isPhantom;

    private final MultiMap<String, JField> phantomFields = Maps.newMultiMap();

    /**
     * If this class is application class.
     */
    private boolean isApplication;

    private int index = -1;

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
        gSignature = builder.getGSignature();
        modifiers = builder.getModifiers();
        annotationHolder = builder.getAnnotationHolder();
        isApplication = builder.isApplication();
        isPhantom = builder.isPhantom();
        try {
            superClass = builder.getSuperClass();
            interfaces = builder.getInterfaces();
            outerClass = builder.getOuterClass();
            declaredFields = Maps.unmodifiableMultiMap(
                    builder.getDeclaredFields()
                            .stream()
                            .collect(MultiMapCollector.get(
                                    () -> Maps.newMultiMap(Maps.newLinkedHashMap(),
                                            Sets::newHybridOrderedSet),
                                    JField::getName, f -> f))
            );
            declaredMethods = Collections.unmodifiableMap(
                    builder.getDeclaredMethods()
                            .stream()
                            .collect(Collectors.toMap(JMethod::getSubsignature, m -> m,
                                    (oldV, newV) -> oldV, Maps::newLinkedHashMap))
            );
        } catch (Exception e) {
            if (World.get().getOptions().isAllowPhantom()) {
                superClass = getClassLoader().loadClass(ClassNames.OBJECT);
                interfaces = Collections.emptySet();
                outerClass = null;
                declaredFields = Maps.emptyMultiMap();
                declaredMethods = Map.of();
            } else {
                throw e;
            }
        }
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

    @Nullable
    @Experimental
    public ClassGSignature getGSignature() {
        return gSignature;
    }

    public Set<Modifier> getModifiers() {
        return modifiers;
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

    @Nullable
    public JClass getSuperClass() {
        return superClass;
    }

    public Collection<JClass> getInterfaces() {
        return interfaces;
    }

    public boolean hasOuterClass() {
        return outerClass != null;
    }

    @Nullable
    public JClass getOuterClass() {
        return outerClass;
    }

    public Collection<JField> getDeclaredFields() {
        return declaredFields.values();
    }

    /**
     * Attempts to retrieve the field with the given name.
     *
     * @throws AmbiguousMemberException if this class has multiple fields
     *                                  with the given name.
     */
    @Nullable
    public JField getDeclaredField(String fieldName) {
        Set<JField> fields = declaredFields.get(fieldName);
        return switch (fields.size()) {
            case 0 -> null;
            case 1 -> CollectionUtils.getOne(fields);
            default -> throw new AmbiguousMemberException(name, fieldName);
        };
    }

    /**
     * Attempts to retrieve the field with given name and type.
     *
     * @return the target field with given name and type,
     * or {@code null} if such field does not exist.
     */
    @Nullable
    public JField getDeclaredField(String fieldName, Type fieldType) {
        for (JField field : declaredFields.get(fieldName)) {
            if (field.getType().equals(fieldType)) {
                return field;
            }
        }
        return null;
    }

    /**
     * Attempts to retrieve the field with given name and type.
     *
     * @param fieldName name of the field
     * @param typeName  name of the field type
     * @return the target field with given name and type,
     * or {@code null} if such field does not exist.
     */
    @Nullable
    public JField getDeclaredField(String fieldName, String typeName) {
        for (JField field : declaredFields.get(fieldName)) {
            if (field.getType().getName().equals(typeName)) {
                return field;
            }
        }
        return null;
    }

    public Collection<JMethod> getDeclaredMethods() {
        return declaredMethods.values();
    }

    /**
     * Attempts to retrieve the method with the given name.
     *
     * @throws AmbiguousMemberException if this class has multiple methods
     *                                  with the given name.
     */
    @Nullable
    public JMethod getDeclaredMethod(String methodName) {
        JMethod result = null;
        for (JMethod method : declaredMethods.values()) {
            if (method.getName().equals(methodName)) {
                if (result == null) {
                    result = method;
                } else {
                    throw new AmbiguousMemberException(name, methodName);
                }
            }
        }
        return result;
    }

    /**
     * Attempts to retrieve the method with given subsignature.
     *
     * @param subsignature subsignature of the method
     * @return the target method with given subsignature,
     * or {@code null} if such method does not exist.
     */
    @Nullable
    public JMethod getDeclaredMethod(Subsignature subsignature) {
        return declaredMethods.get(subsignature);
    }

    @Nullable
    public JMethod getClinit() {
        return getDeclaredMethod(Subsignature.getClinit());
    }

    @Override
    public boolean hasAnnotation(String annotationType) {
        return annotationHolder.hasAnnotation(annotationType);
    }

    @Override
    @Nullable
    public Annotation getAnnotation(String annotationType) {
        return annotationHolder.getAnnotation(annotationType);
    }

    @Override
    public Collection<Annotation> getAnnotations() {
        return annotationHolder.getAnnotations();
    }

    public boolean isApplication() {
        return isApplication;
    }

    public boolean isPhantom() {
        return isPhantom;
    }

    @Nullable
    public JField getPhantomField(String fieldName, Type fieldType) {
        assert isPhantom();
        for (JField field : phantomFields.get(fieldName)) {
            if (field.getType().equals(fieldType)) {
                return field;
            }
        }
        return null;
    }

    public void addPhantomField(String fieldName, Type fieldType, JField field) {
        assert isPhantom();
        assert getPhantomField(fieldName, fieldType) == null :
                String.format("'%s' already has phantom field '%s'", this, field);
        phantomFields.put(fieldName, field);
    }

    void setIndex(int index) {
        if (this.index != -1) {
            throw new IllegalStateException("index already set");
        }
        if (index < 0) {
            throw new IllegalArgumentException(
                    "index must be 0 or positive number, given: " + index);
        }
        this.index = index;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return getName();
    }
}
