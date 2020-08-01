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
import bamboo.pta.element.Method;
import bamboo.pta.env.Environment;
import bamboo.pta.statement.Call;
import bamboo.pta.statement.Statement;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

// TODO: for correctness, record which methods have been processed?
class NativeCallModel {

    private final static BiConsumer<Method, Call> dummyHandler = (m, c) -> {};
    private final ProgramManager pm;
    private final Environment env;
    private final Map<Method, BiConsumer<Method, Call>> handlers;

    NativeCallModel(ProgramManager pm, Environment env) {
        this.pm = pm;
        this.env = env;
        handlers = new HashMap<>();
        initHandlers();
    }

    void process(Method container) {
        Statement[] statements = container.getStatements()
                .toArray(new Statement[0]);
        for (Statement s : statements) {
            if (s instanceof Call) {
                Call call = (Call) s;
                Method callee = call.getCallSite().getMethod();
                handlers.getOrDefault(callee, dummyHandler)
                        .accept(container, call);
            }
        }
    }

    private void initHandlers() {
    }

    private void registerHandler(String signature,
                                 BiConsumer<Method, Call> handler) {
        handlers.put(pm.getUniqueMethodBySignature(signature), handler);
    }
}
