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

package pascal.taie.frontend.soot;

import pascal.taie.util.AnalysisException;
import soot.PackManager;
import soot.SootClass;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Dumps (Jimple) classes via PackManager.writeClass().
 */
class ClassDumper {

    private final PackManager pm;

    /**
     * The dump method.
     */
    private final Method writeClass;

    ClassDumper() {
        pm = PackManager.v();
        try {
            writeClass = pm.getClass()
                    .getDeclaredMethod("writeClass", SootClass.class);
            writeClass.setAccessible(true);
        } catch (NoSuchMethodException e) {
            throw new AnalysisException("Failed to initialize ClassDumper");
        }
    }

    void dump(SootClass c) {
        try {
            writeClass.invoke(pm, c);
        } catch (IllegalAccessException | InvocationTargetException e) {
            System.err.println("Failed to dump class " + c.getName()
                    + " due to " + e.getMessage());
        }
    }
}
