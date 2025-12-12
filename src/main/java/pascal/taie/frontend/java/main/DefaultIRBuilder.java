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

package pascal.taie.frontend.java.main;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.JSRInlinerAdapter;
import pascal.taie.frontend.java.classes.AsmMethodSource;
import pascal.taie.frontend.java.classes.AsmSource;
import pascal.taie.frontend.java.tac.BytecodeIRBuilder;
import pascal.taie.frontend.java.type.FrontendTypeSystem;
import pascal.taie.ir.IR;
import pascal.taie.ir.IRBuildHelper;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.classes.JClass;
import pascal.taie.language.classes.JMethod;
import pascal.taie.language.classes.StringReps;
import pascal.taie.util.collection.Maps;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class implements the IRBuilder interface and is responsible for
 * building Intermediate Representations (IR) for Java methods.
 * It supports building IR for JVM Bytecode ({@link AsmMethodSource}) method sources.
 */
public class DefaultIRBuilder implements pascal.taie.ir.IRBuilder {

    private final FrontendTypeSystem typeSystem;

    private static final Logger logger = LogManager.getLogger(DefaultIRBuilder.class);

    // Magic numbers for class loading status
    private static final int NOT_LOADED = 0;

    private static final int LOADING_START = 1;

    private static final int LOADING_DONE = 2;

    /**
     * A map to track the loading status of methods.
     */
    private final ConcurrentMap<JMethod, AtomicBoolean> methodStatusMap = Maps.newConcurrentMap();

    /**
     * A map to track the loading status of classes.
     */
    private final ConcurrentMap<JClass, AtomicInteger> classStatusMap = Maps.newConcurrentMap();

    /**
     * A map to store the class source for each class.
     */
    private final ConcurrentMap<JClass, AsmSource> class2Node = Maps.newConcurrentMap();

    /**
     * A map to store the method source for each method.
     */
    private final ConcurrentMap<JMethod, AsmMethodSource> method2Source = Maps.newConcurrentMap();

    public DefaultIRBuilder(FrontendTypeSystem typeSystem) {
        this.typeSystem = typeSystem;
    }

    /**
     * A helper class to load method sources concurrently.
     */
    private static class LoadingKV {
        private final Map<String, AsmMethodSource> fastMap;
        private final Map<String, Map<String, AsmMethodSource>> slowMap;

        private LoadingKV() {
            fastMap = Maps.newMap();
            slowMap = Maps.newMap();
        }

        void put(String name, String descriptor, AsmMethodSource value) {
            if (slowMap.containsKey(name)) {
                slowMap.get(name).put(descriptor, value);
            } else if (fastMap.containsKey(name)) {
                slowMap.put(name, Maps.newMap());
                AsmMethodSource oldValue = fastMap.remove(name);
                slowMap.get(name).put(oldValue.adapter().desc, oldValue);
                slowMap.get(name).put(descriptor, value);
            } else {
                fastMap.put(name, value);
            }
        }
    }

    /**
     * Builds the Intermediate Representation (IR) for the given Java method.
     *
     * @param method the Java method for which to build the IR
     * @return the built IR
     */
    @Override
    public IR buildIR(JMethod method) {
        try {
            Object source = method.getMethodSource();
            if (source instanceof AsmMethodSource asmMethodSource) {
                BytecodeIRBuilder builder = new BytecodeIRBuilder(typeSystem, method, asmMethodSource);
                builder.build();
                return builder.getIr();
            } else if (source == null) {
                return loadingAndGetIR(method);
            } else {
                throw new UnsupportedOperationException();
            }
        } catch (RuntimeException e) {
            if (e.getStackTrace()[0].getClassName().startsWith("Asm")) {
                logger.warn("ASM bytecode front failed to build method body for {}," +
                        " constructs an empty IR instead", method);
                return new IRBuildHelper(method).buildEmpty();
            } else {
                throw e;
            }
        }
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

    private IR loadingAndGetIR(JMethod method) {
        loadClassSourceSync(method.getDeclaringClass());
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
            throw new IllegalStateException("IR is null for method %s".formatted(method));
        }
        return ir;
    }

    /**
     * Loads the class source synchronously.
     * <p>
     * The method needs explicit synchronization dues to the {@link DefaultIRBuilder#buildAll(ClassHierarchy)}
     * called this method in parallel.
     * </p>
     * <p>
     * Consider the following scenario:
     * <ul>
     * <li>Thread A calls {@link DefaultIRBuilder#buildIR(JMethod)} for {@code A.f}</li>
     * <li>Thread B calls {@link DefaultIRBuilder#buildIR(JMethod)} for {@code A.g}</li>
     * </ul>
     * Then Thread A will call {@link DefaultIRBuilder#loadingAndGetIR(JMethod)} for {@code A.f},
     * which will call {@link DefaultIRBuilder#loadClassSourceSync(JClass)} for {@code A},
     * and similarly for Thread B.
     * </p>
     * <p>
     * Thus A and B will call this method for the same class {@code A}.
     * </p>
     * <p>
     * In this case, the current implementation will urge the second thread (let's say Thread B)
     * to wait for the first thread (Thread A) to finish loading the class.
     * </p>
     * @param clazz the class to load
     */
    private void loadClassSourceSync(JClass clazz) {
        // TODO: current sync method is correct, but may need some optimization
        AtomicInteger status = classStatusMap.computeIfAbsent(clazz, k -> new AtomicInteger(NOT_LOADED));
        if (status.get() == LOADING_DONE) {
            return;
        } else if (status.compareAndSet(NOT_LOADED, LOADING_START)) {
            try {
                loadClassSourceImpl(clazz);
                status.set(LOADING_DONE);
            } finally {
                // put this in finally block to avoid deadlock
                synchronized (status) { // notifyAll() must be called in synchronized block
                    status.notifyAll();
                }
            }
        } else {
            synchronized (status) { // wait() must be called in synchronized block
                // now we have to wait for the class to be loaded
                while (status.get() == LOADING_START) {
                    try {
                        status.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
        }
    }

    private void loadClassSourceImpl(JClass clazz) {
        // use remove to release memory
        AsmSource source = class2Node.remove(clazz);
        assert source != null;
        int version = source.version();
        LoadingKV kv = new LoadingKV();
        source.reader().accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                JSRInlinerAdapter adapter = new JSRInlinerAdapter(null, access, name, descriptor, signature, exceptions);
                kv.put(name, descriptor, new AsmMethodSource(adapter, version));
                return adapter;
            }
        }, ClassReader.SKIP_FRAMES);
        paringMethodSource(kv, clazz);
    }

    private void paringMethodSource(LoadingKV kv, JClass clazz) {
        for (JMethod method : clazz.getDeclaredMethods()) {
            AsmMethodSource source = kv.fastMap.get(method.getName());
            if (source == null) {
                source = kv.slowMap.get(method.getName())
                        .get(StringReps.toBytecodeDescriptor(method));
            }
            if (source == null) {
                throw new IllegalStateException("Cannot find method source for %s".formatted(method));
            }
            method2Source.put(method, source);
        }
    }

    public void putClassSource(JClass clazz, AsmSource source) {
        class2Node.put(clazz, source);
    }
}
