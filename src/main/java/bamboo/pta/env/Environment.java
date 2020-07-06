/*
 * Bamboo - A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Bamboo is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package bamboo.pta.env;

import bamboo.pta.analysis.ProgramManager;
import bamboo.pta.element.Obj;
import bamboo.pta.element.Type;

/**
 * This class should be seen as part of ProgramManager
 */
public class Environment {

    private StringConstantPool strPool;

    private ReflectionObjectPool reflPool;

    /**
     * Setup Environment object using given programManager.
     * This method must be called before starting pointer analysis;
     * @param programManager
     */
    public void setup(ProgramManager programManager) {
        strPool = new StringConstantPool(programManager);
        reflPool = new ReflectionObjectPool(programManager);
    }

    public Obj getStringConstant(String constant) {
        return strPool.getStringConstant(constant);
    }

    public Obj getClassObj(Type klass) {
        return reflPool.getClassObj(klass);
    }
}
