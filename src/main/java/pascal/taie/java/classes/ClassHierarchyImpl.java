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

package pascal.taie.java.classes;

import pascal.taie.java.ClassHierarchy;
import pascal.taie.java.types.Type;
import pascal.taie.util.ArrayMap;
import pascal.taie.util.HybridArrayHashSet;
import pascal.taie.util.StringReps;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class ClassHierarchyImpl implements ClassHierarchy {

    private JClassLoader defaultLoader;

    private JClassLoader bootstrapLoader;

    // TODO: properly manage class loaders
    private final Map<String, JClassLoader> loaders = new ArrayMap<>();

    private JClass JavaLangObject;

    /**
     * Map from each interface to its direct subinterfaces.
     */
    private final ConcurrentMap<JClass, Set<JClass>> directSubinterfaces
            = new ConcurrentHashMap<>();

    /**
     * Map from each interface to all its subinterfaces.
     */
    private final ConcurrentMap<JClass, Set<JClass>> allSubinterfaces
            = new ConcurrentHashMap<>();

    @Override
    public void setDefaultClassLoader(JClassLoader loader) {
        this.defaultLoader = loader;
        loaders.put("default", loader);
    }

    @Override
    public JClassLoader getDefaultClassLoader() {
        return defaultLoader;
    }

    @Override
    public void setBootstrapClassLoader(JClassLoader loader) {
        this.bootstrapLoader = loader;
        loaders.put("bootstrap", loader);
    }

    @Override
    public JClassLoader getBootstrapClassLoader() {
        return bootstrapLoader;
    }

    @Override
    public Collection<JClassLoader> getClassLoaders() {
        return loaders.values()
                .stream()
                .distinct()
                .collect(Collectors.toList());
    }

    @Override
    public void addClass(JClass jclass) {
        // Add direct subinterface
        if (jclass.isInterface()) {
            jclass.getInterfaces().forEach(iface ->
                    directSubinterfaces.computeIfAbsent(iface,
                            i -> new HybridArrayHashSet<>())
                            .add(jclass));
        }
    }

    @Override
    public Collection<JClass> getAllClasses() {
        return loaders.values()
                .stream()
                .distinct()
                .map(JClassLoader::getLoadedClasses)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public JClass getClass(JClassLoader loader, String name) {
        return loader.loadClass(name);
    }

    @Override
    public JClass getClass(String name) {
        // TODO: add warning
        return getClass(getDefaultClassLoader(), name);
    }

    @Override
    public JClass getJREClass(String name) {
        return getClass(getBootstrapClassLoader(), name);
    }

    @Override
    public JMethod getJREMethod(String signature) {
        String className = StringReps.getDeclaringClass(signature);
        Subsignature subsig = Subsignature.get(
                StringReps.getSubsignatureOf(signature));
        return getJREClass(className).getDeclaredMethod(subsig);
    }

    @Override
    public JMethod resolveMethod(MethodReference methodRef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public JField resolveField(FieldReference fieldRef) {
        JField field = fieldRef.getField();
        if (field == null) {
            field = resolveField(fieldRef.getDeclaringClass(),
                    fieldRef.getName(), fieldRef.getType());
            if (field != null) {
                fieldRef.setField(field);
            } else {
                throw new FieldResolutionFailedException("Cannot resolve " + fieldRef);
            }
        }
        return field;
    }

    private JField resolveField(JClass jclass, String name, Type type) {
        // JVM Spec. (Java 13 Ed.), 5.4.3.2 Field Resolution
        // 1. If C declares a field with the name and descriptor specified
        // by the field reference, field lookup succeeds. The declared field
        // is the result of the field lookup.
        JField field = jclass.getDeclaredField(name);
        if (field != null && field.getType().equals(type)) {
            return field;
        }
        // 2. Otherwise, field lookup is applied recursively to the
        // direct superinterfaces of the specified class or interface C.
        for (JClass iface : jclass.getInterfaces()) {
            field = resolveField(iface, name, type);
            if (field != null) {
                return field;
            }
        }
        // 3. Otherwise, if C has a superclass S, field lookup is applied
        // recursively to S.
        if (jclass.getSuperClass() != null) {
            return resolveField(jclass.getSuperClass(), name, type);
        }
        // 4. Otherwise, field lookup fails.
        return null;
        // TODO:
        //  1. check accessibility
        //  2. handle erroneous cases (e.g., multiple fields with same name)
        //  3. handle phantom fields
    }

    @Override
    public JMethod dispatch(JClass receiverClass, MethodReference methodRef) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean canAssign(JClass toClass, JClass fromClass) {
        if (toClass.equals(fromClass)) {
            return true;
        } else if (toClass == getObjectClass()) {
            return true;
        } else if (fromClass.isInterface()) {
            return toClass.isInterface() &&
                    getAllSubinterfaces(toClass).contains(fromClass);
        } else {
            return canAssign0(toClass, fromClass);
        }
    }

    /**
     * Obtain JClass representing java.lang.Object.
     * Since the creation of JClass requires TypeManager, which may
     * not be initialized when class loaders are created,
     * we provide this method to retrieve Object class lazily.
     * @return JClass for java.lang.Object
     */
    private JClass getObjectClass() {
        if (JavaLangObject == null) {
            JClassLoader loader = bootstrapLoader != null ?
                    bootstrapLoader : defaultLoader;
            JavaLangObject = loader.loadClass("java.lang.Object");
        }
        return JavaLangObject;
    }

    private Set<JClass> getAllSubinterfaces(JClass iface) {
        assert iface.isInterface();
        Set<JClass> result = allSubinterfaces.get(iface);
        if (result == null) {
            Set<JClass> directSubs = directSubinterfaces.get(iface);
            if (directSubs == null) {
                result = Collections.emptySet();
            } else {
                result = new HybridArrayHashSet<>(directSubs);
                for (JClass sub : directSubs) {
                    result.addAll(getAllSubinterfaces(sub));
                }
            }
            allSubinterfaces.put(iface, result);
        }
        return result;
    }

    /**
     * Traverse class hierarchy to check if fromClass can be assigned to toClass.
     * TODO: optimize performance
     */
    private boolean canAssign0(JClass toClass, JClass fromClass) {
        boolean isToInterface = toClass.isInterface();
        for (JClass jclass = fromClass; jclass != null;
             jclass = jclass.getSuperClass()) {
            if (jclass.equals(toClass)) {
                return true;
            }
            if (isToInterface) {
                // Interfaces can only extend other interfaces, thus we only
                // have to consider the interfaces of the fromClass
                // if toClass is an interface.
                for (JClass iface : jclass.getInterfaces()) {
                    if (canAssign0(toClass, iface)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
