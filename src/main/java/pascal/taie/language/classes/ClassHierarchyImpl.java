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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import pascal.taie.ir.proginfo.FieldRef;
import pascal.taie.ir.proginfo.MethodRef;
import pascal.taie.language.annotation.AnnotationHolder;
import pascal.taie.language.type.ArrayType;
import pascal.taie.language.type.ClassType;
import pascal.taie.language.type.Type;
import pascal.taie.util.collection.HybridBitSet;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.MultiMap;
import pascal.taie.util.collection.TwoKeyMap;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

public class ClassHierarchyImpl implements ClassHierarchy {

    private static final Logger logger = LogManager.getLogger(ClassHierarchyImpl.class);

    private JClassLoader defaultLoader;

    private JClassLoader bootstrapLoader;

    // TODO: properly manage class loaders
    private final Map<String, JClassLoader> loaders = Maps.newSmallMap();

    private JClass JavaLangObject;

    private final List<JClass> classes = new ArrayList<>(1024);

    private int classCounter = 0;

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

    /**
     * Cache results of {@link #getAllSubclassesOf(JClass)}.
     */
    private final Map<JClass, Set<JClass>> allSubclasses = Maps.newConcurrentMap();

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
            jclass.getInterfaces()
                    .stream()
                    .filter(Objects::nonNull)
                    .forEach(iface -> directImplementors.put(iface, jclass));
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
        // set index
        jclass.setIndex(classCounter++);
        classes.add(jclass);
        // invalidate global hierarchy information
        // TODO - make this elegant
        allSubclasses.clear();
    }

    @Override
    public int getIndex(JClass jclass) {
        return jclass.getIndex();
    }

    @Override
    public JClass getObject(int index) {
        return classes.get(index);
    }

    @Override
    public Stream<JClass> allClasses() {
        return classes.stream();
    }

    @Override
    public Stream<JClass> applicationClasses() {
        return allClasses().filter(JClass::isApplication);
    }

    @Override
    @Nullable
    public JClass getClass(JClassLoader loader, String name) {
        return loader.loadClass(name);
    }

    @Override
    @Nullable
    public JClass getClass(String name) {
        // TODO: add warning for missing class loader
        return getClass(getDefaultClassLoader(), name);
    }

    @Override
    @Nullable
    public JMethod getMethod(String methodSig) {
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
    @Nullable
    public JField getField(String fieldSig) {
        // TODO: add warning for ambiguous fields (due to classes
        //  with the same name)
        String className = StringReps.getClassNameOf(fieldSig);
        JClass jclass = getClass(className);
        if (jclass != null) {
            String fieldName = StringReps.getFieldNameOf(fieldSig);
            String typeName = StringReps.getFieldTypeOf(fieldSig);
            return jclass.getDeclaredField(fieldName, typeName);
        }
        return null;
    }

    @Override
    @Nullable
    public JClass getJREClass(String name) {
        return getClass(getBootstrapClassLoader(), name);
    }

    @Override
    @Nullable
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
    @Nullable
    public JField getJREField(String fieldSig) {
        String className = StringReps.getClassNameOf(fieldSig);
        JClass jclass = getJREClass(className);
        if (jclass != null) {
            String fieldName = StringReps.getFieldNameOf(fieldSig);
            return jclass.getDeclaredField(fieldName);
        }
        return null;
    }

    @Override
    @Nullable
    public JMethod resolveMethod(MethodRef methodRef) {
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
    @Nullable
    public JField resolveField(FieldRef fieldRef) {
        return resolveField(fieldRef.getDeclaringClass(),
                fieldRef.getName(), fieldRef.getType());
    }

    private JField resolveField(JClass jclass, String name, Type type) {
        JField field;
        // 0. First, check and handle phantom fields
        if (jclass.isPhantom()) {
            field = jclass.getPhantomField(name, type);
            if (field == null) {
                field = new JField(jclass, name, Set.of(),
                        type, null, AnnotationHolder.emptyHolder());
                jclass.addPhantomField(name, type, field);
            }
            return field;
        }
        // JVM Spec. (11 Ed.), 5.4.3.2 Field Resolution
        // 1. If C declares a field with the name and descriptor (type) specified
        // by the field reference, field lookup succeeds. The declared field
        // is the result of the field lookup.
        field = jclass.getDeclaredField(name, type);
        if (field != null) {
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
        // 5. Otherwise, field lookup fails.
        return null;
        // TODO: check accessibility
    }

    @Override
    @Nullable
    public JMethod dispatch(Type receiverType, MethodRef methodRef) {
        JClass cls;
        if (receiverType instanceof ClassType) {
            cls = ((ClassType) receiverType).getJClass();
        } else if (receiverType instanceof ArrayType) {
            cls = getJREClass(ClassNames.OBJECT);
        } else {
            logger.warn("{} cannot be dispatched", receiverType);
            return null;
        }
        return dispatch(cls, methodRef);
    }

    @Override
    @Nullable
    public JMethod dispatch(JClass receiverClass, MethodRef methodRef) {
        // check the subclass relation between the receiver class and
        // the class of method reference to avoid the unexpected method found
        if (!isSubclass(methodRef.getDeclaringClass(), receiverClass)) {
            return null;
        }
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
        } else {
            return getAllSubclassesOf(superclass).contains(subclass);
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
    public Collection<JClass> getAllSubclassesOf(JClass jclass) {
        return allSubclasses.computeIfAbsent(jclass, c -> {
            Set<JClass> subclasses = new HybridBitSet<>(this, true);
            getAllSubclassesOf0(c, subclasses);
            return subclasses;
        });
    }

    private void getAllSubclassesOf0(JClass jclass, Set<JClass> result) {
        result.add(jclass);
        if (jclass.isInterface()) {
            getDirectSubinterfacesOf(jclass).forEach(subiface ->
                    getAllSubclassesOf0(subiface, result));
            getDirectImplementorsOf(jclass).forEach(impl ->
                    getAllSubclassesOf0(impl, result));
        } else {
            getDirectSubclassesOf(jclass).forEach(subclass ->
                    getAllSubclassesOf0(subclass, result));
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
