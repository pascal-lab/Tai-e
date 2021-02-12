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
     * Map from each interface to its subclasses/subinterfaces
     * that directly implements/extends it.
     */
    private final ConcurrentMap<JClass, Set<JClass>> directInterfaceSubs
            = new ConcurrentHashMap<>();

    /**
     * Map from each interface to all its subclasses/subinterfaces
     * that implements/extends it.
     */
    private final ConcurrentMap<JClass, Set<JClass>> allInterfaceSubs
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
        jclass.getInterfaces().forEach(iface ->
                directInterfaceSubs.computeIfAbsent(iface,
                        i -> new HybridArrayHashSet<>())
                        .add(jclass));
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
        throw new UnsupportedOperationException();
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
        } else if (toClass.isInterface()) {
            return getAllInterfaceSubs(toClass).contains(fromClass);
        } else {
            for (JClass c = fromClass.getSuperClass(); c != null;
                 c = c.getSuperClass()) {
                if (c.equals(toClass)) {
                    return true;
                }
            }
            return false;
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

    /**
     * TODO: make this method thread-safe?
     */
    private Set<JClass> getAllInterfaceSubs(JClass iface) {
        assert iface.isInterface();
        Set<JClass> result = allInterfaceSubs.get(iface);
        if (result == null) {
            Set<JClass> directSubs = directInterfaceSubs.get(iface);
            if (directSubs == null) {
                result = Collections.emptySet();
            } else {
                result = new HybridArrayHashSet<>();
                for (JClass sub : directSubs) {
                    result.addAll(getAllInterfaceSubs(sub));
                }
            }
            allInterfaceSubs.put(iface, result);
        }
        return result;
    }
}
