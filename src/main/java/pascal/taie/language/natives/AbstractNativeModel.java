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

package pascal.taie.language.natives;

import pascal.taie.World;
import pascal.taie.analysis.pta.core.heap.MockObj;
import pascal.taie.analysis.pta.core.heap.Obj;
import pascal.taie.language.classes.ClassHierarchy;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeManager;

import static pascal.taie.language.classes.StringReps.STRING;
import static pascal.taie.language.classes.StringReps.THREAD;
import static pascal.taie.language.classes.StringReps.THREAD_GROUP;

abstract class AbstractNativeModel implements NativeModel {

    private static final String OBJ_DESCR = "EnvObj";

    private final Obj mainThread;

    private final Obj systemThreadGroup;

    private final Obj mainThreadGroup;

    private final Obj mainArgs; // main(String[] args)

    private final Obj mainArgsElem; // Element in args

    AbstractNativeModel(TypeManager typeManager,
                        ClassHierarchy hierarchy) {
        mainThread = new MockObj(OBJ_DESCR, "<main-thread>",
                typeManager.getClassType(THREAD));
        systemThreadGroup = new MockObj(OBJ_DESCR, "<system-thread-group>",
                typeManager.getClassType(THREAD_GROUP));
        mainThreadGroup = new MockObj(OBJ_DESCR, "<main-thread-group>",
                typeManager.getClassType(THREAD_GROUP));
        Type string = typeManager.getClassType(STRING);
        Type stringArray = typeManager.getArrayType(string, 1);
        mainArgs = new MockObj(OBJ_DESCR, "<main-arguments>",
                stringArray, World.getMainMethod());
        mainArgsElem = new MockObj(OBJ_DESCR, "<main-arguments-element>",
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
