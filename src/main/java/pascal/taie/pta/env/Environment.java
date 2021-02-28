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

package pascal.taie.pta.env;

import pascal.taie.java.TypeManager;
import pascal.taie.java.World;
import pascal.taie.java.types.Type;
import pascal.taie.pta.PTAOptions;
import pascal.taie.pta.env.nativemodel.NativeModel;
import pascal.taie.pta.ir.IR;
import pascal.taie.pta.ir.Obj;

/**
 * This class should be seen as part of ProgramManager
 */
public class Environment {

    private final NativeModel nativeModel;

    private final StringConstantPool strPool;

    private final ReflectionObjectPool reflPool;

    private final Obj mainThread;

    private final Obj systemThreadGroup;

    private final Obj mainThreadGroup;

    private final Obj mainArgs; // main(String[] args)

    private final Obj mainArgsElem; // Element in args

    public Environment(World world) {
        // TODO: refactor NativeModel to AnalysisMonitor
        // nativeModel must be initialized at first, because following
        // initialization calls pm.getUniqueTypeByName(), which may
        // build IR for class initializer and trigger nativeModel.
        nativeModel = PTAOptions.get().enableNativeModel()
                ? NativeModel.getDefaultModel(world)
                : NativeModel.getDummyModel();
        TypeManager typeManager = world.getTypeManager();
        strPool  = new StringConstantPool(typeManager);
        reflPool = new ReflectionObjectPool(typeManager);
        mainThread = new EnvObj("<main-thread>",
                typeManager.getClassType("java.lang.Thread"), null);
        systemThreadGroup = new EnvObj("<system-thread-group>",
                typeManager.getClassType("java.lang.ThreadGroup"), null);
        mainThreadGroup = new EnvObj("<main-thread-group>",
                typeManager.getClassType("java.lang.ThreadGroup"), null);
        Type string = typeManager.getClassType("java.lang.String");
        Type stringArray = typeManager.getArrayType(string, 1);
        mainArgs = new EnvObj("<main-arguments>",
                stringArray, world.getMainMethod());
        mainArgsElem = new EnvObj("<main-arguments-element>",
                string, world.getMainMethod());
    }

    public Obj getStringConstant(String constant) {
        return strPool.getStringConstant(constant);
    }

    public Obj getClassObj(Type klass) {
        return reflPool.getClassObj(klass);
    }

    public Obj getMainThread() {
        return mainThread;
    }

    public Obj getSystemThreadGroup() {
        return systemThreadGroup;
    }

    public Obj getMainThreadGroup() {
        return mainThreadGroup;
    }

    public Obj getMainArgs() {
        return mainArgs;
    }

    public Obj getMainArgsElem() {
        return mainArgsElem;
    }

    public void processNativeCode(IR ir) {
        nativeModel.process(ir);
    }
}
