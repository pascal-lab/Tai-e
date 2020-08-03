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
import bamboo.pta.element.Type;
import bamboo.pta.element.Variable;
import bamboo.pta.env.Environment;
import bamboo.pta.statement.ArrayLoad;
import bamboo.pta.statement.ArrayStore;
import bamboo.pta.statement.Call;
import bamboo.pta.statement.Statement;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

// TODO: for correctness, record which methods have been processed?
class DefaultCallModel implements NativeCallModel {

    private final ProgramManager pm;
    private final Environment env;
    // Use String as key is to avoid cyclic dependence during the
    // initialization of ProgramManager.
    // TODO: use Method as key to improve performance?
    private final Map<String, BiConsumer<Method, Call>> handlers;
    /**
     * Counter to give each mock variable an unique name.
     */
    private final AtomicInteger counter;

    DefaultCallModel(ProgramManager pm, Environment env) {
        this.pm = pm;
        this.env = env;
        handlers = new HashMap<>();
        counter = new AtomicInteger(0);
        initHandlers();
    }

    @Override
    public void process(Method container) {
        Statement[] statements = container.getStatements()
                .toArray(new Statement[0]);
        for (Statement s : statements) {
            if (s instanceof Call) {
                Call call = (Call) s;
                Method callee = call.getCallSite().getMethod();
                BiConsumer<Method, Call> handler =
                        handlers.get(callee.getSignature());
                if (handler != null) {
                    handler.accept(container, call);
                }
            }
        }
    }

    private void initHandlers() {
        /**********************************************************************
         * java.lang.System
         *********************************************************************/
        // <java.lang.System: void arraycopy(java.lang.Object,int,java.lang.Object,int,int)>
        registerHandler("<java.lang.System: void arraycopy(java.lang.Object,int,java.lang.Object,int,int)>", (method, call) -> {
            Variable temp = newMockVariable(
                    pm.getUniqueTypeByName("java.lang.Object"), method);
            Optional<Variable> src = call.getCallSite().getArg(0);
            Optional<Variable> dest = call.getCallSite().getArg(2);
            if (src.isPresent() && dest.isPresent()) {
                method.addStatement(new ArrayLoad(temp, src.get()));
                method.addStatement(new ArrayStore(dest.get(), temp));
            }
        });
    }

    private void registerHandler(String signature,
                                 BiConsumer<Method, Call> handler) {
        handlers.put(signature, handler);
    }

    private Variable newMockVariable(Type type, Method container) {
        return new MockVariable(type, container,
                "@native-call-mock-var" + counter.getAndIncrement());
    }
}
