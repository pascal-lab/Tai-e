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

package pascal.taie.language.natives;

import pascal.taie.World;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.types.Type;
import pascal.taie.language.types.TypeManager;
import pascal.taie.analysis.pta.core.heap.EnvObj;
import pascal.taie.analysis.pta.core.heap.Obj;

import static pascal.taie.language.classes.StringReps.STRING;
import static pascal.taie.language.classes.StringReps.THREAD;
import static pascal.taie.language.classes.StringReps.THREAD_GROUP;

abstract class AbstractNativeModel implements NativeModel {

    private final Obj mainThread;

    private final Obj systemThreadGroup;

    private final Obj mainThreadGroup;

    private final Obj mainArgs; // main(String[] args)

    private final Obj mainArgsElem; // Element in args

    AbstractNativeModel(TypeManager typeManager,
                        ClassHierarchy hierarchy) {
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

    @Override
    public Obj getMainThread() {
        return mainThread;
    }

    @Override
    public Obj getSystemThreadGroup() {
        return systemThreadGroup;
    }

    @Override
    public Obj getMainThreadGroup() {
        return mainThreadGroup;
    }

    @Override
    public Obj getMainArgs() {
        return mainArgs;
    }

    @Override
    public Obj getMainArgsElem() {
        return mainArgsElem;
    }
}
