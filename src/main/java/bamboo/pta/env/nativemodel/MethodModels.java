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
import bamboo.pta.element.Variable;
import bamboo.pta.env.Environment;
import bamboo.pta.statement.StaticStore;

import java.util.HashMap;
import java.util.Map;

class MethodModels {

    private static ProgramManager pm;
    private static Environment env;
    private static Map<String, MethodHandler> models;

    static void setup(ProgramManager pm, Environment env) {
        MethodModels.pm = pm;
        MethodModels.env = env;
        models = new HashMap<>();
        // register method handlers
        for (Model model : Model.values()) {
            models.put(model.signature, model.handler);
        }
    }

    enum Model {

        /**********************************************************************
         * java.lang.System
         *********************************************************************/
        /**
         * <java.lang.System: void setIn0(java.io.InputStream)>
         */
        JAVA_LANG_SYSTEM_SETIN0("<java.lang.System: void setIn0(java.io.InputStream)>", method -> {
            Field systemIn = pm.getUniqueFieldBySiganture(
                    "<java.lang.System: java.io.InputStream in>");
            Variable param0 = method.getParam(0).get();
            method.addStatement(new StaticStore(systemIn, param0));
        }),
        ;

        private final String signature;
        private final MethodHandler handler;

        Model(String signature, MethodHandler handler) {
            this.signature = signature;
            this.handler = handler;
        }
    }
}
