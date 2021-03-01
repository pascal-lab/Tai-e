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
import pascal.taie.java.types.ArrayType;
import pascal.taie.java.types.ClassType;
import pascal.taie.java.types.Type;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.ArrayMap;
import pascal.taie.util.HybridArrayHashMap;
import pascal.taie.util.HybridArrayHashSet;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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

    /**
     * Cache results of method dispatch.
     */
    private final Map<JClass, Map<Subsignature, JMethod>> dispatchTable = new HashMap<>();

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
    public JMethod getJREMethod(String methodSig) {
        String className = StringReps.getClassNameOf(methodSig);
        JClass jclass = getJREClass(className);
        if (jclass != null) {
            Subsignature subsig = Subsignature.get(
                    StringReps.getSubsignatureOf(methodSig));
            return jclass.getDeclaredMethod(subsig);
        }
        return null;
    }

    @Override
    public JField getJREField(String fieldSig) {
        String className = StringReps.getClassNameOf(fieldSig);
        String fieldName = StringReps.getFieldNameOf(fieldSig);
        return getJREClass(className).getDeclaredField(fieldName);
    }

    @Override
    public JMethod resolveMethod(MethodReference methodRef) {
        JMethod method = lookupMethod(methodRef.getDeclaringClass(),
                methodRef.getSubsignature(), true);
        if (method != null) {
            return method;
        } else {
            throw new MethodResolutionFailedException(
                    "Cannot resolve " + methodRef);
        }
    }

    @Override
    public JField resolveField(FieldReference fieldRef) {
        JField field = resolveField(fieldRef.getDeclaringClass(),
                fieldRef.getName(), fieldRef.getType());
        if (field != null) {
            return field;
        } else {
            throw new FieldResolutionFailedException("Cannot resolve " + fieldRef);
        }
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
    public JMethod dispatch(Type receiverType, MethodReference methodRef) {
        JClass cls;
        if (receiverType instanceof ClassType) {
            cls = ((ClassType) receiverType).getJClass();
        } else if (receiverType instanceof ArrayType) {
            cls = getJREClass("java.lang.Object");
        } else {
            throw new AnalysisException(receiverType + " cannot be dispatched");
        }
        return dispatch(cls, methodRef);
    }

    @Override
    public JMethod dispatch(JClass receiverClass, MethodReference methodRef) {
        Subsignature subsignature = methodRef.getSubsignature();
        JMethod target = dispatchTable.computeIfAbsent(receiverClass,
                c -> new HybridArrayHashMap<>()).get(subsignature);
        if (target == null) {
            target = lookupMethod(receiverClass, subsignature, false);
            if (target != null) {
                dispatchTable.get(receiverClass).put(subsignature, target);
            }
        }
        return target;
    }

    private JMethod lookupMethod(JClass jclass, Subsignature subsignature,
                                 boolean allowAbstract) {
        // JVM Spec. (Java 13 Ed.), 5.4.3.3 Method Resolution
        // 1. If C is an interface, method resolution throws
        // an IncompatibleClassChangeError. TODO: ???

        // 2. Otherwise, method resolution attempts to locate the
        // referenced method in C and its superclasses
        for (JClass c = jclass; c != null; c = c.getSuperClass()) {
            JMethod method = c.getDeclaredMethod(subsignature);
            if (method != null && (allowAbstract || !method.isAbstract())) {
                return method;
            }
        }
        // 3. Otherwise, method resolution attempts to locate the
        // referenced method in the superinterfaces of the specified class C
        for (JClass c = jclass; c != null; c = c.getSuperClass()) {
            for (JClass iface : jclass.getInterfaces()) {
                JMethod method = lookupMethodFromSuperinterfaces(
                        iface, subsignature, allowAbstract);
                if (method != null) {
                    return method;
                }
            }
        }
        return null;
        // TODO:
        //  1. check accessibility
        //  2. handle phantom methods
        //  3. double-check correctness
    }

    private JMethod lookupMethodFromSuperinterfaces(
            JClass jclass, Subsignature subsignature, boolean allowAbstract) {
        JMethod method = jclass.getDeclaredMethod(subsignature);
        if (method != null && (allowAbstract || !method.isAbstract())) {
            return method;
        }
        for (JClass iface : jclass.getInterfaces()) {
            method = lookupMethodFromSuperinterfaces(
                    iface, subsignature, allowAbstract);
            if (method != null) {
                return method;
            }
        }
        return null;
    }

    @Override
    public boolean isSubclass(JClass superclass, JClass subclass) {
        if (superclass.equals(subclass)) {
            return true;
        } else if (superclass == getObjectClass()) {
            return true;
        } else if (subclass.isInterface()) {
            return superclass.isInterface() &&
                    getAllSubinterfaces(superclass).contains(subclass);
        } else {
            return isSubclass0(superclass, subclass);
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
     * Traverse class hierarchy to check if subclass is a subclass of superclass.
     * TODO: optimize performance
     */
    private boolean isSubclass0(JClass superclass, JClass subclass) {
        boolean isToInterface = superclass.isInterface();
        for (JClass jclass = subclass; jclass != null;
             jclass = jclass.getSuperClass()) {
            if (jclass.equals(superclass)) {
                return true;
            }
            if (isToInterface) {
                // Interfaces can only extend other interfaces, thus we only
                // have to consider the interfaces of the subclass
                // if superclass is an interface.
                for (JClass iface : jclass.getInterfaces()) {
                    if (isSubclass0(superclass, iface)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
