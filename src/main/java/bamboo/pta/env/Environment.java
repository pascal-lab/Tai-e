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

import bamboo.pta.core.ProgramManager;
import bamboo.pta.element.Method;
import bamboo.pta.element.Obj;
import bamboo.pta.element.Type;
import bamboo.pta.env.nativemodel.NativeModel;
import bamboo.pta.options.Options;

/**
 * This class should be seen as part of ProgramManager
 */
public class Environment {

    private StringConstantPool strPool;
    private ReflectionObjectPool reflPool;
    private NativeModel nativeModel;

    /**
     * Setup Environment object using given ProgramManager.
     * This method must be called before starting pointer analysis;
     * @param pm
     */
    public Environment(ProgramManager pm) {
        strPool  = new StringConstantPool(pm);
        reflPool = new ReflectionObjectPool(pm);
        if (Options.get().enableNativeModel()) {
            nativeModel = NativeModel.getDefaultModel(pm, this);
        } else {
            nativeModel = NativeModel.getDummyModel();
        }
    }

    public Obj getStringConstant(String constant) {
        return strPool.getStringConstant(constant);
    }

    public Obj getClassObj(Type klass) {
        return reflPool.getClassObj(klass);
    }

    public void processNativeCode(Method method) {
        nativeModel.process(method);
    }
}
