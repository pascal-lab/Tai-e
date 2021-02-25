/*
 * Tai-e: A Program Analysis Framework for Java
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

package pascal.taie.pta.env.nativemodel;

import pascal.taie.callgraph.CallKind;
import pascal.taie.java.ClassHierarchy;
import pascal.taie.java.TypeManager;
import pascal.taie.java.classes.JClass;
import pascal.taie.java.classes.JField;
import pascal.taie.java.classes.JMethod;
import pascal.taie.java.types.Type;
import pascal.taie.pta.PTAOptions;
import pascal.taie.pta.env.EnvObj;
import pascal.taie.pta.ir.Allocation;
import pascal.taie.pta.ir.ArrayStore;
import pascal.taie.pta.ir.Assign;
import pascal.taie.pta.ir.Call;
import pascal.taie.pta.ir.IR;
import pascal.taie.pta.ir.StaticStore;
import pascal.taie.pta.ir.Variable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

class MethodModel {

    private final ClassHierarchy hierarchy;

    private final TypeManager typeManager;

    // Use String as key is to avoid cyclic dependence during the
    // initialization of ProgramManager.
    // TODO: use Method as key to improve performance?
    private final Map<JMethod, Consumer<IR>> handlers;

    /**
     * Counter to give each mock variable an unique name in each method.
     */
    private final ConcurrentMap<JMethod, AtomicInteger> counter;

    MethodModel(ClassHierarchy hierarchy, TypeManager typeManager) {
        this.hierarchy = hierarchy;
        this.typeManager = typeManager;
        handlers = new HashMap<>();
        counter = new ConcurrentHashMap<>(0);
        initHandlers();
    }

    void process(IR ir) {
        Consumer<IR> handler = handlers.get(ir.getMethod());
        if (handler != null) {
            handler.accept(ir);
        }
    }

    private void initHandlers() {
        // --------------------------------------------------------------------
        // java.lang.Object
        // --------------------------------------------------------------------
        // <java.lang.Object: java.lang.Object clone()>
        // TODO: could throw CloneNotSupportedException
        // TODO: should check if the object is Cloneable.
        // TODO: should return a clone of the heap allocation (not
        //  identity). The behaviour implemented here is based on Soot.
        registerHandler("<java.lang.Object: java.lang.Object clone()>", ir ->
            ir.getReturnVariables().forEach(ret ->
                    ir.addStatement(new Assign(ret, ir.getThis())))
        );

        // --------------------------------------------------------------------
        // java.lang.ref.Reference
        // --------------------------------------------------------------------
        // The *garbage collector* assigns every reference to Reference.pending.
        // So basically Reference.pending can point to every reference. The
        // ReferenceHandler takes care of enqueueing the references in a
        // reference queue. If we do not model this GC behavior,
        // Reference.pending points to nothing, and finalize() methods won't
        // get invoked.
        registerHandler("<java.lang.ref.Reference: void <init>(java.lang.Object,java.lang.ref.ReferenceQueue)>", ir -> {
            addStaticStore(ir, "<java.lang.ref.Reference: java.lang.ref.Reference pending>",
                    ir.getThis());
        });

        // --------------------------------------------------------------------
        // java.lang.System
        // --------------------------------------------------------------------
        // <java.lang.System: void setIn0(java.io.InputStream)>
        registerHandler("<java.lang.System: void setIn0(java.io.InputStream)>", ir -> {
            addStaticStore(ir, "<java.lang.System: java.io.InputStream in>",
                    ir.getParam(0));
        });

        // <java.lang.System: void setOut0(java.io.PrintStream)>
        registerHandler("<java.lang.System: void setOut0(java.io.PrintStream)>", ir -> {
            addStaticStore(ir, "<java.lang.System: java.io.PrintStream out>",
                    ir.getParam(0));
        });

        // <java.lang.System: void setErr0(java.io.PrintStream)>
        registerHandler("<java.lang.System: void setErr0(java.io.PrintStream)>", ir -> {
            addStaticStore(ir, "<java.lang.System: java.io.PrintStream err>",
                    ir.getParam(0));
        });

        // --------------------------------------------------------------------
        // java.lang.Thread
        // --------------------------------------------------------------------
        // <java.lang.Thread: void start[0]()>
        // Redirect calls to Thread.start() to Thread.run().
        // Before Java 5, Thread.start() itself is native. Since Java 5,
        // start() is written in Java which calls native method start0().
        final String start = PTAOptions.get().jdkVersion() <= 4
                ? "<java.lang.Thread: void start()>"
                : "<java.lang.Thread: void start0()>";
        registerHandler(start, ir -> {
            JMethod run = hierarchy.getJREMethod(
                    "<java.lang.Thread: void run()>");
            MockCallSite runCallSite = new MockCallSite(CallKind.VIRTUAL,
                    run.getRef(), ir.getThis(), Collections.emptyList(),
                    ir.getMethod(), "thread-run");
            Call runCall = new Call(runCallSite, null);
            ir.addStatement(runCall);
        });

        // --------------------------------------------------------------------
        // java.io.FileSystem
        // --------------------------------------------------------------------
        final List<String> concreteFileSystems = Arrays.asList(
                "java.io.UnixFileSystem",
                "java.io.WinNTFileSystem",
                "java.io.Win32FileSystem"
        );
        // <java.io.FileSystem: java.io.FileSystem getFileSystem()>
        // This API is implemented in Java code since Java 7.
        if (PTAOptions.get().jdkVersion() <= 6) {
            registerHandler("<java.io.FileSystem: java.io.FileSystem getFileSystem()>", ir -> {
                concreteFileSystems.forEach(fsName -> {
                    JClass fs = hierarchy.getJREClass(fsName);
                    if (fs != null) {
                        ir.getReturnVariables().forEach(ret -> {
                            Utils.modelAllocation(hierarchy, ir,
                                    fs.getType(), fsName, ret,
                                    "<" + fs.getName() + ": void <init>()>",
                                    "init-file-system");
                        });
                    }
                });
            });
        }

        // <java.io.*FileSystem: java.lang.String[] list(java.io.File)>
        concreteFileSystems.forEach(fsName -> {
            registerHandler("<" + fsName + ": java.lang.String[] list(java.io.File)>", ir -> {
                JMethod method = ir.getMethod();
                Type string = typeManager.getClassType("java.lang.String");
                EnvObj elem = new EnvObj("dir-element", string, method);
                Variable temp = newMockVariable(string, method);
                Type stringArray = typeManager.getArrayType(string, 1);
                EnvObj array = new EnvObj("element-array", stringArray, method);
                ir.getReturnVariables().forEach(ret -> {
                    ir.addStatement(new Allocation(temp, elem));
                    ir.addStatement(new Allocation(ret, array));
                    ir.addStatement(new ArrayStore(ret, temp));
                });
            });
        });

        // --------------------------------------------------------------------
        // sun.misc.Perf
        // --------------------------------------------------------------------
        // <sun.misc.Perf: java.nio.ByteBuffer createLong(java.lang.String,int,int,long)>
        registerHandler("<sun.misc.Perf: java.nio.ByteBuffer createLong(java.lang.String,int,int,long)>", ir -> {
            ir.getReturnVariables().forEach(ret -> {
                Type buffer = typeManager.getClassType("java.nio.DirectByteBuffer");
                Utils.modelAllocation(hierarchy, ir,
                        buffer, buffer.getName(), ret,
                        "<java.nio.DirectByteBuffer: void <init>(int)>",
                        "create-long-buffer");
            });
        });
    }

    private void registerHandler(String signature, Consumer<IR> handler) {
        JMethod method = hierarchy.getJREMethod(signature);
        handlers.put(method, handler);
    }

    private Variable newMockVariable(Type type, JMethod container) {
        int id = counter.computeIfAbsent(container,
                (k) -> new AtomicInteger(0))
                .getAndIncrement();
        return new MockVariable(type, container,
                "@native-method-mock-var" + id);
    }

    private void addStaticStore(IR ir, String fieldSig, Variable from) {
        JField field = hierarchy.getJREField(fieldSig);
        ir.addStatement(new StaticStore(field.getRef(), from));
    }
}
