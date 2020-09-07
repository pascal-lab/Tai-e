/*
 * Panda - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Panda is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.panda.pta.jimple;

import pascal.panda.util.AnalysisException;
import soot.PackManager;
import soot.SootClass;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Dump (Jimple) classes via PackManager.writeClass().
 */
class ClassDumper {

    private final PackManager pm;
    // the method to dump class files
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
