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

package bamboo.pta.env.nativemodel;

import bamboo.pta.core.ProgramManager;
import bamboo.pta.element.Field;
import bamboo.pta.element.Method;
import bamboo.pta.element.Variable;
import bamboo.pta.env.Environment;
import bamboo.pta.statement.StaticStore;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

// TODO: for correctness, record which methods have been processed?
class DefaultMethodModel implements NativeMethodModel {

    private final ProgramManager pm;
    private final Environment env;
    private final Map<Method, Consumer<Method>> handlers;

    DefaultMethodModel(ProgramManager pm, Environment env) {
        this.pm = pm;
        this.env = env;
        handlers = new HashMap<>();
        initHandlers();
    }

    @Override
    public void process(Method method) {
        Consumer<Method> handler = handlers.get(method);
        if (handler != null) {
            handler.accept(method);
        }
    }

    private void initHandlers() {
        /**********************************************************************
         * java.lang.System
         *********************************************************************/
        /**
         * <java.lang.System: void setIn0(java.io.InputStream)>
         */
        registerHandler("<java.lang.System: void setIn0(java.io.InputStream)>", method -> {
            Field systemIn = pm.getUniqueFieldBySignature(
                    "<java.lang.System: java.io.InputStream in>");
            Variable param0 = method.getParam(0).get();
            method.addStatement(new StaticStore(systemIn, param0));
        });
    }

    private void registerHandler(String signature, Consumer<Method> handler) {
        handlers.put(pm.getUniqueMethodBySignature(signature), handler);
    }
}
