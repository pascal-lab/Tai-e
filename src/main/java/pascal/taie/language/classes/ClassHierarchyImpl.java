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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.AnalysisException;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.Sets;
import pascal.taie.util.collection.TwoKeyMap;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class ClassHierarchyImpl implements ClassHierarchy {

    private static final Logger logger = LogManager.getLogger(ClassHierarchyImpl.class);

    private JClassLoader defaultLoader;

    private JClassLoader bootstrapLoader;

    // TODO: properly manage class loaders
    private final Map<String, JClassLoader> loaders = Maps.newSmallMap();

    private JClass JavaLangObject;

    /**
     * Map from each interface to its direct subinterfaces.
     */
    private final MultiMap<JClass, JClass> directSubinterfaces = Maps.newMultiMap();

    /**
     * Map from each interface to its direct implementors.
     */
    private final MultiMap<JClass, JClass> directImplementors = Maps.newMultiMap();

    /**
     * Map from each class to its direct subclasses.
     */
    private final MultiMap<JClass, JClass> directSubclasses = Maps.newMultiMap();

    /**
     * Map from a class to its direct inner classes.
     */
    private final MultiMap<JClass, JClass> directInnerClasses = Maps.newMultiMap();

    /**
     * Cache results of method dispatch.
     */
    private final TwoKeyMap<JClass, Subsignature, JMethod> dispatchTable = Maps.newTwoKeyMap();

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
                .toList();
    }

    @Override
    public void addClass(JClass jclass) {
        // Add direct subinterface
        if (jclass.isInterface()) {
            jclass.getInterfaces().forEach(iface ->
                    directSubinterfaces.put(iface, jclass));
        } else {
            // add direct implementors
            jclass.getInterfaces().forEach(iface ->
                    directImplementors.put(iface, jclass));
            // add direct subclasses
            JClass superClass = jclass.getSuperClass();
            if (superClass != null) {
                directSubclasses.put(superClass, jclass);
            }
        }
        // add inner classes
        JClass outer = jclass.getOuterClass();
        if (outer != null) {
            directInnerClasses.put(outer, jclass);
        }
    }

    @Override
    public Stream<JClass> allClasses() {
        return loaders.values()
                .stream()
                .distinct()
                .map(JClassLoader::getLoadedClasses)
                .flatMap(Collection::stream);
    }

    @Override
    public Stream<JClass> applicationClasses() {
        return allClasses().filter(JClass::isApplication);
    }

    @Override
    public @Nullable
    JClass getClass(JClassLoader loader, String name) {
        return loader.loadClass(name);
    }

    @Override
    public @Nullable
    JClass getClass(String name) {
        // TODO: add warning for missing class loader
        return getClass(getDefaultClassLoader(), name);
    }

    @Override
    public @Nullable
    JMethod getMethod(String methodSig) {
        // TODO: add warning for ambiguous methods (due to classes
        //  with the same name)
        String className = StringReps.getClassNameOf(methodSig);
        JClass jclass = getClass(className);
        if (jclass != null) {
            Subsignature subsig = Subsignature.get(
                    StringReps.getSubsignatureOf(methodSig));
            return jclass.getDeclaredMethod(subsig);
        }
        return null;
    }

    @Override
    public @Nullable
    JField getField(String fieldSig) {
        // TODO: add warning for ambiguous fields (due to classes
        //  with the same name)
        String className = StringReps.getClassNameOf(fieldSig);
        JClass jclass = getClass(className);
        if (jclass != null) {
            String fieldName = StringReps.getFieldNameOf(fieldSig);
            return jclass.getDeclaredField(fieldName);
        }
        return null;
    }

    @Override
    public @Nullable
    JClass getJREClass(String name) {
        return getClass(getBootstrapClassLoader(), name);
    }

    @Override
    public @Nullable
    JMethod getJREMethod(String methodSig) {
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
    public @Nullable
    JField getJREField(String fieldSig) {
        String className = StringReps.getClassNameOf(fieldSig);
        JClass jclass = getJREClass(className);
        if (jclass != null) {
            String fieldName = StringReps.getFieldNameOf(fieldSig);
            return jclass.getDeclaredField(fieldName);
        }
        return null;
    }

    @Override
    public @Nullable
    JMethod resolveMethod(MethodRef methodRef) {
        JClass declaringClass = methodRef.getDeclaringClass();
        JMethod method = lookupMethod(declaringClass,
                methodRef.getSubsignature(), true);
        if (method != null) {
            return method;
        } else if (methodRef.isPolymorphicSignature()) {
            // For reference to polymorphic signature method, we return
            // method with the same name that is declared in declaringClass.
            return declaringClass.getDeclaredMethod(methodRef.getName());
        }
        return null;
    }

    @Override
    public @Nullable
    JField resolveField(FieldRef fieldRef) {
        return resolveField(fieldRef.getDeclaringClass(),
                fieldRef.getName(), fieldRef.getType());
    }

    private JField resolveField(JClass jclass, String name, Type type) {
        // JVM Spec. (11 Ed.), 5.4.3.2 Field Resolution
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
    public @Nullable
    JMethod dispatch(Type receiverType, MethodRef methodRef) {
        JClass cls;
        if (receiverType instanceof ClassType) {
            cls = ((ClassType) receiverType).getJClass();
        } else if (receiverType instanceof ArrayType) {
            cls = getJREClass(ClassNames.OBJECT);
        } else {
            throw new AnalysisException(receiverType + " cannot be dispatched");
        }
        return dispatch(cls, methodRef);
    }

    @Override
    public @Nullable
    JMethod dispatch(JClass receiverClass, MethodRef methodRef) {
        Subsignature subsignature = methodRef.getSubsignature();
        JMethod target = dispatchTable.get(receiverClass, subsignature);
        if (target == null) {
            target = lookupMethod(receiverClass, subsignature, false);
            if (target != null) {
                dispatchTable.put(receiverClass, subsignature, target);
            } else {
                logger.debug("Failed to dispatch {} on {}",
                        subsignature, receiverClass);
            }
        }
        return target;
    }

    private JMethod lookupMethod(JClass jclass, Subsignature subsignature,
                                 boolean allowAbstract) {
        // JVM Spec. (11 Ed.), 5.4.3.3 Method Resolution
        // 1. If C is an interface, method resolution throws
        // an IncompatibleClassChangeError. TODO: what does this mean???

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
            for (JClass iface : c.getInterfaces()) {
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
                    isSubinterface(superclass, subclass);
        } else {
            return isSubclass0(superclass, subclass);
        }
    }

    /**
     * Obtains JClass representing java.lang.Object.
     * Since the creation of JClass requires TypeSystem, which may
     * not be initialized when class loaders are created,
     * we provide this method to retrieve Object class lazily.
     *
     * @return JClass for java.lang.Object
     */
    private JClass getObjectClass() {
        if (JavaLangObject == null) {
            JClassLoader loader = bootstrapLoader != null ?
                    bootstrapLoader : defaultLoader;
            JavaLangObject = loader.loadClass(ClassNames.OBJECT);
        }
        return JavaLangObject;
    }

    /**
     * Traverses class hierarchy to check if subiface is
     * a subinterface of superiface.
     */
    private boolean isSubinterface(JClass superiface, JClass subiface) {
        if (subiface.equals(superiface)) {
            return true;
        }
        for (JClass iface : subiface.getInterfaces()) {
            if (isSubinterface(superiface, iface)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Traverses class hierarchy to check if subclass is a subclass of superclass.
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

    @Override
    public Collection<JClass> getAllSubclassesOf(JClass jclass, boolean selfInclude) {
        // TODO: cache results?
        Set<JClass> subclasses = Sets.newHybridSet();
        getAllSubclassesOf0(jclass, subclasses, selfInclude);
        return subclasses;
    }

    private void getAllSubclassesOf0(JClass jclass, Set<JClass> result, boolean selfInclude) {
        if (selfInclude) {
            result.add(jclass);
        }
        if (jclass.isInterface()) {
            getDirectSubinterfacesOf(jclass).forEach(subiface ->
                    getAllSubclassesOf0(subiface, result, true));
            getDirectImplementorsOf(jclass).forEach(impl ->
                    getAllSubclassesOf0(impl, result, true));
        } else {
            getDirectSubclassesOf(jclass).forEach(subclass ->
                    getAllSubclassesOf0(subclass, result, true));
        }
    }

    @Override
    public Collection<JClass> getDirectSubinterfacesOf(JClass jclass) {
        return directSubinterfaces.get(jclass);
    }

    @Override
    public Collection<JClass> getDirectImplementorsOf(JClass jclass) {
        return directImplementors.get(jclass);
    }

    @Override
    public Collection<JClass> getDirectSubclassesOf(JClass jclass) {
        return directSubclasses.get(jclass);
    }

    @Override
    public Collection<JClass> getDirectInnerClassesOf(JClass jclass) {
        return directInnerClasses.get(jclass);
    }
}
