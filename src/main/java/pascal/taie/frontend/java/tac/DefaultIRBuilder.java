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

package pascal.taie.frontend.java.tac;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import pascal.taie.frontend.java.classes.AsmClassSource;
import pascal.taie.frontend.java.type.FrontendTypeSystem;
import pascal.taie.ir.IR;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.ClassSource;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.StringReps;
import pascal.taie.util.collection.Maps;
import pascal.taie.util.collection.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

/**
 * This class implements the IRBuilder interface and is responsible for
 * building Intermediate Representations (IR) for Java methods.
 * It supports building IR for JVM bytecode in ({@link AsmMethodSource}).
 */
public class DefaultIRBuilder implements pascal.taie.ir.IRBuilder {

    private final FrontendTypeSystem typeSystem;

    /**
     * Tracks which classes have had their method sources extracted.
     * Thread-safe set to prevent duplicate parsing of the same class.
     */
    private final Set<JClass> loadedClasses = Sets.newConcurrentSet();

    /**
     * Maps methods to their bytecode sources (adapters).
     * Populated during class parsing and consumed during IR building.
     */
    private final ConcurrentMap<JMethod, AsmMethodSource> method2Source
            = Maps.newConcurrentMap(1024);

    public DefaultIRBuilder(FrontendTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    /**
     * Builds the Intermediate Representation (IR) for the given Java method.
     *
     * @param method the Java method for which to build the IR
     * @return the built IR
     */
    @Override
    public IR buildIR(JMethod method) {
        loadMethodSourcesIfNeeded(method.getDeclaringClass());
        AsmMethodSource source = method2Source.remove(method);
        if (source == null) {
            throw new IllegalStateException("""
                Cannot find method source for %s,
                most likely the method is built twice by mistake.
                """.formatted(method));
        }
        BytecodeIRBuilder builder = new BytecodeIRBuilder(typeSystem, method, source);
        builder.build();
        IR ir = builder.getIr();
        if (ir == null) {
            throw new IllegalStateException("Failed to build IR for method %s"
                    .formatted(method));
        }
        return ir;
    }

    /**
     * Builds IR for all methods in given class hierarchy.
     */
    @Override
    public void buildAll(ClassHierarchy hierarchy) {
        List<JClass> classes;
        classes = hierarchy.allClasses().toList();
        classes.parallelStream().forEach(c -> {
            for (JMethod m : c.getDeclaredMethods()) {
                if (!m.isAbstract() && !m.isNative()) {
                    m.getIR();
                }
            }
        });
    }

    /**
     * Ensures that method sources for the specified class are loaded.
     * If the method sources have already been loaded, this is a no-op.
     * Uses synchronization to guard against concurrent loading.
     * @param clazz the class whose method sources should be loaded
     */
    private void loadMethodSourcesIfNeeded(JClass clazz) {
        // Fast path: already loaded (no synchronization needed)
        if (loadedClasses.contains(clazz)) {
            return;
        }
        // Slow path: need to load
        synchronized (clazz) {
            // Double-check: another thread might have loaded while we waited
            if (loadedClasses.contains(clazz)) {
                return;
            }
            loadMethodSources(clazz);
            loadedClasses.add(clazz);
        }
    }

    /**
     * Loads the method sources for all declared methods in the given class.
     *
     * @param clazz the class whose method sources are to be loaded
     */
    private void loadMethodSources(JClass clazz) {
        ClassSource classSource = clazz.getClassSource();
        if (classSource == null) {
            throw new IllegalStateException("""
                    Cannot find class source for %s,
                    most likely the class is built twice by mistake.
                    """.formatted(clazz));
        }
        if (!(classSource instanceof AsmClassSource source)) {
            throw new UnsupportedOperationException(
                    "Unsupported ClassSource type: " + classSource.getClass()
            );
        }

        // load AsmMethodSources from the AsmClassSource
        int version = source.version();
        Map<MethodKey, AsmMethodSource> methodSources = Maps.newMap();
        source.reader().accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(
                    int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                JSRInlinerAdapter adapter = new JSRInlinerAdapter(
                        null, access, name, descriptor, signature, exceptions);
                methodSources.put(new MethodKey(name, descriptor),
                        new AsmMethodSource(adapter, version));
                return adapter;
            }
        }, ClassReader.SKIP_FRAMES);

        // build mapping from JMethod to AsmMethodSource
        for (JMethod method : clazz.getDeclaredMethods()) {
            AsmMethodSource methodSource = methodSources.get(new MethodKey(
                    method.getName(),
                    StringReps.toBytecodeDescriptor(method)));
            if (methodSource == null) {
                throw new IllegalStateException(
                        "Cannot find method source for %s".formatted(method));
            }
            method2Source.put(method, methodSource);
        }

        // Release ClassSource to save memory after loading method sources
        clazz.releaseClassSource();
    }

    /**
     * Composite key for uniquely identifying methods by name and descriptor.
     * Handles method overloading naturally.
     */
    private record MethodKey(String name, String descriptor) {
    }
}
