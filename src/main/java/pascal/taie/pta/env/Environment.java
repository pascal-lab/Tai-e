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
import pascal.taie.newpta.PTAOptions;
import pascal.taie.pta.env.nativemodel.NativeModel;
import pascal.taie.pta.ir.PTAIR;
import pascal.taie.pta.ir.Obj;

import static pascal.taie.java.classes.StringReps.STRING;
import static pascal.taie.java.classes.StringReps.THREAD;
import static pascal.taie.java.classes.StringReps.THREAD_GROUP;

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

    public Environment() {
        // TODO: refactor NativeModel to AnalysisMonitor
        // nativeModel must be initialized at first, because following
        // initialization calls pm.getUniqueTypeByName(), which may
        // build IR for class initializer and trigger nativeModel.
        nativeModel = PTAOptions.get().enableNativeModel()
                ? NativeModel.getDefaultModel(
                        World.getClassHierarchy(), World.getTypeManager())
                : NativeModel.getDummyModel();
        TypeManager typeManager = World.getTypeManager();
        strPool  = new StringConstantPool(typeManager);
        reflPool = new ReflectionObjectPool(typeManager);
        mainThread = new EnvObj("<main-thread>",
                typeManager.getClassType(THREAD), null);
        systemThreadGroup = new EnvObj("<system-thread-group>",
                typeManager.getClassType(THREAD_GROUP), null);
        mainThreadGroup = new EnvObj("<main-thread-group>",
                typeManager.getClassType(THREAD_GROUP), null);
        Type string = typeManager.getClassType(STRING);
        Type stringArray = typeManager.getArrayType(string, 1);
        mainArgs = new EnvObj("<main-arguments>",
                stringArray, World.getMainMethod());
        mainArgsElem = new EnvObj("<main-arguments-element>",
                string, World.getMainMethod());
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

    public void processNativeCode(PTAIR ir) {
        nativeModel.process(ir);
    }
}
