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

import pascal.taie.java.World;
import pascal.taie.java.types.Type;
import pascal.taie.pta.PTAOptions;
import pascal.taie.pta.core.ProgramManager;
import pascal.taie.pta.env.nativemodel.NativeModel;
import pascal.taie.pta.ir.IR;
import pascal.taie.pta.ir.Obj;

/**
 * This class should be seen as part of ProgramManager
 */
public class Environment {

    private NativeModel nativeModel;
    private StringConstantPool strPool;
    private ReflectionObjectPool reflPool;
    private Obj mainThread;
    private Obj systemThreadGroup;
    private Obj mainThreadGroup;
    private Obj mainArgs; // main(String[] args)
    private Obj mainArgsElem; // Element in args

    /**
     * Setup Environment object using given ProgramManager.
     * This method must be called before starting pointer analysis.
     */
    public void setup(ProgramManager pm) {
        // TODO: refactor NativeModel to AnalysisMonitor
        // nativeModel must be initialized at first, because following
        // initialization calls pm.getUniqueTypeByName(), which may
        // build IR for class initializer and trigger nativeModel.
        nativeModel = PTAOptions.get().enableNativeModel()
                ? NativeModel.getDefaultModel(World.get())
                : NativeModel.getDummyModel();
        strPool  = new StringConstantPool(pm);
        reflPool = new ReflectionObjectPool(pm);
        mainThread = new EnvObj("<main-thread>",
                pm.getUniqueTypeByName("java.lang.Thread"), null);
        systemThreadGroup = new EnvObj("<system-thread-group>",
                pm.getUniqueTypeByName("java.lang.ThreadGroup"), null);
        mainThreadGroup = new EnvObj("<main-thread-group>",
                pm.getUniqueTypeByName("java.lang.ThreadGroup"), null);
        mainArgs = new EnvObj("<main-arguments>",
                pm.getUniqueTypeByName("java.lang.String[]"),
                pm.getMainMethod());
        mainArgsElem = new EnvObj("<main-arguments-element>",
                pm.getUniqueTypeByName("java.lang.String"),
                pm.getMainMethod());
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
