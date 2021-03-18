/*
 * Tai-e: A Static Analysis Framework for Java
 *
 * Copyright (C) 2020-- Tian Tan <tiantan@nju.edu.cn>
 * Copyright (C) 2020-- Yue Li <yueli@nju.edu.cn>
 * All rights reserved.
 *
 * Tai-e is only for educational and academic purposes,
 * and any form of commercial use is disallowed.
 * Distribution of Tai-e is disallowed without the approval.
 */

package pascal.taie.frontend.soot;

import pascal.taie.language.classes.JMethod;
import pascal.taie.language.types.Type;
import pascal.taie.analysis.oldpta.ir.DefaultVariable;
import pascal.taie.analysis.oldpta.ir.Variable;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import static pascal.taie.util.CollectionUtils.newConcurrentMap;

/**
 * Manager for new created variables during method creation.
 */
class NewVariableManager {

    private final ConcurrentMap<JMethod, AtomicInteger> varNumbers
            = newConcurrentMap();

    Variable newTempVariable(
            String baseName, Type type, JMethod container) {
        String varName = baseName + getNewNumber(container);
        return newVariable(varName, type, container);
    }

    Variable getThisVariable(JMethod container) {
        return newVariable("@this",
                container.getDeclaringClass().getType(), container);
    }

    Variable getParameter(JMethod container, int index) {
        return newVariable("@parameter" + index,
                container.getParamType(index), container);
    }

    Variable getReturnVariable(JMethod container) {
        return newVariable("@return", container.getReturnType(), container);
    }

    private Variable newVariable(
            String varName, Type type, JMethod container) {
        return new DefaultVariable(varName, type, container);
    }

    private int getNewNumber(JMethod container) {
        return varNumbers.computeIfAbsent(container,
                m -> new AtomicInteger(0))
                .getAndIncrement();
    }
}
