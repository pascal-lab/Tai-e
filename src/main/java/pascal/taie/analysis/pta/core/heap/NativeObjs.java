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

package pascal.taie.analysis.pta.core.heap;

import pascal.taie.World;
import pascal.taie.language.type.Type;
import pascal.taie.language.type.TypeSystem;

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

    public NativeObjs(TypeSystem typeSystem) {
        mainThread = new MockObj(NATIVE_DESC, "<main-thread>",
                typeSystem.getClassType(THREAD));
        systemThreadGroup = new MockObj(NATIVE_DESC, "<system-thread-group>",
                typeSystem.getClassType(THREAD_GROUP));
        mainThreadGroup = new MockObj(NATIVE_DESC, "<main-thread-group>",
                typeSystem.getClassType(THREAD_GROUP));
        Type string = typeSystem.getClassType(STRING);
        Type stringArray = typeSystem.getArrayType(string, 1);
        mainArgs = new MockObj(NATIVE_DESC, "<main-arguments>",
                stringArray, World.get().getMainMethod());
        mainArgsElem = new MockObj(NATIVE_DESC, "<main-arguments-element>",
                string, World.get().getMainMethod());
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
