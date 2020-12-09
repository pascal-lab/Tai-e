/*
 * Tai-e: A Program Analysis Framework for Java
 *
 * Copyright (C) 2020 Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020 Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * This software is designed for the "Static Program Analysis" course at
 * Nanjing University, and it supports a subset of Java features.
 * Tai-e is only for educational and academic purposes, and any form of
 * commercial use is disallowed.
 */

package pascal.taie.pta.jimple;

import soot.Local;
import soot.jimple.internal.JimpleLocal;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manager for new created variables during method creation.
 */
class NewVariableManager {

    private final IRBuilder irBuilder;
    private final ConcurrentMap<JimpleMethod, AtomicInteger> varNumbers =
            new ConcurrentHashMap<>();

    NewVariableManager(IRBuilder irBuilder) {
        this.irBuilder = irBuilder;
    }

    JimpleVariable newTempVariable(
            String baseName, JimpleType type, JimpleMethod container) {
        String varName = baseName + getNewNumber(container);
        return newVariable(varName, type, container);
    }

    JimpleVariable getThisVariable(JimpleMethod container) {
        return newVariable("@this", container.getClassType(), container);
    }

    JimpleVariable getParameter(JimpleMethod container, int index) {
        JimpleType type = irBuilder.getType(container.getSootMethod().getParameterType(index));
        return newVariable("@parameter" + index, type, container);
    }

    JimpleVariable getReturnVariable(JimpleMethod container) {
        JimpleType type = irBuilder.getType(container.getSootMethod().getReturnType());
        return newVariable("@return", type, container);
    }

    private JimpleVariable newVariable(
            String varName, JimpleType type, JimpleMethod container) {
        Local local = new JimpleLocal(varName, type.getSootType());
        return new JimpleVariable(local, type, container);
    }

    private int getNewNumber(JimpleMethod container) {
        return varNumbers.computeIfAbsent(container,
                m -> new AtomicInteger(0))
                .getAndIncrement();
    }
}
