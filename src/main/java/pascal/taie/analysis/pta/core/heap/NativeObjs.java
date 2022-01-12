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

package pascal.taie.analysis.pta.core.heap;

import pascal.taie.World;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeManager;

import static pascal.taie.language.classes.ClassNames.STRING;
import static pascal.taie.language.classes.ClassNames.THREAD;
import static pascal.taie.language.classes.ClassNames.THREAD_GROUP;

/**
 * Models objects created by native code.
 */
public class NativeObjs {

    /**
     * Description for native-generating objects.
     */
    private static final String NATIVE_DESC = "EnvObj";

    private final Obj mainThread;

    private final Obj systemThreadGroup;

    private final Obj mainThreadGroup;

    private final Obj mainArgs; // main(String[] args)

    private final Obj mainArgsElem; // Element in args

    public NativeObjs(TypeManager typeManager) {
        mainThread = new MockObj(NATIVE_DESC, "<main-thread>",
                typeManager.getClassType(THREAD));
        systemThreadGroup = new MockObj(NATIVE_DESC, "<system-thread-group>",
                typeManager.getClassType(THREAD_GROUP));
        mainThreadGroup = new MockObj(NATIVE_DESC, "<main-thread-group>",
                typeManager.getClassType(THREAD_GROUP));
        Type string = typeManager.getClassType(STRING);
        Type stringArray = typeManager.getArrayType(string, 1);
        mainArgs = new MockObj(NATIVE_DESC, "<main-arguments>",
                stringArray, World.getMainMethod());
        mainArgsElem = new MockObj(NATIVE_DESC, "<main-arguments-element>",
                string, World.getMainMethod());
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
}
